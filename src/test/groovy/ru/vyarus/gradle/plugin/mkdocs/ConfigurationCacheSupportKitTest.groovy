package ru.vyarus.gradle.plugin.mkdocs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 10.04.2024
 */
class ConfigurationCacheSupportKitTest extends AbstractKitTest {

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
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn', 'mkdocsInit')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "build site"
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'mkdocsBuild')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        file('build/mkdocs/1.0-SNAPSHOT/index.html').exists()

        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'mkdocsBuild')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(':mkdocsBuild').outcome == TaskOutcome.UP_TO_DATE
    }

    def "Check versions file update"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '3.0'  
            
            mkdocs.publish.existingVersionsFile = 'prev-versions.json'
            
            python.scope = USER
        """
        file('prev-versions.json') << '[{"version":"2.0.0","title":"2.0.0","aliases":["latest"]},{"version":"1.0.0","title":"1.0.0","aliases":[]}]'

        when: "run init"
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn','mkdocsInit')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS

        when: "build site"
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'mkdocsBuild')
        File vers = file('build/mkdocs/versions.json')
        println vers.text

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        vers.exists()
        result.output.contains('Existing versions file \'prev-versions.json\' loaded with 2 versions')
        result.output.contains('New version added: 3.0')
        result.output.contains('Versions written to file: 3.0, 2.0.0, 1.0.0')


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'mkdocsBuild')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(':mkdocsBuild').outcome == TaskOutcome.UP_TO_DATE
    }
}
