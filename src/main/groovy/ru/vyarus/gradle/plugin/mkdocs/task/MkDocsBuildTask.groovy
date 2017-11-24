package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.transform.CompileStatic
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory

/**
 * Builds mkdocs site. If version is configured as default for publication, then generate extra index.html.
 *
 * @author Vyacheslav Rusakov
 * @since 14.11.2017
 */
@CompileStatic
class MkDocsBuildTask extends MkdocsTask {

    @OutputDirectory
    File outputDir

    @Override
    void run() {
        super.run()

        // add redirect index file
        if (extension.publishAsDefaultVersion) {
            URL template = getClass()
                    .getResource('/ru/vyarus/gradle/plugin/mkdocs/template/publish/index.html')

            project.copy {
                from project.file(template)
                into extension.buildDir
                filter(ReplaceTokens, tokens: [docVersion: extension.docVersion])
            }
        }
    }

    @InputDirectory
    @SuppressWarnings('UnnecessaryGetter')
    File getSourcesDir() {
        return project.file(getWorkDir())
    }

    @Override
    String getCommand() {
        return "build --clean --site-dir ${getOutputDir()}"
    }
}
