package ru.vyarus.gradle.plugin.mkdocs

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.ajoberstar.gradle.git.publish.GitPublishPlugin
import org.ajoberstar.gradle.git.publish.tasks.GitPublishReset
import org.ajoberstar.grgit.Branch
import org.ajoberstar.grgit.Configurable
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.BranchChangeOp
import org.ajoberstar.grgit.operation.OpenOp
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import ru.vyarus.gradle.plugin.mkdocs.task.GitVersionsTask

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
class MkdocsPlugin implements Plugin<Project> {

    private static final String GIT_PUSH_TASK = 'gitPublishPush'
    private static final String GIT_RESET_TASK = 'gitPublishReset'
    private static final String GIT_COPY_TASK = 'gitPublishCopy'

    @Override
    void apply(Project project) {
        project.plugins.apply(MkdocsBuildPlugin)
        MkdocsExtension extension = project.extensions.getByType(MkdocsExtension)

        project.afterEvaluate {
            // set publish repository to the current project by default
            extension.publish.repoUri = extension.publish.repoUri ?: getProjectRepoUri(project)
        }

        configurePublish(project, extension)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    @SuppressWarnings('NestedBlockDepth')
    private void configurePublish(Project project, MkdocsExtension extension) {
        project.plugins.apply(GitPublishPlugin)

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

        configurePublishTasks(project, extension)
    }

    private void configurePublishTasks(Project project, MkdocsExtension extension) {
        // mkdocsBuild <- gitPublishReset <- generateMkdocsVersionsFile <- gitPublishCopy <- gitPublishCommit
        // <- gitPublishPush <- mkdocsPublish
        project.tasks.named(GIT_RESET_TASK).configure {
            it.with {
                dependsOn MKDOCS_BUILD_TASK
                applyGitCredentials((GitPublishReset) it)
            }
        }

        project.tasks.named(GIT_PUSH_TASK).configure {
            it.with {
                doLast {
                    // UP_TO_DATE fix
                    fixOrphanBranch(project, extension.publish.repoDir, extension.publish.branch)
                }
            }
        }

        TaskProvider versionsTask = project.tasks.register('mkdocsVersionsFile', GitVersionsTask) {
            it.with {
                group = DOCUMENTATION_GROUP
                description = 'Generate/actualize versions.json file from publish repository'
                dependsOn GIT_RESET_TASK
            }
        }

        // versions generation before copy because all updated files must be correctly registered in git (by copy task)
        project.tasks.named(GIT_COPY_TASK).configure {
            it.dependsOn versionsTask
        }

        // create dummy task to simplify usage
        project.tasks.register('mkdocsPublish') {
            it.with {
                group = DOCUMENTATION_GROUP
                description = 'Publish documentation'
                dependsOn GIT_PUSH_TASK
            }
        }
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

    private void fixOrphanBranch(Project project, String repoDir, String branchName) {
        //https://github.com/ajoberstar/gradle-git-publish/issues/82
        // if remote branch not exists gitPublishReset will create orphan local branch and gitPublishPush
        // will not set tracking after push! (also gitPublishReset will not set tracking on next execution and so
        // branch will remain orphan on next run, even though remote branch already exists (and plugin knows it!)
        // This leads to incorrect UP_TO_DATE behaviour: gitPublishPush will NEVER be SKIPPED (even on consequent
        // task execution) because it tries to check branch status and fails (as branch does not have tracking)
        // To workaround this behaviour (not terribly incorrect, ofc) setting branch tracking manually, if required
        Grgit git = Grgit.open({ OpenOp op -> op.dir = project.file(repoDir) } as Configurable<OpenOp>)
        Branch branch = git.branch.current()
        // normally, this would be executed just once: after first publication, creating remote brnach; in all
        // other cases, correct tracking would be set automatically
        if (branch?.name == branchName && branch?.trackingBranch == null) {
            // set update branch with remote tracking
            git.branch.change({ BranchChangeOp op ->
                op.name = branchName
                op.startPoint = "origin/${branchName}"
                op.mode = BranchChangeOp.Mode.TRACK
            } as Configurable<BranchChangeOp>)
        }
    }
}
