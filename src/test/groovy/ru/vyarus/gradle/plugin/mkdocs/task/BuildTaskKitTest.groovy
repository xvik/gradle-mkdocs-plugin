package ru.vyarus.gradle.plugin.mkdocs.task

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.mkdocs.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 29.11.2017
 */
class BuildTaskKitTest extends AbstractKitTest {

    def "Check build"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'  
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "build site"
        result = run('mkdocsBuild')

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        result.output =~ /\[python\] python -m mkdocs build -c -d "[^"]+" -s/

        file('build/mkdocs/1.0/index.html').exists()
        file('build/mkdocs/index.html').exists()
        file('build/mkdocs/index.html').text.contains('URL=\'1.0\'')

        when: "up to date check"
        result = run('mkdocsBuild')

        then: "ok"
        result.task(':mkdocsBuild').outcome == TaskOutcome.UP_TO_DATE

    }

    def "Check build not default doc"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'  
            
            mkdocs.publish {
                // simulation case: updating docs for older version 
                docPath = '0.9'
                rootRedirect = false
            }
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "build site"
        result = run('mkdocsBuild')

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        file('build/mkdocs/0.9/index.html').exists()
        !file('build/mkdocs/index.html').exists()

    }

    def "Check non strict build"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            mkdocs.strict = false
        """
        file('src/doc/').mkdirs()

        when: "build site"
        BuildResult result = runFailed('mkdocsBuild')

        then: "command correct"
        result.task(':mkdocsBuild').outcome == TaskOutcome.FAILED
        !(result.output =~ /\[python\] python -m mkdocs build -c -d "[^"]+" -s/)
    }

    def "Check different source folder"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            mkdocs.sourcesDir = 'docs'
        """
        file('docs/').mkdirs()

        when: "build site"
        BuildResult result = runFailed('mkdocsBuild')

        then: "correct source path used"
        result.task(':mkdocsBuild').outcome == TaskOutcome.FAILED
        result.output.contains("Config file '${file('docs/mkdocs.yml').canonicalPath}' does not exist.")
    }
}
