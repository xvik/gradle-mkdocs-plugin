package ru.vyarus.gradle.plugin.mkdocs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 13.03.2020
 */
class LegacyKitTest extends AbstractKitTest {

    String GRADLE_VERSION = '7.0'

    def "Check workflow"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0-SNAPSHOT'
            python.scope = USER
        """

        when: "run init"
        BuildResult result = runVer(GRADLE_VERSION, 'mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "build site"
        result = runVer(GRADLE_VERSION, 'mkdocsBuild')

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        file('build/mkdocs/1.0-SNAPSHOT/index.html').exists()

    }

    def "Check custom task"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            python.scope = USER
            
            task mkHelp(type: MkdocsTask) {
                command = '--help'
            }            
        """

        when: "run help"
        BuildResult result = runVer(GRADLE_VERSION, 'mkHelp')

        then: "executed"
        result.task(':mkHelp').outcome == TaskOutcome.SUCCESS
        result.output.contains('-V, --version  Show the version and exit.')
    }
}
