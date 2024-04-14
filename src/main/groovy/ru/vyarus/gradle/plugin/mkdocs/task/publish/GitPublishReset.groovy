package ru.vyarus.gradle.plugin.mkdocs.task.publish

import groovy.transform.CompileStatic
import org.ajoberstar.grgit.Configurable
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Ref
import org.ajoberstar.grgit.operation.*
import org.eclipse.jgit.transport.URIish
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.gradle.api.tasks.util.PatternFilterable
import ru.vyarus.gradle.plugin.mkdocs.service.GrgitService

import javax.inject.Inject
import java.nio.file.Files
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Reset/checkout repo dir task. Based on https://github.com/ajoberstar/gradle-git-publish.
 *
 * @author Vyacheslav Rusakov
 * @since 08.04.2024
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithPublicConstructor')
abstract class GitPublishReset extends DefaultTask {

    public static final String REFERENCE = 'reference'
    public static final String ORIGIN = 'origin'
    public static final String HEAD_REFS = 'refs/heads/'
    public static final String NONE = 'none'
    public static final String ORIGIN_ = 'origin/'

    /**
     * Directory to checkout repository.
     */
    @OutputDirectory
    abstract DirectoryProperty getRepoDirectory()

    /**
     * Repository to publish into (must exists).
     */
    @Input
    abstract Property<String> getRepoUri()

    /**
     * (Optional) Where to fetch from prior to fetching from the remote (i.e. a local repo to save time).
     */
    @Input
    @Optional
    abstract Property<String> getReferenceRepoUri()

    /**
     * Target branch (would be created if does not exists).
     */
    @Input
    abstract Property<String> getBranch()

    /**
     * Repository user name (for authorization) or github token. See "org.ajoberstar.grgit.auth.username" property
     * https://ajoberstar.org/grgit/main/grgit-authentication.html
     */
    @Input
    @Optional
    abstract Property<String> getUsername()

    /**
     * Repository password. See "org.ajoberstar.grgit.auth.password" property
     * https://ajoberstar.org/grgit/main/grgit-authentication.html
     */
    @Input
    @Optional
    abstract Property<String> getPassword()

    @Internal
    abstract Property<GrgitService> getGrgit()

    @Inject
    abstract FileOperations getFs()

    @Internal
    PatternFilterable preserve

    GitPublishReset() {
        // always consider this task out of date
        this.outputs.upToDateWhen({ t -> false } as Spec<? super Task>)
    }

    @TaskAction
    void reset() {
        setupAuth()
        Grgit git = findExistingRepo().orElseGet(() -> freshRepo())
        grgit.get().grgit = git

        fetchReference(git)

        if (isRemoteBranchExists(git, ORIGIN)) {
            resetRemote(git)
        } else {
            // create a new orphan branch
            git.checkout({ CheckoutOp op ->
                op.branch = branch.get()
                op.orphan = true
            } as Configurable<CheckoutOp>)
        }

        cleanupFiles(git)

        // stage the removals, relying on dirs not being tracked by git
        git.add({ AddOp op ->
            op.patterns = Stream.of('.').collect(Collectors.toSet())
            op.update = true
        } as Configurable<AddOp>)
    }

    private void setupAuth() {
        // https://ajoberstar.org/grgit/main/grgit-authentication.html
        // IMPORTANT: this will set system properties which ALL consequent tasks would use (including push)
        if (username.present) {
            System.setProperty('org.ajoberstar.grgit.auth.username', username.get())
        }
        if (password.present) {
            System.setProperty('org.ajoberstar.grgit.auth.password', password.get())
        }
    }

    @SuppressWarnings(['UnnecessaryPackageReference', 'CatchException'])
    private java.util.Optional<Grgit> findExistingRepo() {
        try {
            return java.util.Optional.of(Grgit.open({ OpenOp op -> op.dir = repoDirectory.get().asFile
            } as Configurable<OpenOp>))
                    .filter { Grgit repo ->
                        boolean valid = isRemoteUriMatch(repo, ORIGIN, repoUri.get())
                                && (!referenceRepoUri.present
                                || isRemoteUriMatch(repo, REFERENCE, referenceRepoUri.get()))
                                && branch.get() == repo.branch.current().name
                        if (!valid) {
                            repo.close()
                        }
                        return valid
                    }
        } catch (Exception e) {
            // missing, invalid, or corrupt repo
            logger.debug('Failed to find existing Git publish repository.', e)
            return java.util.Optional.empty()
        }
    }

