package ru.vyarus.gradle.plugin.mkdocs.util

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Project
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileTree

/**
 * Utility to extract templates from classpath (plugin jar) and copy to target location with placeholders processing.
 *
 * @author Vyacheslav Rusakov
 * @since 05.12.2017
 */
@CompileStatic
class TemplateUtils {

    private static final String SLASH = '/'

    /**
     * Copies templates from classpath (inside the jar) with placeholders substitution.
     *
     * @param project project instance
     * @param path path to folder on classpath to copy (path must start with '/')
     * @param to target location (string, File)
     * @param tokens substitution tokens
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    static void copy(Project project, String path, Object to, Map<String, String> tokens) {
        URL folder = getUrl(path)
        FileTree tree
        boolean isJar = folder.toString().startsWith('jar:')
        if (isJar) {
            tree = project.zipTree(getJarUrl(folder))
        } else {
            tree = project.fileTree(folder)
        }

        project.copy {
            from tree
            into to
            if (isJar) {
                include pathToWildcard(path)
                // cut off path
                eachFile { FileCopyDetails f ->
                    f.path = f.path.replaceFirst(pathToCutPrefix(path), '')
                }
                includeEmptyDirs = false
            }
            filter(ReplaceTokens, tokens: tokens)
        }
    }

    private static URL getUrl(String path) {
        if (!path.startsWith(SLASH)) {
            throw new IllegalArgumentException("Path must be absolute (start with '/'): $path")
        }
        URL folder = TemplateUtils.getResource(path)
        if (folder == null) {
            // workaround for tests
            folder = Thread.currentThread().contextClassLoader.getResource(path.replaceAll('^/', ''))
        }
        if (folder == null) {
            throw new IllegalArgumentException("No resources found on path: $path")
        }
        return folder
    }

    private static URL getJarUrl(URL fileUrl) {
        return ((JarURLConnection) fileUrl.openConnection()).jarFileURL
    }

    private static String pathToWildcard(String path) {
        return (path.endsWith(SLASH) ? path : path + SLASH) + '**'
    }

    private static String pathToCutPrefix(String path) {
        String res = path[1..-1]
        return res.endsWith(SLASH) ? res : res + SLASH
    }
}
