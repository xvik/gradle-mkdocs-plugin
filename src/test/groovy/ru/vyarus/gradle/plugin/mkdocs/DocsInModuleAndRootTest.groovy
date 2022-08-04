package ru.vyarus.gradle.plugin.mkdocs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 04.08.2022
 */
class DocsInModuleAndRootTest extends AbstractKitTest {

    def "Check docs in root and submodule"() {

        setup:
        file('settings.gradle') << ' include "doc"'
        file('doc').mkdir()
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0-SNAPSHOT'
            
            mkdocs {
                sourcesDir = 'docs'
            }
            
            project(':doc') {
                apply plugin: 'ru.vyarus.mkdocs' 
                
                mkdocs {
                    sourcesDir = 'src'
                }
            }
                        
        """

        // root docs
        file('docs').mkdir()
        file('docs/mkdocs.yml') << """
site_name: root

nav:
  - Home: index.md    
"""
        file('docs/docs').mkdir()
        file('docs/docs/index.md') << """
root index page
"""

        // module docs
        file('doc').mkdir()
        file('doc/src').mkdir()
        file('doc/src/mkdocs.yml') << """
site_name: sub

nav:
  - Home: index.md    
"""
        
        file('doc/src/docs').mkdir()
        file('doc/src/docs/index.md') << """
submodule index page
"""

        when: "build site"
        BuildResult result = run('mkdocsBuild')

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        result.task(':doc:mkdocsBuild').outcome == TaskOutcome.SUCCESS
        file('build/mkdocs/1.0-SNAPSHOT/index.html').text.contains('root index page')
        file('doc/build/mkdocs/1.0-SNAPSHOT/index.html').text.contains('submodule index page')
    }
}
