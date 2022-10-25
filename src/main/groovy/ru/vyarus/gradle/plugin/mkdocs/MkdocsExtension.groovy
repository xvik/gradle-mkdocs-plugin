package ru.vyarus.gradle.plugin.mkdocs

import groovy.text.GStringTemplateEngine
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.gradle.api.Project

/**
 * Mkdocs plugin extension.
 * <p>
 * If extra python packages are required, use python extension: {@code python.pip 'module:version', 'module2:version'}.
 * To override default package version: {@code python.pip 'mkdocs:17.0'} (even downgrade is allowed).
 * See <a href="https://github.com/xvik/gradle-use-python-plugin">gradle-use-python-plugin</a> for details.
 * <p>
 * Publishing to github pages is implemented with
 * <a href="https://github.com/ajoberstar/gradle-git-publish#configuration>gradle-git-publish</a> plugin.
 *
 * @author Vyacheslav Rusakov
 * @since 29.10.2017
 * @see <a href="http://www.mkdocs.org/">mkdocs site</a>
 * @see <a href="https://squidfunk.github.io/mkdocs-material/">mkdocs-material site</a>
 */
@CompileStatic
class MkdocsExtension {

    /**
     * These are default modules required to use mkdocs and mkdocs material.
     * Always applied. Module version could be overridden by python pip configuration
     * {@code python.pip 'mkdocs:0.18.0'} (note that version could be also downgraded).
     */
    static final String[] DEFAULT_MODULES = [
            'mkdocs:1.4.1',
            'mkdocs-material:8.5.7',
            'pygments:2.13.0',
            'pymdown-extensions:9.7',
            'mkdocs-markdownextradata-plugin:0.2.5',
    ]

    /**
     * To avoid returning null in docs path function, current dir shortcut is returned.
     */
    static final String SINGLE_VERSION_PATH = '.'

    private final Project project

    MkdocsExtension(Project project) {
        this.project = project
    }

    /**
     * Documentation sources folder (mkdocs sources root folder).
     */
    String sourcesDir = 'src/doc'

    /**
     * Cause mkdocs to abort build on any warning (--strict option of build and serve commands).
     * Disabled by default.
     */
    boolean strict = true

    /**
     * Documentation build directory root. Folder structure is different from usual mkdocs because multi-version
     * publishing is expected. Mkdocs build task will actually build into {@code '$buildDir/$publish.docPath/'}.
     * If root redirect is enabled (publish as default version) then index.html (with redirect) will be
     * automatically added at the root folder during the build.
     */
    String buildDir = 'build/mkdocs'

    /**
     * 'site_url' configuration in mkdocs.yml declares full site url. Configuration affects meta tag and
     * generated sitemap.xml. With multi-version publication, different site url's must be used for each
     * generated version.
     * <p>
     * Set root site url into 'site_url' and mkdocsBuild will update mkdocs.yml by adding correct folder
     * to the root path. This way generated site will contain correct urls. After the build original config
     * is reverted (so config will always contain the root url).
     * <p>
     * Note that config will not be touched if site_url is not defined or multi-version publishing not used.
     * Config will not be changed for mkdocsServe task because it is not important for general documentation
     * development (you can always build final version (with mkdocsBuild) and verify generated urls).
     * <p>
     * Set to false to disable mkdocs.yml config modification.
     */
    boolean updateSiteUrl = true

    /**
     * Extra variables to use in markdown files. Requires extra plugin: 'markdownextradata' (plugin module installed
     * by default, but plugin is not activated). To activate plugin, add in mkdocs.yml:
     * <pre>
     * plugins:
     *      - search
     *      - markdownextradata
     * <pre>
     * (search plugin is active by default when plugins section not specified, so have to specify it when declaring
     * plugins). If plugin is not active, but variables declared, error would be thrown (indicating problem).
     * <p>
     * When extra variables declared, plugin will generate an additional file:
     * <code>[mkdocs.yml location]/docs/_data/gradle.yml</code> containing all specified properties.
     * Markdownextradata plugin loads all yaml files in this directory and so all gradle-defined properties
     * will be accessible as <code>{{ gradle.prop_key }}</code>. Note that you can create extra data files manually
     * if required or declare additional (static) variables directly in mkdocs.yml (extra section) - read plugin
     * documentation for more details.
     * <p>
     * If variable name would contain spaces or '-', they would be replaced with '_'. For example:
     * {@code mkdocs.extras = ['long name': 10]} would be available as <pre>{{ gradle.long_name }}</pre> and
     * {@code mkdocs.extras = ['other-name': 10]} would be available as <pre>{{ gradle.other-name }}</pre> .
     * Null values are replaced with empty line: {@code mkdocs.extras = ['name': null]} would result in empty value
     * ({@code name:}) in the generated file.
     * <p>
     * Extra properties file is generated before any mkdocs task, including custom tasks
     * extending {@code MkdocsTask}. After task execution generated file is removed.
     * <p>
     * In most cases, variable values would be evaluated immediately, but if you need delayed (lazy) evaluation then
     * use "lazy-closure" trick: {@code extras = ['version': "${-> project.version}"]}. This time, version value
     * would be evaluated only before mkdocs task execution.
     * <p>
     * Property named "extras" instead of "variables" because markdownextradata refers to extra properties section
     * by default for variables declaration.
     */
    Map<String, Serializable> extras = [:]

