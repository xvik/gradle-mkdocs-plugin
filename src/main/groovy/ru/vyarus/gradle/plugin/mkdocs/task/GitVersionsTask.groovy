package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import ru.vyarus.gradle.plugin.mkdocs.MkdocsExtension
import ru.vyarus.gradle.plugin.mkdocs.util.VersionsComparator
import ru.vyarus.gradle.plugin.mkdocs.util.VersionsFileUtils

import java.util.regex.Pattern

import static ru.vyarus.gradle.plugin.mkdocs.util.VersionsFileUtils.ALIASES
import static ru.vyarus.gradle.plugin.mkdocs.util.VersionsFileUtils.VERSIONS_FILE

/**
 * Generate versions.json file in documentation root folder by analyzing existing folders in git branch. File use
 * mike (https://github.com/jimporter/mike) format and required for version switcher activation (mike itself is not
 * required - theme just requires this file).
 * <p>
 * Task takes into account existing versions.json file, but removes all versions not actually present in branch
 * and adds all missed versions. Also, versions might be re-ordered. All custom version titles would survive update.
 *
 * @author Vyacheslav Rusakov
 * @since 03.12.2021
 */
@CompileStatic
class GitVersionsTask extends DefaultTask {

    // assume version must start with a digit, followed by dot (no matter what ending)
    private static final Pattern VERSION_FOLDER = Pattern.compile('\\d+(\\..+)?')
    private static final Comparator<String> VERSIONS_COMPARATOR = VersionsComparator.comparingVersions(false)

    @TaskAction
    void run() {
        MkdocsExtension extension = project.extensions.findByType(MkdocsExtension)
        if (!extension.publish.docPath || !extension.publish.generateVersionsFile) {
            // single version doc or versions generation disabled
            return
        }

        File repo = project.file(extension.publish.repoDir)

        List<String> versions = listRepoVersions(repo)
        // read file stored in repository
        File oldFile = new File(repo, VERSIONS_FILE)
        Map<String, Map<String, Object>> index = VersionsFileUtils.parse(oldFile)
        cleanupAliases(index, versions, extension)
        Report report = updateVersions(index, versions, extension)
        validateUpdate(index, repo, extension)

        // write into build directory (it would be incorrect to write directly to repo dir)
        File file = VersionsFileUtils.getTarget(project, extension)
        VersionsFileUtils.write(index, file)
        logger.lifecycle('Versions file generated with {} versions: {} \n{}',
                index.size(), file.absolutePath, composeReport(report))
    }

    private List<String> listRepoVersions(File repo) {
        List<String> versions = []
        int start = repo.absolutePath.length() + 1
        repo.listFiles()
                .findAll { it.directory && !it.name.startsWith('.') }
                .each { File it ->
                    List<File> roots = []
                    findRoots(roots, it)
                    roots.each {
                        // replace slashes for windows
                        versions.add(it.absolutePath.replace('\\', '/')[start..-1])
                    }
                }
        return versions
    }

    private void findRoots(List<File> paths, File current) {
        // recursively searching for directories containing 404.html
        // (using 404 instead of index because index files could be used for auto redirects)
        if (current.list().contains('404.html')) {
            paths.add(current)
        } else {
            current.listFiles().each {
                if (isValidDirectory(it)) {
                    findRoots(paths, it)
                }
            }
        }
    }

    private boolean isValidDirectory(File file) {
        // note: this automatically filters simple aliases like dev or latest, bit not aliases like "1.x"
        return file.directory && VERSION_FOLDER.matcher(file.name).matches()
    }

    /**
     * First, all aliases must be removed from the folders list (aliases info is stored in the versions file).
     * Then current alias must be removed from previous versions (e.g. if we publish new "latest" version,
     * this alias must be removed from previous "latest" version).
     *
     * @param file current versions file content
     * @param actual version folders found in repo
     * @param extension version configuration
     */
    private void cleanupAliases(Map<String, Map<String, Object>> file,
                                List<String> actual,
                                MkdocsExtension extension) {
        String[] currentAliases = extension.publish.versionAliases
        file.values().each {
            List aliases = it[ALIASES] as List
            // remove aliases from found repo directories
            actual.removeAll(aliases)
            // remove current aliases from old version
            if (currentAliases) {
                aliases.removeAll(currentAliases)
            }
        }
    }

    /**
     * First, remove all versions not found in repo from versions file. Then add all not mentioned folders
     * as new versions (recover versions file by repository state, but in this case it is impossible to recover
     * aliases).
     *
     * @param file current versions file content
     * @param actual version folders found in repo
     * @param extension version configuration
     * @return report of versions file modifications
     */
    private Report updateVersions(Map<String, Map<String, Object>> file,
                                  List<String> actual,
                                  MkdocsExtension extension) {
        Report report = new Report()
        report.removed.addAll(file.keySet())

        // remove all versions not found on fs
        file.keySet().removeIf { !actual.contains(it) }
        report.removed.removeAll(file.keySet())

        String currentVer = extension.resolveDocPath()
        // version would always be absent due to gitReset task (removing all outdated content before copying)
        if (!actual.contains(currentVer)) {
            actual.add(currentVer)
        }

        actual.each { ver ->
            // assume custom title for other versions will survive in file
            if (VersionsFileUtils.addVersion(file, ver)) {
                report.added.add(ver)
            }
        }

        // update custom title (to handle case when existing version re-generated)
        String currenTitle = extension.resolveVersionTitle() ?: currentVer
        VersionsFileUtils.updateVersion(file, currentVer, currenTitle, extension.publish.versionAliases)

        report.survived.addAll(actual)
        report.survived.removeAll(report.added)
        return report
    }

    /**
     * Using versions file, plugin could warn about older version overrides root redirect or alias folder.
     * It does not stop process with an error because versions comparison logic is way not ideal and plugin
     * could be simply wrong about this warnings.
     * <p>
     * Must be called after update so versions file would be completely ready.
     * <p>
     * NOTE: this validation will not be performed if versions file creation is disabled.
     */
    private void validateUpdate(Map<String, Map<String, Object>> file,
                                File repo,
                                MkdocsExtension extension) {
        String lastVersion = file.keySet().iterator().next()
        String currentVersion = extension.resolveDocPath()
        if (lastVersion != currentVersion) {
            StringBuilder errors = new StringBuilder()
            // then publishing version is not the latest
            if (extension.publish.rootRedirect) {
                errors.append("\troot redirect override to '${extension.resolveRootRedirectionPath()}'\n")
            }
            (file.get(currentVersion).get(ALIASES) as List<String>).each { String alias ->
                if ((new File(repo, alias)).exists()) {
                    errors.append("\texisting alias '$alias' override\n")
                }
            }
            if (errors.size() > 0) {
                logger.warn("\nWARNING: Publishing version '$currentVersion' is older then the latest published " +
                        "'$lastVersion' and the following overrides might not be desired: \n{}", errors.toString())
            }
        }
    }

    private String composeReport(Report report) {
        StringBuilder out = new StringBuilder()
        appendReportLine(out, report.added, 'new versions')
        appendReportLine(out, report.removed, 'removed from file')
        appendReportLine(out, report.survived, 'remains the same')
        // new version section would always contain at least just published version
        return out.toString()
    }

    private void appendReportLine(StringBuilder out, List<String> list, String name) {
        if (!list.empty) {
            list.sort(VERSIONS_COMPARATOR.reversed())
            out.append("\t$name: ").append(list.join(', ')).append('\n')
        }
    }

    private static class Report {
        List<String> added = []
        List<String> removed = []
        List<String> survived = []
    }
}

