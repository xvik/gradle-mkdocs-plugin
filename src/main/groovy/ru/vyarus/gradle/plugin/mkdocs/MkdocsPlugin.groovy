package ru.vyarus.gradle.plugin.mkdocs

import groovy.text.GStringTemplateEngine
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.ajoberstar.gradle.git.publish.GitPublishPlugin
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
        MkdocsExtension extension = project.extensions.create('mkdocs', MkdocsExtension)

        project.plugins.apply(PythonPlugin)

        project.afterEvaluate {
            // project version by default
            extension.docVersion = extension.docVersion ?: project.version
        }

        applyDefaultModules(project)
        configureMkdocsTasks(project, extension)
        configurePublish(project, extension)
    }

    private void applyDefaultModules(Project project) {
        // apply default mkdocs, material and minimal plugins
        // // user will be able to override versions, if required
        project.extensions.getByType(PythonExtension).modules.addAll(MkdocsExtension.DEFAULT_MODULES)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void configureMkdocsTasks(Project project, MkdocsExtension extension) {

        Closure strictConvention = { extension.strict ? ['-s'] : null }

        project.tasks.create(MKDOCS_BUILD_TASK, MkDocsBuildTask) {
            description = 'Build mkdocs documentation'
            group = DOCUMENTATION_GROUP
            conventionMapping.with {
                it.extraArgs = strictConvention
                it.outputDir = { project.file("${extension.buildDir}/${extension.docVersion}") }
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
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void configurePublish(Project project, MkdocsExtension extension) {
        project.plugins.apply(GitPublishPlugin)

        project.afterEvaluate {
            project.configure(project) {
                gitPublish {

                    // by default the same repo
                    if (extension.publishRepoUri) {
                        repoUri = extension.publishRepoUri
                    }

                    branch = extension.publishBranch

                    // folder to checkout branch, apply changes and commit
                    repoDir = file(extension.publishRepoDir)

                    contents {
                        from("${extension.buildDir}")
                    }

                    // keep everything (all other versions) except publishing version
                    preserve {
                        include '**'
                        exclude "${extension.docVersion}/**"
                    }

                    commitMessage = getComment(extension)
                }
            }
        }

        // mkdocsBuild <- gitPublishReset <- gitPublishCopy <- gitPublishCommit <- gitPublishPush <- mkdocsPublish
        project.tasks.gitPublishReset.dependsOn MKDOCS_BUILD_TASK

        // create dummy task to simplify usage
        project.tasks.create('mkdocsPublish') {
            group = DOCUMENTATION_GROUP
            description = 'Publish documentation version to github pages'
            dependsOn 'gitPublishPush'
        }
    }

    private String getComment(MkdocsExtension extension) {
        return new GStringTemplateEngine().createTemplate(extension.publishComment)
                .make([docVersion: extension.docVersion]).toString()
    }

}
