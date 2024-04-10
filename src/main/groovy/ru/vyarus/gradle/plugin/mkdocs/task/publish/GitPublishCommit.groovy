package ru.vyarus.gradle.plugin.mkdocs.task.publish

import groovy.transform.CompileStatic
import org.ajoberstar.grgit.Configurable
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.AddOp
import org.ajoberstar.grgit.operation.CommitOp
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import ru.vyarus.gradle.plugin.mkdocs.service.GrgitService

import javax.inject.Inject
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Git commit task. Based on https://github.com/ajoberstar/gradle-git-publish.
 *
 * @author Vyacheslav Rusakov
 * @since 08.04.2024
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithPublicConstructor')
abstract class GitPublishCommit extends DefaultTask {
    @Internal
    abstract Property<GrgitService> getGrgit()

    @Input
    abstract Property<String> getMessage()

    @Input
    @Optional
    abstract Property<Boolean> getSign()

    @Inject
    GitPublishCommit() {
        // always consider this task out of date
        this.outputs.upToDateWhen(t -> false)
    }

    @OutputDirectory
    File getRepoDirectory() {
        return grgit.get().grgit.repository.rootDir
    }

    @TaskAction
    void commit() {
        Grgit git = grgit.get().grgit
        git.add({ AddOp op ->
            op.patterns = Stream.of('.').collect(Collectors.toSet())
        } as Configurable<AddOp>)

        // check if anything has changed
        if (git.status().clean) {
            didWork = false
        } else {
            git.commit({ CommitOp op ->
                op.message = message.get()
                if (sign.present) {
                    op.sign = sign.get()
                }
            } as Configurable<CommitOp>)
            didWork = true
        }
    }
}
