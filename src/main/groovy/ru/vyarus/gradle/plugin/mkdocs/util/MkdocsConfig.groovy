package ru.vyarus.gradle.plugin.mkdocs.util

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Utility to work with mkdocs config (mkdocs.yml).
 *
 * @author Vyacheslav Rusakov
 * @since 07.12.2017
 */
@CompileStatic
class MkdocsConfig {

    private final Project project
    private final String configLocation

    MkdocsConfig(Project project, String sourceDir) {
        this.project = project
        this.configLocation = (sourceDir ? "$sourceDir/" : '') + 'mkdocs.yml'
    }

    /**
     * @return mkdocs config file
     * @throws GradleException if config does not exist
     */
    File getConfig() {
        File config = project.file(configLocation)
        if (!config.exists()) {
            throw new GradleException("Mkdocs config file not found: ${project.relativePath(config)}")
        }
        return config
    }

    /**
     * @param option config option to find
     * @return option value or null if option commented or not defined
     */
    String find(String option) {
        String line = config.readLines().find {
            it.startsWith("$option:")
        }
        if (line) {
            int pos = line.indexOf(':')
            // special case: no value defined on property
            String res = pos < line.length() - 1 ? line[pos + 1..-1].trim() : ''
            // remove quotes
            return res.replaceAll(/^['"]/, '').replaceAll(/['"]$/, '')
        }
        return null
    }

    /**
     * Replace option value in mkdocks config.
     *
     * @param option option name
     * @param value new option value
     */
    void set(String option, String value) {
        config.text = config.text.replaceAll(/(?m)^$option:.*/, "$option: $value")
    }

    /**
     * Backup configuration file.
     *
     * @return configuration backup file
     */
    File backup() {
        File backup = new File(config.parentFile, 'mkdocs.yml.bak')
        if (backup.exists()) {
            backup.delete()
        }
        backup << config.text
        return backup
    }

    /**
     * Replace current configuration file with provided backup.
     *
     * @param backup configuration backup
     * @throws IllegalStateException if backup file or config does not exists
     */
    void restoreBackup(File backup) {
        if (!backup.exists()) {
            throw new IllegalStateException("No backup file found: ${project.relativePath(backup)}")
        }
        File cfg = config
        cfg.delete()
        if (!backup.renameTo(cfg)) {
            throw new IllegalStateException("Failed to rename ${project.relativePath(backup.absolutePath)} back " +
                    "to ${project.relativePath(cfg.absolutePath)}. Please rename manually to recover.")
        }
    }
}
