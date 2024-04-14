package ru.vyarus.gradle.plugin.mkdocs

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet

/**
 * Git publication extension. Based on https://github.com/ajoberstar/gradle-git-publish.
 * Note: plugin features preserved almost entirely to keep backwards compatibility for customization cases.
 *
 * @author Vyacheslav Rusakov
 * @since 08.04.2024
 */
@CompileStatic
@SuppressWarnings(['AbstractClassWithPublicConstructor', 'ConfusingMethodName'])
abstract class GitPublishExtension {

    /**
     * Repository directory. For example, {@code file("$buildDir/something")}.
     */
    abstract DirectoryProperty getRepoDir()
    /**
     * Repository to publish into (must exists). For example, {@code 'git@github.com:user/test-repo.git'}
     */
    abstract Property<String> getRepoUri()
    /**
     * (Optional) Where to fetch from prior to fetching from the remote (i.e. a local repo to save time).
     */
    abstract Property<String> getReferenceRepoUri()
    /**
     * Target branch (would be created if does not exists).
     */
    abstract Property<String> getBranch()
    /**
     * Commit message.
     */
    abstract Property<String> getCommitMessage()
    /**
     * Signing commits. Omit to use the default from your gitconfig.
     */
    abstract Property<Boolean> getSign()

    /**
     * Repository user name (for authorization) or github token. See "org.ajoberstar.grgit.auth.username" property
     * https://ajoberstar.org/grgit/main/grgit-authentication.html
     */
    abstract Property<String> getUsername()
    /**
     * Repository password. See "org.ajoberstar.grgit.auth.password" property
     * https://ajoberstar.org/grgit/main/grgit-authentication.html
     */
    abstract Property<String> getPassword()

    /**
     * {@link CopySpec} content to add into repository.
     * <code>
     *  contents {
     *    from 'src/pages'
     *    from(javadoc) {
     *      into 'api'
     *    }
     *  }
     * </code>
     * @see <a href="https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Copy.html">gradle docs</a>
     */
    CopySpec contents
    /**
     * What to keep (or remote) in existing branch.
     * E.g. to keep version 1.0.0 files except temp.txt file:
     * <code>
     *     preserve {
     *        include '1.0.0/**'
     *        exclude '1.0.0/temp.txt'
     *      }
     * </code>
     * <p>
     * By default, only ".git" folder preserved.
     *
     * @see <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/util/PatternFilterable.html">
     *     gradle doc</a>
     */
    PatternFilterable preserve

    GitPublishExtension(Project project) {
        this.contents = project.copySpec()
        this.preserve = new PatternSet()
        this.preserve.include('.git/**/*')
    }

    void contents(Action<? super CopySpec> action) {
        action.execute(contents)
    }

    void preserve(Action<? super PatternFilterable> action) {
        action.execute(preserve)
    }
}
