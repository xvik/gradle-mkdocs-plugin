package ru.vyarus.gradle.plugin.mkdocs.task

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.mkdocs.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 29.11.2017
 */
class ServeTaskKitTest extends AbstractKitTest {

    def "Check serve"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            python.scope = USER
        """
        file('src/doc/').mkdirs()

        when: "serve site"
        BuildResult result = runFailed('mkdocsServe')

        then: "command correct"
        result.task(':mkdocsServe').outcome == TaskOutcome.FAILED
        result.output =~ /\[python] python(3)? -m mkdocs serve -s/
    }

    def "Check non strict serve"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            python.scope = USER
            
            mkdocs.strict = false
        """
        file('src/doc/').mkdirs()

        when: "serve site"
        BuildResult result = runFailed('mkdocsServe')

        then: "command correct"
        result.task(':mkdocsServe').outcome == TaskOutcome.FAILED
        !(result.output =~ /\[python] python(3)? -m mkdocs serve -s/)
    }

    def "Check different source folder"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            python.scope = USER
            
            mkdocs.sourcesDir = 'docs'
        """
        file('docs/').mkdirs()

        when: "serve site"
        BuildResult result = runFailed('mkdocsServe')

        then: "correct source path used"
        result.task(':mkdocsServe').outcome == TaskOutcome.FAILED
        result.output.contains("Config file 'mkdocs.yml' does not exist.")
    }
}
