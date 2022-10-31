package ru.vyarus.gradle.plugin.mkdocs

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 31.10.2022
 */
class VersionsUpdateKitTest extends AbstractKitTest {

    def "Check versions file creation with build"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'  
            
            mkdocs.publish.existingVersionsFile = 'prev-versions.json'
            
            python.scope = USER
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS

        when: "build site"
        result = run('mkdocsBuild')
        File vers = file('build/mkdocs/versions.json')
        println vers.text

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        vers.exists()
        result.output.contains('WARNING: configured versions file \'prev-versions.json\' does not exist - creating new file instead')
        result.output.contains('New version added: 1.0')
        result.output.contains('Versions written to file: 1.0')

    }

    def "Check not existing versions file url"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'  
            
            mkdocs.publish.existingVersionsFile = 'https://xvik.github.io/gradle-use-python-plugin/versionssss.json'
            
            python.scope = USER
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS

        when: "build site"
        result = run('mkdocsBuild')
        File vers = file('build/mkdocs/versions.json')
        println vers.text

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        vers.exists()
        result.output.contains('WARNING: configured versions file \'https://xvik.github.io/gradle-use-python-plugin/versionssss.json\' does not exist - creating new file instead')
        result.output.contains('New version added: 1.0')
        result.output.contains('Versions written to file: 1.0')
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
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS

        when: "build site"
        result = run('mkdocsBuild')
        File vers = file('build/mkdocs/versions.json')
        println vers.text

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        vers.exists()
        result.output.contains('Existing versions file \'prev-versions.json\' loaded with 2 versions')
        result.output.contains('New version added: 3.0')
        result.output.contains('Versions written to file: 3.0, 2.0.0, 1.0.0')

    }

    def "Check aliases update"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '3.0'  
            
            mkdocs.publish {
                versionAliases = ['latest']
                existingVersionsFile = 'prev-versions.json'
            }
            
            python.scope = USER
        """
        file('prev-versions.json') << '[{"version":"2.0.0","title":"2.0.0","aliases":["latest"]},{"version":"1.0.0","title":"1.0.0","aliases":[]}]'

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS

        when: "build site"
        result = run('mkdocsBuild')
        File vers = file('build/mkdocs/versions.json')
        println vers.text

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        vers.text == '[{"version":"3.0","title":"3.0","aliases":["latest"]},{"version":"2.0.0","title":"2.0.0","aliases":[]},{"version":"1.0.0","title":"1.0.0","aliases":[]}]'

    }

    def "Check version already declared"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '3.0'  
            
            mkdocs.publish {
                versionAliases = ['latest']
                existingVersionsFile = 'prev-versions.json'
            }
            
            python.scope = USER
        """
        file('prev-versions.json') << '[{"version":"3.0","title":"3.0","aliases":["latest"]},{"version":"2.0.0","title":"2.0.0","aliases":[]},{"version":"1.0.0","title":"1.0.0","aliases":[]}]'

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS

        when: "build site"
        result = run('mkdocsBuild')
        File vers = file('build/mkdocs/versions.json')
        println vers.text

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        vers.text == '[{"version":"3.0","title":"3.0","aliases":["latest"]},{"version":"2.0.0","title":"2.0.0","aliases":[]},{"version":"1.0.0","title":"1.0.0","aliases":[]}]'

    }

    def "Check versions file from url update"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'  
            
            mkdocs.publish.existingVersionsFile = 'https://xvik.github.io/gradle-use-python-plugin/versions.json'
            
            python.scope = USER
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS

        when: "build site"
        result = run('mkdocsBuild')
        File vers = file('build/mkdocs/versions.json')
        println vers.text

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        vers.exists()
        result.output.contains('Existing versions file \'https://xvik.github.io/gradle-use-python-plugin/versions.json\' loaded with')
        result.output.contains('New version added: 1.0')

    }
}
