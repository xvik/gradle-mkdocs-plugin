package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import ru.vyarus.gradle.plugin.mkdocs.MkdocsExtension
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

    /**
     * Output directory.
     */
    @OutputDirectory
    abstract Property<File> getOutputDir()

    /**
     * Update 'site_url' in mkdocs configuration to correct path. Required for multi-version publishing to
     * correctly update version url.
     */
    @Input
    abstract Property<Boolean> getUpdateSiteUrl()

    /**
     * Version directory path. "." if no version directories used.
     * @see {@link ru.vyarus.gradle.plugin.mkdocs.MkdocsExtension.Publish#docPath}
     */
    @Input
    abstract Property<String> getVersionPath()

    /**
     * Version name (what to show in version dropdown).
     * @see {@link ru.vyarus.gradle.plugin.mkdocs.MkdocsExtension.Publish#versionTitle}
     */
    @Input
    @Optional
    abstract Property<String> getVersionName()

    /**
     * Root redirection path or null to disable root redirection.
     */
    @Input
    @Optional
    abstract Property<String> getRootRedirectPath()

    /**
     * Version aliases.
     */
    @Input
    @Optional
    abstract ListProperty<String> getVersionAliases()

    /**
     * Mkdocs build directory.
     */
    @OutputDirectory
    abstract Property<File> getBuildDir()

    /**
     * Versions file to update (when publish tasks not used).
     */
    @Input
    @Optional
    abstract Property<String> getExistingVersionFile()

    @Internal
    Provider<Directory> projectBuildDir = project.layout.buildDirectory

    MkdocsBuildTask() {
        command.set(project.provider {
            boolean isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
            String path = outputDir.get().canonicalPath
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
        String path = versionPath.get()
        boolean multiVersion = path != MkdocsExtension.SINGLE_VERSION_PATH
        Closure action = { super.run() }

        if (updateSiteUrl.get() && multiVersion) {
            // update mkdocs.yml site_url from global to published folder (in order to build correct sitemap)
            withModifiedConfig(path, action)
        } else {
            action.call()
        }

        // output directory must be owned by current user, not root, otherwise clean would fail
        dockerChown(outputDir.get().toPath())

        // optional remote versions file update
        updateVersions()

        if (multiVersion) {
            // add root index.html
            copyRedirect(path)
            copyAliases(path)
        }
    }

    @InputDirectory
    File getSourcesDirectory() {
        return fs.file(sourcesDir.get())
    }

    private void withModifiedConfig(String path, Closure action) {
        MkdocsConfig conf = new MkdocsConfig(fs, sourcesDir.get())
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
            logger.lifecycle("Modified ${fs.relativePath(conf.config)}: '$SITE_URL: $folderUrl'")
            action.call()
        } finally {
            conf.restoreBackup(backup)
            logger.lifecycle("Original ${fs.relativePath(conf.config)} restored")
        }
    }

    private void updateVersions() {
        String versions = existingVersionFile.orNull
        if (versions) {
            File target
            if (versions.contains(':')) {
                target = new File(projectBuildDir.get().asFile, 'old-versions.json')
                download(versions, target)
            } else {
                target = fs.file(versions)
            }
            File res = VersionsFileUtils.getTarget(buildDir.get())
            logger.lifecycle('Creating versions file: {}', fs.relativePath(res))
            Map<String, Map<String, Object>> parse = VersionsFileUtils.parse(target)
            if (target.exists()) {
                logger.lifecycle("\tExisting versions file '{}' loaded with {} versions", versions, parse.size())
            } else {
                logger.warn("\tWARNING: configured versions file '{}' does not exist - creating new file instead",
                        versions)
            }

            String currentVersion = versionPath.get()
            if (VersionsFileUtils.addVersion(parse, currentVersion)) {
                logger.lifecycle('\tNew version added: {}', currentVersion)
            }
            VersionsFileUtils.updateVersion(parse,
                    currentVersion, versionName.get(), versionAliases.get())
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
        if (rootRedirectPath.orNull) {
            String target = rootRedirectPath.get()
            List<String> possible = [path]
            possible.addAll(versionAliases.get())
            if (!possible.contains(target)) {
                throw new GradleException("Invalid mkdocs.publish.rootRedirectTo option value: '$target'. " +
                        "Possible values are: ${possible.join(', ')} ('\$docPath' for actual version)")
            }
            // create root redirection file
            TemplateUtils.copy(fs, '/ru/vyarus/gradle/plugin/mkdocs/template/publish/',
                    fs.relativePath(buildDir.get()), [docPath: target])
            logger.lifecycle('Root redirection enabled to: {}', target)
        } else {
            // remove stale index.html (to avoid unintentional redirect override)
            // of course, build always must be called after clean, but at least minimize damage on incorrect usage
            File index = new File(buildDir.get(), 'index.html')
            if (index.exists()) {
                index.delete()
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void copyAliases(String version) {
        File baseDir = buildDir.get()

        List<String> aliases = versionAliases.get()
        if (aliases) {
            aliases.each { String alias ->
                fs.copy { spec ->
                    spec.from new File(baseDir, version)
                    spec.into new File(baseDir, alias)
                }
            }
            logger.lifecycle('Version aliases added: {}', aliases.join(', '))
        }
    }
}
