package ru.vyarus.gradle.plugin.mkdocs.util

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.internal.file.FileOperations

import java.util.regex.Matcher

/**
 * Utility to work with mkdocs config (mkdocs.yml).
 *
 * @author Vyacheslav Rusakov
 * @since 07.12.2017
 */
@CompileStatic
class MkdocsConfig {

    private final FileOperations fs
    private final String configLocation

    MkdocsConfig(FileOperations fs, String sourceDir) {
        this.fs = fs
        this.configLocation = (sourceDir ? "$sourceDir/" : '') + 'mkdocs.yml'
    }

    /**
     * @return mkdocs config file
     * @throws GradleException if config does not exist
     */
    File getConfig() {
        File config = fs.file(configLocation)
        if (!config.exists()) {
            throw new GradleException("Mkdocs config file not found: ${fs.relativePath(config)}")
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
     * Searches for specified property. Supports nesting: if property contains dots then it will search for
     * each property part sequentially (note that multiline list value also counted as property).
     * <p>
     * Looks only not commented lines. Counts hierarchy.
     *
     * @param option option (maybe composite: separated with dots) name to find
     * @return true is string found, false otherwise
     */
    boolean contains(String option) {
        String[] parts = option.split('\\.')
        int i = 0
        int whitespace = 0
        String line = config.readLines().find {
            // line must not be commented, contain enough whitespace and required option part
            // allowed: [ prop, prop:, - prop, -prop ]
            if (!it.trim().startsWith('#') && it.find(
                    /${whitespace == 0 ? '^' : '\\s{' + whitespace + ',}'}(-\s{0,})?${parts[i]}(:|$|\s)/)) {
                if (whitespace == 0) {
                    whitespace++
                } else {
                    // count starting whitespace (to correctly recognize structure)
                    Matcher matcher = it =~ /^(\s+)/
                    if (!matcher.find()) {
                        throw new IllegalStateException("Failed to recognize preceeding whitespace in '$it'")
                    }
                    whitespace = matcher.group(1).length() + 1
                }
                i++
            }
            return i == parts.length
        }
        return line != null
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
            throw new IllegalStateException("No backup file found: ${fs.relativePath(backup)}")
        }
        File cfg = config
        cfg.delete()
        if (!backup.renameTo(cfg)) {
            throw new IllegalStateException("Failed to rename ${fs.relativePath(backup.absolutePath)} back " +
                    "to ${fs.relativePath(cfg.absolutePath)}. Please rename manually to recover.")
        }
    }
}
