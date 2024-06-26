package ru.vyarus.gradle.plugin.mkdocs.task.publish

import groovy.transform.CompileStatic
import org.ajoberstar.grgit.BranchStatus
import org.ajoberstar.grgit.Configurable
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.BranchChangeOp
import org.ajoberstar.grgit.operation.BranchStatusOp
import org.ajoberstar.grgit.operation.PushOp
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import ru.vyarus.gradle.plugin.mkdocs.service.GrgitService

/**
 * Git push task. Based on https://github.com/ajoberstar/gradle-git-publish.
 *
 * @author Vyacheslav Rusakov
 * @since 08.04.2024
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithPublicConstructor')
abstract class GitPublishPush extends DefaultTask {

    /**
     * Target branch (would be created if doesn't exists).
     */
    @Input
    abstract Property<String> getBranch()

    // grgit instance initiated under reset task
    @Internal
    abstract Property<GrgitService> getGrgit()

    GitPublishPush() {
        // always consider this task out of date
        this.outputs.upToDateWhen({ t -> false } as Spec<? super Task>)
        // NOTE: onlyIf removed due to conflicts with configuration cache so task would never be up to date
    }

    @OutputDirectory
    File getRepoDirectory() {
        return grgit.get().grgit.repository.rootDir
    }

    @TaskAction
    void push() {
        Grgit git = grgit.get().grgit
        // moved from onlyIf to avoid calling git under configuration stage
        if (!isPushRequired(git)) {
            return
        }
        String pubBranch = branch.get()
        git.push({ PushOp op ->
            op.refsOrSpecs = Arrays.asList(String.format('refs/heads/%s:refs/heads/%s', pubBranch, pubBranch))
        } as Configurable<PushOp>)
        // ensure tracking is set
        git.branch.change({ BranchChangeOp op ->
            op.name = pubBranch
            op.startPoint = 'origin/' + pubBranch
            op.mode = BranchChangeOp.Mode.TRACK
        } as Configurable<BranchChangeOp>)
    }

    private boolean isPushRequired(Grgit git) {
        try {
            BranchStatus status = git.branch.status({ BranchStatusOp op ->
                op.name = branch.get()
            } as Configurable<BranchStatusOp>)
            return status.aheadCount > 0
        } catch (IllegalStateException e) {
            // if we're not tracking anything yet (i.e. orphan) we need to push
            return true
        }
    }
}
