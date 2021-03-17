package ru.vyarus.gradle.plugin.mkdocs.task

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.mkdocs.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 04.03.2021
 */
class ExtraVarsKitTest extends AbstractKitTest {

    def "Check variables support"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'                          
            
            python.scope = USER
            
            mkdocs {
                sourcesDir 'doc' 
                extras = [ 
                    'version': project.version,
                    'abra': 'sample' 
                ]                
            }
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
Abrakadabra {{ gradle.abra }}
"""

        when: "run build"
        BuildResult result = run('mkdocsBuild')

        then: "build success"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        file('build/mkdocs/1.0/index.html').exists()
        def idx = file('build/mkdocs/1.0/index.html').text
        idx.contains('Version 1.0')
        idx.contains('Abrakadabra sample')

        and: "data file removed"
        !file('doc/docs/_data').exists()
    }

    def "Check extradata plugin not active"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'                          
            
            python.scope = USER
            
            mkdocs {
                sourcesDir 'doc' 
                extras = [ 
                    'version': project.version
                ]                
            }
        """

        file('doc').mkdir()
        file('doc/mkdocs.yml') << """
site_name: test

plugins:
    - search
#    - markdownextradata

nav:
  - Home: index.md    
"""
        file('doc/docs').mkdir()
        file('doc/docs/index.md') << """
# Index page

Version {{ gradle.version }}
"""

        when: "run build"
        BuildResult result = runFailed('mkdocsBuild')

        then: "build success"
        result.output.contains('Gradle-defined extra properties require \'markdownextradata\' plugin active in')
        !file('build/mkdocs/1.0/index.html').exists()
    }

    def "Check data dir preserve"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'                          
            
            python.scope = USER
            
            mkdocs {
                sourcesDir 'doc' 
                extras = [ 
                    'version': project.version
                ]                
            }
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
"""

        file('doc/docs/_data').mkdir()
        file('doc/docs/_data/sample.yml') << """
foo: bar
"""

        when: "run build"
        BuildResult result = run('mkdocsBuild')

        then: "build success"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        file('build/mkdocs/1.0/index.html').exists()
        def idx = file('build/mkdocs/1.0/index.html').text
        idx.contains('Version 1.0')

        and: "data file removed, but dir remains"
        file('doc/docs/_data').exists()
        file('doc/docs/_data/sample.yml').exists()
        !file('doc/docs/_data/gradle.yml').exists()
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
        BuildResult result = run('mkdocsBuild')

        then: "build success"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        file('build/mkdocs/1.1/index.html').exists()
        def idx = file('build/mkdocs/1.1/index.html').text
        idx.contains('Version 1.0')
        idx.contains('Ver 1.1')

        and: "data file removed"
        !file('doc/docs/_data').exists()
    }
}
