package ru.vyarus.gradle.plugin.mkdocs

import groovy.transform.CompileStatic

/**
 * Mkdocs plugin extension.
 * <p>
 * If extra python packages required, use python extension: {@code python.pip 'module:version', 'module2:version'}.
 * To override default package version: {@code python.pip 'mkdocs:17.0'} (even downgrade allowed).
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
            'mkdocs:0.17.2',
            'mkdocs-material:2.2.0',
            'pygments:2.2.0',
            'pymdown-extensions:4.3',
    ]

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
     * Current documentation version. Used to publish generated docs under that version on github pages.
     * It will be set project version by default, but in most cases it should be manually set to the last
     * released version because current version documentation is often updated after the release.
     * <p>
     * Reminder: expected github pages structure is /version/doc-site (so github pages holds documentation
     * for all versions).
     */
    String docVersion

    /**
     * Root directory used for documentation building (folder structure is different from usual mkdocs because
     * multi-version publishing is expected).
     * Mkdocs build task will build into $buildDir/$docVersion/.
     * If root redirect enabled (publish as default version) then index.html will be added at the root.
     */
    String buildDir = 'build/mkdocs'

    /**
     * Folder used for gh-pages repository checkout/update/publish. Use gradle folder to "cache" gh-pages checkout
     * because eventually it would contain multiple versions which does not have to be loaded (checkout) on each
     * publication.
     */
    String publishRepoDir = '.gradle/gh-pages'

    /**
     * Publish repository url. If not set (default) then the same repository will be used (different branch).
     * This is quite common for open source projects to publish github pages on the same repository as source.
     */
    String publishRepoUri

    /**
     * Target branch name. Github pages branch name by default.
     */
    String publishBranch = 'gh-pages'

    /**
     * Set redirect from github pages root to published version (index.html committed at the root with redirect).
     * Example: if true, then root github pages url (http://something/) will redirect to published
     * version (http://something/1.1.0/).
     */
    boolean publishAsDefaultVersion = true

    /**
     * Commit message template.
     */
    String publishComment = 'Publish documentation for version $docVersion'

}
