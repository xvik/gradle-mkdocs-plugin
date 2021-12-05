package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import ru.vyarus.gradle.plugin.mkdocs.MkdocsExtension

import java.util.regex.Pattern

/**
 * Generate versions.json file in documentation root folder. File use mike (https://github.com/jimporter/mike)
 * format and required for version switcher activation (mike itself is not required - theme just requires this file).
 *
 * @author Vyacheslav Rusakov
 * @since 03.12.2021
 */
@CompileStatic
class VersionsTask extends DefaultTask {

    private static final String VERSIONS_FILE = 'versions.json'
    private static final String ALIASES = 'aliases'
    // assume version must start with a digit, followed by dot (no matter what ending)
    private static final Pattern VERSION_FOLDER = Pattern.compile('\\d+(\\..+)?')

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
        File oldFile = new File(project.file(extension.publish.repoDir), VERSIONS_FILE)
        Map<String, Map<String, Object>> index = parseExistingFile(oldFile)
        cleanupAliases(index, versions, extension)
        Report report = updateVersions(index, versions, extension)

        // write into build directory (it would be incorrect to write directly to repo dir)
        File file = new File(project.file(extension.buildDir), VERSIONS_FILE)
        writeVersions(index, file)
        logger.lifecycle('Versions file generated with {} versions: {} \n{}',
                index.size(), file.absolutePath, composeReport(report))
    }

    private List<String> listRepoVersions(File repo) {
        List<String> versions = []
        int start = repo.absolutePath.length() + 1
        if (repo.exists()) {
            repo.listFiles()
                    .findAll { it.directory && !it.name.startsWith('.') }
                    .forEach { File it ->
                        List<File> roots = []
                        findRoots(roots, it)
                        roots.each {
                            // replace slashes for windows
                            versions.add(it.absolutePath.replace('\\', '/')[start..-1])
                        }
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

    private Map<String, Map<String, Object>> parseExistingFile(File file) {
        // self-sorted
        Map<String, Map<String, Object>> res =
                new TreeMap<String, Map<String, Object>>(Comparator.<String> reverseOrder())

        if (file.exists()) {
            ((List<Map<String, Object>>) new JsonSlurper().parse(file))
                    .each { ver -> res.put((String) ver['version'], ver) }
        }
        return res
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
    @SuppressWarnings('SpaceAroundMapEntryColon')
    private Report updateVersions(Map<String, Map<String, Object>> file,
                                  List<String> actual,
                                  MkdocsExtension extension) {
        Report report = new Report()
        report.removed.addAll(file.keySet())

        // remove all versions not found on fs
        file.keySet().removeIf { !actual.contains(it) }
        report.removed.removeAll(file.keySet())

        String currentVer = extension.resolveDocPath()
        // version generation called before copying so version will exist in case of docs update and will not
        // for initial version generation
        if (!actual.contains(currentVer)) {
            actual.add(currentVer)
        }

        actual.each { ver ->
            if (!file.containsKey(ver)) {
                // assume custom title for other versions will survive in file

                file.put(ver, ['version': ver,
                               'title'  : ver,
                               (ALIASES): [],])
                report.added.add(ver)
            }
        }

        // update custom title (to handle case when existing version re-generated)
        String currenTitle = extension.resolveVersionTitle() ?: currentVer
        file.get(currentVer).with {
            it.put('title', currenTitle)
            if (extension.publish.versionAliases) {
                it.put(VersionsTask.ALIASES, extension.publish.versionAliases)
            }
        }

        report.survived.addAll(actual)
        report.survived.removeAll(report.added)
        return report
    }

    private void writeVersions(Map<String, Map<String, Object>> content, File file) {
        List<Map<String, Object>> res = []
        // map was sorted
        res.addAll(content.values())

        String json = JsonOutput.toJson(res)
        // create parent dirs
        file.parentFile.mkdirs()
        file.newWriter().withWriter { w ->
            w << json
        }
    }

    private String composeReport(Report report) {
        StringBuilder out = new StringBuilder()
        appendReportLine(out, report.added, 'new versions')
        appendReportLine(out, report.removed, 'removed from file')
        appendReportLine(out, report.survived, 'remains the same')

        if (out.length() == 0) {
            out.append('\tno version changes\n')
        }
        return out.toString()
    }

    private void appendReportLine(StringBuilder out, List<String> list, String name) {
        if (!list.empty) {
            out.append("\t$name: ").append(list.join(', ')).append('\n')
        }
    }

    private static class Report {
        List<String> added = []
        List<String> removed = []
        List<String> survived = []
    }
}

