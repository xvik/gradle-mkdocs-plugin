package ru.vyarus.gradle.plugin.mkdocs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 29.10.2017
 */
class MkdocsPluginKitTest extends AbstractKitTest {

    def "Check plugin execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'
            }
        """

        when: "run task"
        BuildResult result = run('mkdocsInit')

        then: "task successful"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
    }
}