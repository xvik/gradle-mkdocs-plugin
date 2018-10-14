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
            'mkdocs:1.0.4',
            'mkdocs-material:3.0.4',
            'pygments:2.2.0',
            'pymdown-extensions:6.0.0',
    ]

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
     * Publication configuration.
     */
    final Publish publish = new Publish()

    void setPublish(@DelegatesTo(Publish) Closure config) {
        project.configure(publish, config)
    }

    @Memoized
    String resolveDocPath() {
        if (!publish.docPath) {
            return null
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
    String resolveComment() {
        render(publish.comment, [docPath: resolveDocPath() ?: ''])
    }

    private String render(String template, Map args) {
        new GStringTemplateEngine().createTemplate(template).make(args).toString()
    }

    /**
     * Publication configuration.
     */
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
         * When enabled, publish additional index.html at the root. Index file will redirect to the published
         * documentation sub folder (last published version).
         * <p>
         * Ignored if {@link #docPath} set to null or empty (multi version publishing not used).
         */
        boolean rootRedirect = true

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
