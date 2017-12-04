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
class MkkdocsBuildTask extends MkdocsTask {

    @OutputDirectory
    File outputDir

    @Override
    void run() {
        super.run()

        // add redirect index file if multi-version publishing used
        String path = extension.resolveDocPath()
        if (path && extension.publish.rootRedirect) {
            if (extension.publish.rootRedirect) {
                // create root redirection file
                URL template = getClass()
                        .getResource('/ru/vyarus/gradle/plugin/mkdocs/template/publish/index.html')

                project.copy {
                    from project.file(template)
                    into extension.buildDir
                    filter(ReplaceTokens, tokens: [docPath: path])
                }
            } else {
                // remove stale index.html (to avoid unintentional redirect override)
                // of course, build always must be called after clean, but at leas minimize damage on incorrect usage
                File index = project.file(extension.buildDir + '/index.html')
                if (index.exists()) {
                    index.delete()
                }
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
        return "build -c -d \"${getOutputDir().canonicalPath}\""
    }
}
