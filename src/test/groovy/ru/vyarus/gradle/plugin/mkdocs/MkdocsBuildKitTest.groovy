package ru.vyarus.gradle.plugin.mkdocs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 28.10.2022
 */
class MkdocsBuildKitTest extends AbstractKitTest {

    def "Check workflow"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs-build'                                
            }
            
            version = '1.0-SNAPSHOT'
            python.scope = USER
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
        file('build/mkdocs/1.0-SNAPSHOT/index.html').exists()

    }

    def "Check custom task"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs-build'                                
            }
            
            python.scope = USER
            
            task mkHelp(type: MkdocsTask) {
                command = '--help'
            }            
        """

        when: "run help"
        BuildResult result = run('mkHelp')

        then: "executed"
        result.task(':mkHelp').outcome == TaskOutcome.SUCCESS
        result.output.contains('-V, --version  Show the version and exit.')
    }
}
