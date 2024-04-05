package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import ru.vyarus.gradle.plugin.mkdocs.MkdocsExtension
import ru.vyarus.gradle.plugin.mkdocs.util.MkdocsConfig
import ru.vyarus.gradle.plugin.python.task.PythonTask

/**
 * General mkdocs task.
 * <p>
 * Provides support for gradle-driven variables. When variables be declared in {@code mkdocs.extras} map,
 * task would generate special data file: {@code [mkdocs.yml location]/docs/_data/gradle.yml}.
 * Markdownextradata plugin must be activated in mkdocs.yml (exception will be thrown if not). Plugin searches
 * by default in @{code [mkdocs.yml location]/docs/_data/} dir for variable files (no magic behaviour).
 * In documentation variables may be used as <pre>{{ gradle.var_name }}</pre>.
 *
 * @author Vyacheslav Rusakov
 * @since 13.11.2017
 * @see <a href="https://github.com/rosscdh/mkdocs-markdownextradata-plugin">markdownextradata plugin documentation</a>
 */
@CompileStatic
@SuppressWarnings(['AbstractClassWithoutAbstractMethod', 'AbstractClassWithPublicConstructor'])
abstract class MkdocsTask extends PythonTask {

    /**
     * Extra gradle-provided variables to use in documentation.
     */
    @Internal
    // marked as internal because since gradle 7.4 it is impossible to have map property (see getExtrasCacheKey)
    // note that MapProperty can't be used too as it has its own problems
    Map<String, Serializable> extras

    MkdocsTask() {
        // restrict commands to mkdocs module
        module.set('mkdocs')
    }
/**
     * Used ONLY to provide gradle a cache key for extras property, which has to be internal because since gradle
     * 7.4 it is impossible to use {@link Nested} with {@link Map} property (all properties must have explicit
     * annotation, which is impossible in case of map).
     *
     * @return extras map manual serialization for caching
     */
    @Input
    @Optional
    String getExtrasCacheKey() {
        getExtras() != null ? getExtras().collect { k, v -> k + '=' + v }.join(', ') : null
    }

    @Override
    @SuppressWarnings('UnnecessaryGetter')
    void run() {
        if (getExtras() == null || getExtras().isEmpty()) {
            // no vars - simple run
            super.run()
        } else {
            runWithVariables()
        }
    }

    /**
     * For use in specialized tasks.
     *
     * @return mkdocs extension object
     */
    @Internal
    protected MkdocsExtension getExtension() {
        project.extensions.findByType(MkdocsExtension)
    }

    private void runWithVariables() {
        File data = resolveDataDir()
        boolean removeDataDir = !data.exists()
        File gen = new File(data, 'gradle.yml')
        try {
            if (removeDataDir) {
                data.mkdirs()
            }
            // assuming this file owned by gradle exclusively and may remain only because of incorrect task shutdown
            if (gen.exists()) {
                gen.delete()
            }
            logger.lifecycle('Generating mkdocs data file: {}', getFilePath(gen))
            String report = ''
            gen.withWriter { BufferedWriter writer ->
                getExtras().each { k, v ->
                    // Object value used for deferred evaluation (GString may use lazy placeholders)
                    String line = k.replaceAll('[ -]', '_') + ': ' + (v ?: '')
                    writer.writeLine(line)
                    report += "\t$line\n"
                }
            }
            logger.lifecycle(report)
            super.run()
        } finally {
            gen.delete()
            if (removeDataDir) {
                data.delete()
            }
        }
    }

    private File resolveDataDir() {
        MkdocsConfig config = new MkdocsConfig(project, extension.sourcesDir)

        if (!config.contains('plugins.markdownextradata')) {
            throw new GradleException(
                    'Gradle-defined extra properties require \'markdownextradata\' plugin active in ' +
                            'your mkdocs.yml file, which is currently not the case. \nEither remove extra properties ' +
                            'declaration (in build.gradle) or declare plugin (in mkdocs.yml) like this: \n' +
                            'plugins:\n' +
                            '   - search\n' +
                            '   - markdownextradata')
        }

        // mkdocs.yml location
        File root = project.file(extension.sourcesDir)

        // configuration may override default "docs" location
        String docsPath = config.find('docs_dir') ?: 'docs'
        File docs = new File(docsPath)
        // docs_dir config value may contain absolute path declaration
        if (!docs.absolute) {
            docs = new File(root, docs.path)
        }

        return new File(docs, '_data')
    }

    /**
     * Looks if file inside project and relative path would be reasonable, otherwise return absolute path.
     *
     * @param file file
     * @return relative or absolute file path
     */
    private String getFilePath(File file) {
        if (file.path.startsWith(project.rootDir.path)) {
            return project.relativePath(file)
        }
        return file.absolutePath
    }
}
