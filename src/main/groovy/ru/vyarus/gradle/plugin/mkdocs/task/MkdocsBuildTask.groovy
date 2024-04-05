package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import ru.vyarus.gradle.plugin.mkdocs.util.MkdocsConfig
import ru.vyarus.gradle.plugin.mkdocs.util.TemplateUtils
import ru.vyarus.gradle.plugin.mkdocs.util.VersionsFileUtils

/**
 * Builds mkdocs site. If version is configured as default for publication, then generate extra index.html.
 * If mkdocs.yml contains site_url value then current path will applied in config (original config reverted after
 * the build).
 *
 * @author Vyacheslav Rusakov
 * @since 14.11.2017
 */
@CompileStatic
@SuppressWarnings(['DuplicateStringLiteral', 'AbstractClassWithoutAbstractMethod',
        'AbstractClassWithPublicConstructor'])
abstract class MkdocsBuildTask extends MkdocsTask {

    private static final String SITE_URL = 'site_url'

    @OutputDirectory
    File outputDir

    @Input
    boolean updateSiteUrl

    MkdocsBuildTask() {
        command.set(project.provider {
            boolean isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
            String path = getOutputDir().canonicalPath
            if (isWindows) {
                // always wrap into quotes for windows
                path = "\"$path\""
            }
            // use array to avoid escaping spaces in path (and consequent args parsing)
            return ['build', '-c', '-d', path]
        })
    }

    @Override
    void run() {
        String path = extension.resolveDocPath()
        boolean multiVersion = extension.multiVersion
        Closure action = { super.run() }

        if (getUpdateSiteUrl() && multiVersion) {
            // update mkdocs.yml site_url from global to published folder (in order to build correct sitemap)
            withModifiedConfig(path, action)
        } else {
            action.call()
        }

        // output directory must be owned by current user, not root, otherwise clean would fail
        dockerChown(getOutputDir().toPath())

        // optional remote versions file update
        updateVersions()

        if (multiVersion) {
            // add root index.html
            copyRedirect(path)
            copyAliases(path)
        }
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

    private void updateVersions() {
        String versions = extension.publish.existingVersionsFile
        if (versions) {
            File target
            if (versions.contains(':')) {
                target = new File(project.buildDir, 'old-versions.json')
                download(versions, target)
            } else {
                target = project.file(versions)
            }
            File res = VersionsFileUtils.getTarget(project, extension)
            logger.lifecycle('Creating versions file: {}', project.relativePath(res))
            Map<String, Map<String, Object>> parse = VersionsFileUtils.parse(target)
            if (target.exists()) {
                logger.lifecycle("\tExisting versions file '{}' loaded with {} versions", versions, parse.size())
            } else {
                logger.warn("\tWARNING: configured versions file '{}' does not exist - creating new file instead",
                        versions)
            }

            String currentVersion = extension.resolveDocPath()
            if (VersionsFileUtils.addVersion(parse, currentVersion)) {
                logger.lifecycle('\tNew version added: {}', currentVersion)
            }
            VersionsFileUtils.updateVersion(parse,
                    currentVersion, extension.resolveVersionTitle(), extension.publish.versionAliases)
            VersionsFileUtils.write(parse, res)
            logger.lifecycle('\tVersions written to file: {}', parse.keySet().join(', '))
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void download(String src, File target) {
        // ignore all errors (create new file if failed to load)
        ant.get(src: src, dest: target, maxtime: 5, skipexisting: true, ignoreerrors: true)
    }

    private void copyRedirect(String path) {
        if (extension.publish.rootRedirect) {
            String target = extension.resolveRootRedirectionPath()
            List<String> possible = [path]
            possible.addAll(extension.publish.versionAliases ?: [] as String[])
            if (!possible.contains(target)) {
                throw new GradleException("Invalid mkdocs.publish.rootRedirectTo option value: '$target'. " +
                        "Possible values are: ${possible.join(', ')} ('\$docPath' for actual version)")
            }
            // create root redirection file
            TemplateUtils.copy(project, '/ru/vyarus/gradle/plugin/mkdocs/template/publish/',
                    extension.buildDir, [docPath: target])
            logger.lifecycle('Root redirection enabled to: {}', target)
        } else {
            // remove stale index.html (to avoid unintentional redirect override)
            // of course, build always must be called after clean, but at least minimize damage on incorrect usage
            File index = project.file(extension.buildDir + '/index.html')
            if (index.exists()) {
                index.delete()
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void copyAliases(String version) {
        File baseDir = project.file(extension.buildDir)

        String[] aliases = extension.publish.versionAliases
        if (aliases) {
            aliases.each { String alias ->
                project.copy {
                    from new File(baseDir, version)
                    into new File(baseDir, alias)
                }
            }
            logger.lifecycle('Version aliases added: {}', aliases.join(', '))
        }
    }
}
