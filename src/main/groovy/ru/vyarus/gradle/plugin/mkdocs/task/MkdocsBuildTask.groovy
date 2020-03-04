package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.transform.CompileStatic
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import ru.vyarus.gradle.plugin.mkdocs.util.MkdocsConfig
import ru.vyarus.gradle.plugin.mkdocs.util.TemplateUtils

/**
 * Builds mkdocs site. If version is configured as default for publication, then generate extra index.html.
 * If mkdocs.yml contains site_url value then current path will applied in config (original config reverted after
 * the build).
 *
 * @author Vyacheslav Rusakov
 * @since 14.11.2017
 */
@CompileStatic
class MkdocsBuildTask extends MkdocsTask {

    private static final String SITE_URL = 'site_url'

    @OutputDirectory
    File outputDir

    @Input
    boolean updateSiteUrl

    @Override
    void run() {
        String path = extension.resolveDocPath()
        boolean isMultiVersion = path != null
        Closure action = { super.run() }

        if (getUpdateSiteUrl() && isMultiVersion) {
            // update mkdocs.yml site_url from global to published folder (in order to build correct sitemap)
            withModifiedConfig(path, action)
        } else {
            action.call()
        }

        if (isMultiVersion) {
            // add root index.html
            copyRedirect(path)
        }
    }

    @Override
    Object getCommand() {
        boolean isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
        String path = getOutputDir().canonicalPath
        if (isWindows) {
            // always wrap into quotes for windows
            path = "\"$path\""
        }
        // use array to avoid escaping spaces in path (and consequent args parsing)
        return ['build', '-c', '-d', path]
    }

    @InputDirectory
    @SuppressWarnings('UnnecessaryGetter')
    File getSourcesDir() {
        return project.file(getWorkDir())
    }

    private void withModifiedConfig(String path, Closure action) {
        MkdocsConfig conf = new MkdocsConfig(project, extension.sourcesDir)
        String url = conf.find(SITE_URL)

        // site_url not defined or already points to correct location
        if (!url || url.endsWith(path) || url.endsWith("$path/")) {
            action.call()
            return
        }

        File backup = conf.backup()
        try {
            String slash = '/'
            String folderUrl = (url.endsWith(slash) ? url : (url + slash)) + path
            conf.set(SITE_URL, folderUrl)
            logger.lifecycle("Modified ${project.relativePath(conf.config)}: '$SITE_URL: $folderUrl'")
            action.call()
        } finally {
            conf.restoreBackup(backup)
            logger.lifecycle("Original ${project.relativePath(conf.config)} restored")
        }
    }

    private void copyRedirect(String path) {
        if (extension.publish.rootRedirect) {
            // create root redirection file
            TemplateUtils.copy(project, '/ru/vyarus/gradle/plugin/mkdocs/template/publish/',
                    extension.buildDir, [docPath: path])
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