    /**
     * Port for serving docs with mkdocsServe command. This overrides 'dev_addr' configuration from mkdocs.yml!
     * This option required to automate proper mkdocs configuration so it could work the same in all cases.
     * <p>
     * This configuration is extremely important for docker environment when mkdocs must be bound on external ip
     * 0.0.0.0 instead of default (local) 127.0.0.1. Also, port must be mapped for docker container
     * (so plugin must know exact port).
     * <p>
     * Also, plugin would disable strict mode under docker because, otherwise, server would not start at all).
     */
    int devPort = 3000

    /**
     * Publication configuration.
     */
    final Publish publish = new Publish()

    void setPublish(@DelegatesTo(Publish) Closure config) {
        project.configure(publish, config)
    }

    /**
     * Documentation folder is configured with {@code publish.docPath}, but it is a template which needs to be resolved
     * into actual path.
     * <p>
     * For single version publishing ({@code publish.docPath} set to null and so no version sub-folders created) this
     * method returns '.' (current folder reference) in order unify {@link #resolveDocPath()} method usages in both
     * modes.
     *
     * @return resolved documentation folder, never null
     */
    @Memoized
    String resolveDocPath() {
        if (!publish.docPath) {
            return SINGLE_VERSION_PATH
        }
        String path = render(publish.docPath, [version: project.rootProject.version])
        String slash = '/'
        // cut off leading slash
        if (path.startsWith(slash)) {
            path = path[1..path.length() - 1]
        }
        // cut off trailing slash
        if (path.endsWith(slash)) {
            path = path[0..path.length() - 2]
        }
        return path
    }

    @Memoized
    String resolveVersionTitle() {
        render(publish.versionTitle, [docPath: resolveDocPath()])
    }

    @Memoized
    String resolveRootRedirectionPath() {
        render(publish.rootRedirectTo, [docPath: resolveDocPath()])
    }

    /**
     * @return true when multi-version publishing enabled, false when each publication override previous files
     *          (no sub folders)
     */
    @Memoized
    boolean isMultiVersion() {
        return resolveDocPath() != SINGLE_VERSION_PATH
    }

    @Memoized
    String resolveComment() {
        render(publish.comment, [docPath: multiVersion ? resolveDocPath() : ''])
    }

    private String render(String template, Map args) {
        new GStringTemplateEngine().createTemplate(template).make(args).toString()
    }

    /**
     * Publication configuration.
     */
    @SuppressWarnings('CodeNarc.DuplicateStringLiteral')
    static class Publish {
        /**
         * Documentation publishing path (relative to github pages root). By default set to project version
         * (to be published on as {@code '/version/'}, for example {@code '/1.0.2/').
         * <p>
         * If documentation needs to be updated after project release (quite often case) or simply not
         * published with the project release, set required version manually ({@code mkdocs.publish.docPath = '1.1'}).
         * <p>
         * Path may represent any folder structure, for example: {@code '/en/12.1/'}. Set to null or empty
         * to publish documentation without sub folders (in this case {@link # rootRedirect} option will be ignored.
         */
        String docPath = '$version'

        /**
         * When multi-version publishing enabled, plugin would generate versions.json file at the root.
         * The format used from mike tool's file (https://github.com/jimporter/mike). This file is required to
         * show version selection list in material theme (just configure it as described in docs for mike).
         * <p>
         * On publication, plugin synchronizes existing file with the list of existing folders.
         */
        boolean generateVersionsFile = true

        /**
         * Sets version name in version switcher. The value would be used only for versions.json generation only and
         * may differ from actual version path ({@link #docPath}).
         * <p>
         * Do nothing when {@link #generateVersionsFile} disabled.
         */
        String versionTitle = '$docPath'

        /**
         * When specified, version would be published in 2 (or more) folders: version itself and alias(es). This is
         * required, for example, to publish the latest documentation into 'latest' folder so user could reference
         * docs with latest instead of version. Another example is 'dev' docs.
         */
        String[] versionAliases

        /**
         * When enabled, publish additional index.html at the root. Index file will redirect to the published
         * documentation sub folder (last published version).
         * <p>
         * Ignored if {@link #docPath} set to null or empty (multi version publishing not used).
         */
        boolean rootRedirect = true

        /**
         * Specifies where root redirection should lead. By default, to just published version.
         * Option is useful when aliases used and you want to open alias by default instead of exact version (for
         * example, `latest` alias).
         * <p>
         * Plugin would verify that redirect path points to new version folder or one of its aliases and fail
         * otherwise (to prevent incorrect redirections).
         */
        String rootRedirectTo = '$docPath'

        /**
         * Publish repository url. If not set, then the same repository will be used (with different branch).
         * This is quite common for open source projects to publish github pages on the same repository as source
         * (into gh-pages branch).
         */
        String repoUri

        /**
         * Target branch name. Github pages branch name by default.
         */
        String branch = 'gh-pages'

        /**
         * Commit message template.
         */
        String comment = 'Publish $docPath documentation'

        /**
         * Folder used for gh-pages repository checkout/update/publish. Use gradle folder to cache gh-pages checkout
         * because eventually it would contain multiple versions which does not have to be loaded (checkout) on each
         * publication.
         */
        String repoDir = '.gradle/gh-pages'
    }
}
