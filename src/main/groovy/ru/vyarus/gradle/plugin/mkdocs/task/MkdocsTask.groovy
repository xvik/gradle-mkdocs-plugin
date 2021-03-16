package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
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
class MkdocsTask extends PythonTask {

    /**
     * Extra gradle-provided variables to use in documentation.
     */
    @Nested
    @Optional
    Map<String, Serializable> extras

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

    @Override
    @SuppressWarnings('GetterMethodCouldBeProperty')
    String getModule() {
        // restrict commands to mkdocs module
        return 'mkdocs'
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

        // data file generation
        File root = project.file(extension.sourcesDir)
        File data = new File(root, 'docs/_data')
        if (!data.parentFile.exists()) {
            // docs/ dir must be present (dir with actual documentation sources)
            throw new GradleException(
                    "Mkdocs documentation sources directory not found: ${project.relativePath(data.parentFile)}")
        }
        boolean removeDataDir = !data.exists()
        File gen = new File(data, 'gradle.yml')
        try {
            if (removeDataDir) {
                data.mkdir()
            }
            // assuming this file owned by gradle exclusively and may remain only because of incorrect task shutdown
            if (gen.exists()) {
                gen.delete()
            }
            logger.lifecycle('Generating mkdocs data file: {}', project.relativePath(gen))
            String report = ''
            gen.withWriter { BufferedWriter writer ->
                getExtras().each { k, v ->
                    // Object value used for deferred evaluation (GString may use lazy placeholders)
                    String line = k.replace(' ', '_') + ': ' + (v ?: '')
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
}
