package ru.vyarus.gradle.plugin.mkdocs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 14.10.2018
 */
class DocsInModuleKitTest extends AbstractKitTest {

    def "Check docs submodule workflow"() {
        setup:
        file('settings.gradle') << ' include "doc"'
        file('doc').mkdir()
        build """
            plugins {
                id 'ru.vyarus.mkdocs' apply false                                
            }
            
            version = '1.0-SNAPSHOT'
            
            project(':doc') {
                apply plugin: 'ru.vyarus.mkdocs' 
                
                mkdocs {
                    sourcesDir = 'src'
                }
            }
                        
        """

        when: "run init"
        BuildResult result = run(':doc:mkdocsInit')

        then: "docs created"
        result.task(':doc:mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('doc/src/mkdocs.yml').exists()

        when: "build site"
        result = run(':doc:mkdocsBuild')

        then: "built"
        result.task(':doc:mkdocsBuild').outcome == TaskOutcome.SUCCESS
        file('doc/build/mkdocs/1.0-SNAPSHOT/index.html').exists()

    }
}
