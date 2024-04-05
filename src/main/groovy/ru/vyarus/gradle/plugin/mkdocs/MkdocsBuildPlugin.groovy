package ru.vyarus.gradle.plugin.mkdocs

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.mkdocs.task.MkdocsBuildTask
import ru.vyarus.gradle.plugin.mkdocs.task.MkdocsInitTask
import ru.vyarus.gradle.plugin.mkdocs.task.MkdocsTask
import ru.vyarus.gradle.plugin.python.PythonExtension
import ru.vyarus.gradle.plugin.python.PythonPlugin

/**
 * Base mkdocs plugin without publishing tasks (and so without grgit plugin registration). May be used in cases when
 * publication is not required or when there are problems with grgit plugin.
 * <p>
 * Provides tasks:
 * <ul>
 *     <li>mkdocsInit - create documentation site
 *     <li>mkdocsBuild - build site
 *     <li>mkdocsServe - start livereload server (for development)
 * </ul>
 * <p>
 * mkdocksInit not use 'mkdocs new', instead more advanced template used with pre-initialized material theme.
 * <p>
 * Plugin will also apply all required pip modules to use mkdocks with material theme and basic plugins
 * (see {@link ru.vyarus.gradle.plugin.mkdocs.MkdocsExtension#DEFAULT_MODULES}).
 *
 * @author Vyacheslav Rusakov
 * @since 28.10.2022
 */
@CompileStatic
class MkdocsBuildPlugin implements Plugin<Project> {

    private static final List<String> STRICT = ['-s']
    private static final String DEV_ADDR = '--dev-addr'

    public static final String MKDOCS_BUILD_TASK = 'mkdocsBuild'
    public static final String DOCUMENTATION_GROUP = 'documentation'

    @Override
    void apply(Project project) {
        MkdocsExtension extension = project.extensions.create('mkdocs', MkdocsExtension, project)

        project.plugins.apply(PythonPlugin)
        applyDefaults(project)
        configureMkdocsTasks(project, extension)
        configureServe(project, extension)
    }

    private void applyDefaults(Project project) {
        // apply default mkdocs, material and minimal plugins
        // user will be able to override versions, if required
        project.extensions.getByType(PythonExtension).pip(MkdocsExtension.DEFAULT_MODULES)
    }

    @SuppressWarnings('AbcMetric')
    private void configureMkdocsTasks(Project project, MkdocsExtension extension) {
        project.tasks.register(MKDOCS_BUILD_TASK, MkdocsBuildTask) { task ->
            task.description = 'Build mkdocs documentation'
            task.group = DOCUMENTATION_GROUP
            task.extraArgs.convention project.provider { extension.strict ? STRICT : null }
            task.outputDir.convention(project.file("${getBuildOutputDir(extension)}"))
            task.updateSiteUrl.convention(extension.updateSiteUrl)
            task.versionPath.convention(extension.resolveDocPath())
            task.versionName.convention(extension.resolveVersionTitle())
            task.rootRedirectPath
                    .convention(extension.publish.rootRedirect ? extension.resolveRootRedirectionPath() : null)
            task.versionAliases.convention(extension.publish.versionAliases
                    ? extension.publish.versionAliases as List : [])
            task.buildDir.convention(project.file(extension.buildDir))
            task.existingVersionFile.convention(extension.publish.existingVersionsFile)
        }

        project.tasks.register('mkdocsInit', MkdocsInitTask) { task ->
            task.description = 'Create mkdocs documentation'
            task.group = DOCUMENTATION_GROUP
            task.sourcesDir.convention(extension.sourcesDir)
        }

        project.tasks.withType(MkdocsTask).configureEach { task ->
            task.workDir.convention(extension.sourcesDir)
            task.sourcesDir.convention(extension.sourcesDir)
            task.extras.convention(project.provider {
                // resolving lazy gstrings ("${-> something}") because they can't be serialized
                Map<String, Serializable> res = [:]
                extension.extras.each {
                    res.put(it.key, it.value?.toString())
                }
                res
            })
        }

        // simplify direct task usage
        project.extensions.extraProperties.set(MkdocsTask.simpleName, MkdocsTask)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void configureServe(Project project, MkdocsExtension extension) {
        project.tasks.register('mkdocsServe', MkdocsTask) { task ->
            task.description = 'Start mkdocs live reload server'
            task.group = DOCUMENTATION_GROUP
            task.command.set('serve')
            if (dockerUsed) {
                // mkdocs in strict mode does not allow external mappings, so avoid strict, event if configured
                // also ip must be changed, otherwise server would be invisible outside docker
                task.extraArgs DEV_ADDR, "0.0.0.0:${extension.devPort}"
            } else {
                List<String> args = extension.strict ? new ArrayList<>(STRICT) : []
                args += [DEV_ADDR, "127.0.0.1:${extension.devPort}"]
                task.extraArgs.addAll(args)
            }
            // docker activation is still up to global configuration - here just required tuning
            // task would be started in exclusive container in order to stream output immediately
            task.docker.exclusive.set(true)
            task.docker.ports extension.devPort
        }
    }

    private String getBuildOutputDir(MkdocsExtension extension) {
        return extension.buildDir + (extension.multiVersion ? '/' + extension.resolveDocPath() : '')
    }
}
