package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import ru.vyarus.gradle.plugin.mkdocs.MkdocsExtension
import ru.vyarus.gradle.plugin.mkdocs.util.TemplateUtils

/**
 * Generate initial mkdocs site. Does not use "mkdocs new" command. Custom template is used instead in order
 * to pre-configure target site with the material theme and enable some extensions.
 *
 * @author Vyacheslav Rusakov
 * @since 13.11.2017
 */
@CompileStatic
class MkdocsInitTask extends DefaultTask {

    @TaskAction
    void run() {
        MkdocsExtension extension = project.extensions.findByType(MkdocsExtension)

        File dir = project.file(extension.sourcesDir)
        if (dir.exists() && dir.listFiles().length > 0) {
            throw new GradleException("Can't init new mkdocs site because target directory is not empty: $dir")
        }

        TemplateUtils.copy(project, '/ru/vyarus/gradle/plugin/mkdocs/template/init/', dir, [
                projectName: project.name,
                projectDescription: project.description ?: '',
                docDir: extension.sourcesDir,
        ])
        logger.lifecycle("Mkdocs site initialized in $extension.sourcesDir")
    }
}
