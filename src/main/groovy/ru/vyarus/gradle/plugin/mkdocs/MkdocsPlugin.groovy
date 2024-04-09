package ru.vyarus.gradle.plugin.mkdocs

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.ajoberstar.grgit.Configurable
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.OpenOp
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.build.event.BuildEventsListenerRegistry
import ru.vyarus.gradle.plugin.mkdocs.service.GrgitService
import ru.vyarus.gradle.plugin.mkdocs.task.GitVersionsTask
import ru.vyarus.gradle.plugin.mkdocs.task.publish.GitPublishCommit
import ru.vyarus.gradle.plugin.mkdocs.task.publish.GitPublishPush
import ru.vyarus.gradle.plugin.mkdocs.task.publish.GitPublishReset

import javax.inject.Inject

import static MkdocsBuildPlugin.DOCUMENTATION_GROUP
import static MkdocsBuildPlugin.MKDOCS_BUILD_TASK

/**
 * Mkdocs plugin. All build-related mkdocs tasks implemented with {@link MkdocsBuildPlugin}. This plugin only
 * configures publication tasks. This plugin considered as default and so will copy build plugin's javadoc here to
 * reflect complete functionality.
 * <p>
 * Provides tasks:
 * <ul>
 *     <li>mkdocsInit - create documentation site
 *     <li>mkdocsBuild - build site
 *     <li>mkdocsServe - start livereload server (for development)
 *     <li>mkdocsPublish - publish generated site to github pages (same repo)
 * </ul>
 * <p>
 * mkdocksInit not use 'mkdocs new', instead more advanced template used with pre-initialized material theme.
 * <p>
 * mkdocsPublish is a custom task (based on <a href="https://github.com/ajoberstar/gradle-git-publish">
 * git-publish</a> plugin). This is because native 'mkdocs publish' only support single documentation version and custom
 * task will manage multi-version documentation site.
 * <p>
 * Plugin will also apply all required pip modules to use mkdocks with material theme and basic plugins
 * (see {@link ru.vyarus.gradle.plugin.mkdocs.MkdocsExtension#DEFAULT_MODULES}).
 * <p>
 * NOTE: this exact plugin applies only grgit plugin and configures publication tasks. If you don't need publication
 * then you can use {@link MkdocsBuildPlugin} containing everything without publication.
 *
 * @author Vyacheslav Rusakov
 * @since 29.10.2017
 */
@CompileStatic
abstract class MkdocsPlugin implements Plugin<Project> {

    static final String GIT_RESET_TASK = 'gitPublishReset'
    static final String GIT_COPY_TASK = 'gitPublishCopy'
    static final String GIT_COMMIT_TASK = 'gitPublishCommit'
    static final String GIT_PUSH_TASK = 'gitPublishPush'
    public static final String GROUP_PUBLISHING = 'publishing'

    @Inject
    abstract BuildEventsListenerRegistry getEventsListenerRegistry()

