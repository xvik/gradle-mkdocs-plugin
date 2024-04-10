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

    abstract DirectoryProperty getRepoDir()
    abstract Property<String> getRepoUri()
    abstract Property<String> getReferenceRepoUri()
    abstract Property<String> getBranch()
    abstract Property<String> getCommitMessage()
    abstract Property<Boolean> getSign()
    CopySpec contents
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
