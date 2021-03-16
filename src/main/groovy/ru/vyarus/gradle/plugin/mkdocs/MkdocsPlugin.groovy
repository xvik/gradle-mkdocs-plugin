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
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.mkdocs.task.MkdocsBuildTask
import ru.vyarus.gradle.plugin.mkdocs.task.MkdocsInitTask
import ru.vyarus.gradle.plugin.mkdocs.task.MkdocsTask
import ru.vyarus.gradle.plugin.python.PythonExtension
import ru.vyarus.gradle.plugin.python.PythonPlugin

/**
 * Mkdocs plugin. Provides tasks:
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
 *
 * @author Vyacheslav Rusakov
 * @since 29.10.2017
 */
@CompileStatic
class MkdocsPlugin implements Plugin<Project> {

    private static final String DOCUMENTATION_GROUP = 'documentation'
    private static final String MKDOCS_BUILD_TASK = 'mkdocsBuild'

    private static final String GIT_PUSH_TASK = 'gitPublishPush'
    private static final String GIT_RESET_TASK = 'gitPublishReset'

    @Override
    void apply(Project project) {
        MkdocsExtension extension = project.extensions.create('mkdocs', MkdocsExtension, project)

        project.plugins.apply(PythonPlugin)

        applyDefaults(project, extension)
        configureMkdocsTasks(project, extension)
        configurePublish(project, extension)
    }

    private void applyDefaults(Project project, MkdocsExtension extension) {
        // apply default mkdocs, material and minimal plugins
        // user will be able to override versions, if required
        project.extensions.getByType(PythonExtension).modules.addAll(MkdocsExtension.DEFAULT_MODULES)

        project.afterEvaluate {
            // set publish repository to the current project by default
            extension.publish.repoUri = extension.publish.repoUri ?: getProjectRepoUri(project)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void configureMkdocsTasks(Project project, MkdocsExtension extension) {
        Closure strictConvention = { extension.strict ? ['-s'] : null }

        project.tasks.register(MKDOCS_BUILD_TASK, MkdocsBuildTask) {
            it.with {
                description = 'Build mkdocs documentation'
                group = DOCUMENTATION_GROUP
                conventionMapping.with {
                    it.extraArgs = strictConvention
                    it.outputDir = { project.file("${getBuildOutputDir(extension)}") }
                    it.updateSiteUrl = { extension.updateSiteUrl }
                }
            }
        }

        project.tasks.register('mkdocsServe', MkdocsTask) {
            it.with {
                description = 'Start mkdocs live reload server'
                group = DOCUMENTATION_GROUP
                command = 'serve'
                conventionMapping.extraArgs = strictConvention
            }
        }

        project.tasks.register('mkdocsInit', MkdocsInitTask) {
            it.with {
                description = 'Create mkdocs documentation'
                group = DOCUMENTATION_GROUP
            }
        }

        project.tasks.withType(MkdocsTask).configureEach { task ->
            task.conventionMapping.with {
                it.workDir = { extension.sourcesDir }
                it.extras = { extension.extras }
            }
        }

        // simplify direct task usage
        project.extensions.extraProperties.set(MkdocsTask.simpleName, MkdocsTask)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void configurePublish(Project project, MkdocsExtension extension) {
        project.plugins.apply(GitPublishPlugin)

        project.afterEvaluate {
            MkdocsExtension.Publish publish = extension.publish
            String path = extension.resolveDocPath()

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
                    if (path) {
                        // keep everything (all other versions) except publishing version
                        preserve {
                            include '**'
                            exclude "${path}/**"
                        }
                    }

                    commitMessage = extension.resolveComment()
                }
            }
        }

        configurePublishTasks(project, extension)
    }

    private void configurePublishTasks(Project project, MkdocsExtension extension) {
        // mkdocsBuild <- gitPublishReset <- gitPublishCopy <- gitPublishCommit <- gitPublishPush <- mkdocsPublish
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

        // create dummy task to simplify usage
        project.tasks.register('mkdocsPublish') {
            it.with {
                group = DOCUMENTATION_GROUP
                description = 'Publish documentation'
                dependsOn GIT_PUSH_TASK
            }
        }
    }

    private String getBuildOutputDir(MkdocsExtension extension) {
        String path = extension.resolveDocPath()
        return extension.buildDir + (path ? '/' + path : '')
    }

    @SuppressWarnings('UnnecessaryCast')
    private String getProjectRepoUri(Project project) {
        try {
            Grgit repo = Grgit.open({ OpenOp op -> op.dir = project.rootDir } as Configurable<OpenOp>)
            return repo.remote.list().find { it.name == 'origin' }?.url
        } catch (RepositoryNotFoundException ignored) {
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
