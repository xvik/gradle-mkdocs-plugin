package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import ru.vyarus.gradle.plugin.mkdocs.util.TemplateUtils

import javax.inject.Inject

/**
 * Generate initial mkdocs site. Does not use "mkdocs new" command. Custom template is used instead in order
 * to pre-configure target site with the material theme and enable some extensions.
 *
 * @author Vyacheslav Rusakov
 * @since 13.11.2017
 */
@CompileStatic
abstract class MkdocsInitTask extends DefaultTask {

    /**
     * Documentation sources folder (mkdocs sources root folder).
     */
    @Input
    abstract Property<String> getSourcesDir()

    protected Provider<String> projectName = project.provider { project.name }
    protected Provider<String> projectDesc = project.provider { project.description }

    @TaskAction
    void run() {
        String sourcesPath = sourcesDir.get()
        File dir = fs.file(sourcesPath)
        if (dir.exists() && dir.listFiles().length > 0) {
            throw new GradleException("Can't init new mkdocs site because target directory is not empty: $dir")
        }

        TemplateUtils.copy(fs, '/ru/vyarus/gradle/plugin/mkdocs/template/init/', dir, [
                projectName: projectName.get(),
                projectDescription: projectDesc.orNull ?: '',
                docDir: sourcesPath,
        ])
        logger.lifecycle("Mkdocs site initialized: $sourcesPath")
    }

    @Inject
    protected abstract FileOperations getFs()
}
