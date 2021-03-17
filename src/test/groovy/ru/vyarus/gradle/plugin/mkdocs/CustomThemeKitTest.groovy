package ru.vyarus.gradle.plugin.mkdocs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 17.03.2021
 */
class CustomThemeKitTest extends AbstractKitTest {

    def "Check theme change"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'                          
            
            python.pip 'mkdocs-ivory:0.4.6'
            
            mkdocs {
                sourcesDir 'doc'                        
            }
        """

        file('doc').mkdir()
        file('doc/mkdocs.yml') << """
site_name: test

theme:
  name: 'ivory'

nav:
  - Home: index.md    
"""
        file('doc/docs').mkdir()
        file('doc/docs/index.md') << """
# Index page
"""

        when: "run build"
        BuildResult result = run('mkdocsBuild')

        then: "build success"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        file('build/mkdocs/1.0/index.html').exists()
    }
}
