package ru.vyarus.gradle.plugin.mkdocs.util

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

/**
 * Versions file utilities.
 *
 * @author Vyacheslav Rusakov
 * @since 30.10.2022
 */
@CompileStatic
class VersionsFileUtils {

    static final String VERSIONS_FILE = 'versions.json'
    static final String ALIASES = 'aliases'

    private static final Comparator<String> VERSIONS_COMPARATOR = VersionsComparator.comparingVersions(false)

    /**
     * @param file versions file to parse
     * @return parsed versions or empty map
     */
    static Map<String, Map<String, Object>> parse(File file) {
        // self-sorted
        Map<String, Map<String, Object>> res =
                new TreeMap<String, Map<String, Object>>(VERSIONS_COMPARATOR.reversed())

        if (file.exists()) {
            ((List<Map<String, Object>>) new JsonSlurper().parse(file))
                    .each { ver -> res.put((String) ver['version'], ver) }
        }
        return res
    }

    /**
     * @param file versions structure
     * @param version version to add
     * @return true if version added, false if version already present
     */
    @SuppressWarnings('SpaceAroundMapEntryColon')
    static boolean addVersion(Map<String, Map<String, Object>> file, String version) {
        boolean added = false
        if (!file.containsKey(version)) {
            file.put(version, ['version': version,
                               'title'  : version,
                               (ALIASES): [],])
            added = true
        }
        return added
    }

    /**
     * Updates version title and aliases (plus, removes used aliases from other versions).
     *
     * @param file versions structure
     * @param version taret version
     * @param title version title
     * @param aliases version aliases (may be null)
     */
    static void updateVersion(Map<String, Map<String, Object>> file,
                              String version, String title, List<String> aliases) {
        // remove current version aliases from all versions (avoid alias duplication)
        if (aliases) {
            file.values().each {
                List als = it[ALIASES] as List
                als.removeAll(aliases)
            }
        }
        // update custom title (to handle case when existing version re-generated)
        file.get(version).with {
            it.put('title', title)
            it.put(VersionsFileUtils.ALIASES, aliases as String[] ?: [])
        }
    }

    /**
     * @param buildDir mkdocs build dir
     * @return versions file in mkdocs build directory
     */
    static File getTarget(File buildDir) {
        return new File(buildDir, VERSIONS_FILE)
    }

    /**
     * Writes versions structure into version file (overriding its content).
     *
     * @param file version structure
     * @param target target file
     */
    static void write(Map<String, Map<String, Object>> file, File target) {
        List<Map<String, Object>> res = []
        // map was sorted
        res.addAll(file.values())

        String json = JsonOutput.toJson(res)
        // create parent dirs
        target.parentFile.mkdirs()
        target.newWriter().withWriter { w ->
            w << json
        }
    }
}
