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

    def "Check full plugin workflow"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
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
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn','mkHelp')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "executed"
        result.task(':mkHelp').outcome == TaskOutcome.SUCCESS
        result.output.contains('-V, --version  Show the version and exit.')


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'mkHelp')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(':mkHelp').outcome == TaskOutcome.SUCCESS
        result.output.contains('-V, --version  Show the version and exit.')
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


    def "Check variables lazy declaration"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = 1.0                          
            
            python.scope = USER
            
            mkdocs {
                sourcesDir 'doc' 
                extras = [ 
                    'version': project.version,
                    'ver-ver': "\${->project.version}" 
                ]                
            }
            
            version = 1.1
        """

        file('doc').mkdir()
        file('doc/mkdocs.yml') << """
site_name: test

plugins:
    - search
    - markdownextradata

nav:
  - Home: index.md    
"""
        file('doc/docs').mkdir()
        file('doc/docs/index.md') << """
# Index page

Version {{ gradle.version }}
Ver {{ gradle.ver_ver }}
"""

        when: "run build"
        BuildResult result = run('--configuration-cache', '--configuration-cache-problems=warn', 'mkdocsBuild')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "build success"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        file('build/mkdocs/1.1/index.html').exists()
        def idx = file('build/mkdocs/1.1/index.html').text
        idx.contains('Version 1.0')
        idx.contains('Ver 1.1')

        and: "data file removed"
        !file('doc/docs/_data').exists()


        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'mkdocsBuild')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(':mkdocsBuild').outcome == TaskOutcome.UP_TO_DATE
    }
}
