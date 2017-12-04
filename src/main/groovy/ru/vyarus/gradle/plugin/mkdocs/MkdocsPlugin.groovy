package ru.vyarus.gradle.plugin.mkdocs

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.ajoberstar.gradle.git.publish.GitPublishPlugin
import org.ajoberstar.grgit.Grgit
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.mkdocs.task.MkDocsBuildTask
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

        project.tasks.create(MKDOCS_BUILD_TASK, MkDocsBuildTask) {
            description = 'Build mkdocs documentation'
            group = DOCUMENTATION_GROUP
            conventionMapping.with {
                it.extraArgs = strictConvention
                it.outputDir = { project.file("${getBuildOutputDir(extension)}") }
            }
        }

        project.tasks.create('mkdocsServe', MkdocsTask) {
            description = 'Start mkdocs live reload server'
            group = DOCUMENTATION_GROUP
            command = 'serve'
            conventionMapping.extraArgs = strictConvention
        }

        project.tasks.create('mkdocsInit', MkdocsInitTask) {
            description = 'Create mkdocs documentation'
            group = DOCUMENTATION_GROUP
        }

        project.tasks.withType(MkdocsTask) { task ->
            task.conventionMapping.workDir = { extension.sourcesDir }
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

        // mkdocsBuild <- gitPublishReset <- gitPublishCopy <- gitPublishCommit <- gitPublishPush <- mkdocsPublish
        project.tasks.gitPublishReset.dependsOn MKDOCS_BUILD_TASK

        // create dummy task to simplify usage
        project.tasks.create('mkdocsPublish') {
            group = DOCUMENTATION_GROUP
            description = 'Publish documentation'
            dependsOn 'gitPublishPush'
        }
    }

    private String getBuildOutputDir(MkdocsExtension extension) {
        String path = extension.resolveDocPath()
        return extension.buildDir + (path ? '/' + path : '')
    }

    @SuppressWarnings('UnnecessaryCast')
    private String getProjectRepoUri(Project project) {
        try {
            Grgit repo = Grgit.open([dir: project.rootDir] as Map<String, Object>)
            return repo.remote.list().find { it.name == 'origin' }?.url
        } catch (RepositoryNotFoundException ignored) {
            // repository not initialized case - do nothing (most likely user is just playing with the plugin)
        }
        return null
    }

}