    protected Grgit freshRepo() {
        fs.delete(repoDirectory.get().asFile)

        Grgit repo = Grgit.init({ InitOp op ->
            op.dir = repoDirectory.get().asFile
        } as Configurable<InitOp>)
        repo.remote.add({ RemoteAddOp op ->
            op.name = ORIGIN
            op.url = repoUri.get()
        } as Configurable<RemoteAddOp>)
        if (referenceRepoUri.present) {
            repo.remote.add({ RemoteAddOp op ->
                op.name = REFERENCE
                op.url = referenceRepoUri.get()
            } as Configurable<RemoteAddOp>)
        }
        return repo
    }

    @SuppressWarnings('ThrowRuntimeException')
    private boolean isRemoteUriMatch(Grgit grgit, String remoteName, String remoteUri) {
        try {
            String currentRemoteUri = grgit.remote.list().stream()
                    .filter { remote -> (remote.name == remoteName) }
                    .map { remote -> remote.url }
                    .findAny()
                    .orElse(null)

            // need to use the URIish to normalize them and ensure we support all Git compatible URI-ishs (URL
            // is too limiting)
            return new URIish(remoteUri) == new URIish(currentRemoteUri)
        } catch (URISyntaxException e) {
            throw new RuntimeException('Invalid URI.', e)
        }
    }

    private boolean isRemoteBranchExists(Grgit git, String type) {
        Map<Ref, String> referenceBranches = git.lsremote({ LsRemoteOp op ->
            op.remote = type
            op.heads = true
        } as Configurable<LsRemoteOp>)

        return referenceBranches.keySet().stream()
                .anyMatch { ref -> (ref.fullName == HEAD_REFS + branch.get()) }
    }

    private void fetchReference(Grgit git) {
        if (referenceRepoUri.present) {
            String pubBranch = branch.get()

            if (isRemoteBranchExists(git, REFERENCE)) {
                logger.info('Fetching from reference repo: ' + referenceRepoUri.get())
                git.fetch({ FetchOp op ->
                    op.refSpecs = Arrays.asList(
                            String.format('+refs/heads/%s:refs/remotes/reference/%s', pubBranch, pubBranch))
                    op.tagMode = NONE
                } as Configurable<FetchOp>)
            }
        }
    }

    private void resetRemote(Grgit git) {
        String pubBranch = branch.get()
        // fetch only the existing pages branch
        git.fetch({ FetchOp op ->
            logger.info('Fetching from remote repo: ' + repoUri.get())
            op.refSpecs = Arrays.asList(
                    String.format('+refs/heads/%s:refs/remotes/origin/%s', pubBranch, pubBranch))
            op.tagMode = NONE
        } as Configurable<FetchOp>)

        // make sure local branch exists
        if (!git.branch.list().stream().anyMatch { branch -> (branch.name == pubBranch) }) {
            git.branch.add({ BranchAddOp op ->
                op.name = pubBranch
                op.startPoint = ORIGIN_ + pubBranch
            } as Configurable<BranchAddOp>)
        }

        // get to the state the remote has
        git.clean({ CleanOp op ->
            op.directories = true
            op.ignore = false
        } as Configurable<CleanOp>)
        git.checkout({ CheckoutOp op -> op.setBranch(pubBranch) } as Configurable<CheckoutOp>)
        git.reset({ ResetOp op ->
            op.commit = ORIGIN_ + pubBranch
            op.mode = 'hard'
        } as Configurable<ResetOp>)
    }

    private void cleanupFiles(Grgit git) {
        // clean up unwanted files
        FileTree repoTree = fs.fileTree(git.repository.rootDir)
        FileTree preservedTree = repoTree.matching(preserve)
        FileTree unwantedTree = (repoTree - preservedTree).asFileTree
        unwantedTree.visit(new FileVisitor() {
            @Override
            void visitDir(FileVisitDetails fileVisitDetails) {
                // do nothing
            }

            @Override
            void visitFile(FileVisitDetails fileVisitDetails) {
                try {
                    Files.delete(fileVisitDetails.file.toPath())
                } catch (IOException e) {
                    throw new UncheckedIOException(e)
                }
            }
        })
    }
}