    @Override
    void apply(Project project) {
        project.plugins.apply(MkdocsBuildPlugin)
        MkdocsExtension extension = project.extensions.getByType(MkdocsExtension)
        GitPublishExtension gitExt = project.extensions.create('gitPublish', GitPublishExtension, project)

        // set publish repository to the current project by default
        extension.publish.repoUri = getProjectRepoUri(project)

        configurePublish(project, extension, gitExt)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings('NestedBlockDepth')
    private void configurePublish(Project project, MkdocsExtension extension, GitPublishExtension gitExt) {
        project.afterEvaluate {
            MkdocsExtension.Publish publish = extension.publish

            project.configure(project) {
                gitPublish {
                    repoUri = publish.repoUri
                    branch = publish.branch

                    // folder to checkout branch, apply changes and commit
                    repoDir = file(publish.repoDir)

                    contents {
                        from("${extension.buildDir}")
                    }

                    // required only when multi-version publishing used
                    if (extension.multiVersion) {
                        // keep everything (all other versions) except publishing version
                        preserve {
                            include '**'
                            // remove publishing version
                            exclude "${extension.resolveDocPath()}/**"
                            // remove publishing version aliases
                            publish.versionAliases?.each {
                                exclude "$it/**"
                            }
                        }
                    }

                    commitMessage = extension.resolveComment()
                }
            }
        }

        configurePublishTasks(project, extension, gitExt)
    }

    @SuppressWarnings(['AbcMetric', 'MethodSize'])
    private void configurePublishTasks(Project project, MkdocsExtension extension, GitPublishExtension gitExt) {
        // mkdocsBuild <- gitPublishReset <- generateMkdocsVersionsFile <- gitPublishCopy <- gitPublishCommit
        // <- gitPublishPush <- mkdocsPublish

        // service per project (no sharing)
        Provider<GrgitService> service = createService(project)

        TaskProvider<GitPublishReset> reset = createResetTask(project, gitExt, service)
        TaskProvider copy = createCopyTask(project, gitExt)
        TaskProvider commit = createCommitTask(project, gitExt, service)
        TaskProvider push = createPushTask(project, gitExt, service)
        push.configure { it.dependsOn(commit) }
        commit.configure { it.dependsOn(copy) }
        copy.configure { it.dependsOn(reset) }

        project.tasks.named(GIT_RESET_TASK).configure {
            it.with {
                dependsOn MKDOCS_BUILD_TASK
                applyGitCredentials((GitPublishReset) it)
            }
        }

        TaskProvider versionsTask = project.tasks.register('mkdocsVersionsFile', GitVersionsTask) { task ->
            task.group = DOCUMENTATION_GROUP
            task.description = 'Generate/actualize versions.json file from publish repository'
            task.dependsOn GIT_RESET_TASK

            task.versionPath.convention(extension.resolveDocPath())
            task.versionName.convention(extension.resolveVersionTitle())
            task.generateVersionsFile.convention(extension.publish.generateVersionsFile)
            task.repoDir.convention(project.file(extension.publish.repoDir))
            task.rootRedirectPath
                    .convention(extension.publish.rootRedirect ? extension.resolveRootRedirectionPath() : null)
            task.versionAliases.convention(extension.publish.versionAliases
                    ? extension.publish.versionAliases as List : [])
            task.buildDir.convention(project.file(extension.buildDir))
        }

        // versions generation before copy because all updated files must be correctly registered in git (by copy task)
        project.tasks.named(GIT_COPY_TASK).configure {
            it.dependsOn versionsTask
        }

        // create dummy task to simplify usage
        project.tasks.register('mkdocsPublish') { task ->
            task.group = DOCUMENTATION_GROUP
            task.description = 'Publish documentation'
            task.dependsOn GIT_PUSH_TASK
        }
    }

    private Provider<GrgitService> createService(Project project) {
        // service per project (no sharing)
        Provider<GrgitService> service = project.gradle.sharedServices.registerIfAbsent(
                'mkdocsGrgit' + (project.path == ':' ? '' : project.name.capitalize()) + 'Service', GrgitService) {
        }
        // it is not required, but used to prevent KILLING service too early under configuration cache
        eventsListenerRegistry.onTaskCompletion(service)
        return service
    }

    @SuppressWarnings(['UnnecessaryCast', 'CatchException'])
    private String getProjectRepoUri(Project project) {
        try {
            Grgit repo = Grgit.open({ OpenOp op -> op.dir = project.rootDir } as Configurable<OpenOp>)
            return repo.remote.list().find { it.name == 'origin' }?.url
        } catch (Exception ignored) {
            // repository not initialized case - do nothing (most likely user is just playing with the plugin)
        }
        return null
    }

    private TaskProvider<GitPublishReset> createResetTask(Project project, GitPublishExtension extension,
                                                          Provider<GrgitService> service) {
        return project.tasks.register(GIT_RESET_TASK, GitPublishReset) { task ->
            task.group = GROUP_PUBLISHING
            task.description = 'Prepares a git repo for new content to be generated.'
            task.repoDirectory.set(extension.repoDir)
            task.repoUri.set(extension.repoUri)
            task.referenceRepoUri.set(extension.referenceRepoUri)
            task.branch.set(extension.branch)
            task.grgit.set(service)
            task.preserve = extension.preserve
        }
    }

    private TaskProvider<Copy> createCopyTask(Project project, GitPublishExtension extension) {
        return project.tasks.register(GIT_COPY_TASK, Copy) { task ->
            task.group = GROUP_PUBLISHING
            task.description = 'Copy contents to be published to git.'
            task.with(extension.contents)
            task.into(extension.repoDir)
        }
    }

    private TaskProvider<GitPublishCommit> createCommitTask(Project project,
                                                            GitPublishExtension extension,
                                                            Provider<GrgitService> service) {
        return project.tasks.register(GIT_COMMIT_TASK, GitPublishCommit) { task ->
            task.group = GROUP_PUBLISHING
            task.description = 'Commits changes to be published to git.'
            task.grgit.set(service)
            task.message.set(extension.commitMessage)
            task.sign.set(extension.sign)
        }
    }

    private TaskProvider<GitPublishPush> createPushTask(Project project,
                                                        GitPublishExtension extension,
                                                        Provider<GrgitService> service) {
        return project.tasks.register(GIT_PUSH_TASK, GitPublishPush) { task ->
            task.group = GROUP_PUBLISHING
            task.description = 'Pushes changes to git.'
            task.grgit.set(service)
            task.branch.set(extension.branch)
        }
    }

    private void applyGitCredentials(GitPublishReset task) {
        // allow to configure git auth with global gradle properties (instead of swing popup)
        // http://ajoberstar.org/grgit/grgit-authentication.html
        task.doFirst {
            ['username', 'password', 'ssh.private', 'ssh.passphrase'].each {
                String key = "org.ajoberstar.grgit.auth.$it"
                String value = task.project.findProperty(key)
                if (value) {
                    task.project.logger.lifecycle("Git auth gradle property detected: $key")
                    System.setProperty(key, value)
                }
            }
        }
    }
}
