package ru.vyarus.gradle.plugin.mkdocs.task

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.mkdocs.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 29.11.2017
 */
class BuildTaskKitTest extends AbstractKitTest {

    def "Check build"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'  
            
            python.scope = USER
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS

        when: "build site"
        result = run('mkdocsBuild')

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        result.output =~ /\[python] python(3)? -m mkdocs build -d ${isWin ? '"[^"]+"' : '[^(-)]+'} -c -s/

        file('build/mkdocs/1.0/index.html').exists()
        // site_url wasn't modified
        file('build/mkdocs/1.0/sitemap.xml').text.contains('<loc>/guide/installation/</loc>')
        file('build/mkdocs/index.html').exists()
        file('build/mkdocs/index.html').text.contains('URL=\'1.0\'')

        when: "up to date check"
        result = run('mkdocsBuild')

        then: "ok"
        result.task(':mkdocsBuild').outcome == TaskOutcome.UP_TO_DATE

    }

    def "Check build path with space"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = 'v 1.0'  
            
            python.scope = USER
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "build site"
        result = run('mkdocsBuild')
        println(file('build/mkdocs/').list())

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        result.output =~ /\[python] python(3)? -m mkdocs build -d ${isWin ? '"[^"]+"' : '[^(-)]+'} -c -s/
        file('build/mkdocs/v 1.0/index.html').exists()
    }


    def "Check build not default doc"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'  
            
            python.scope = USER
            
            mkdocs.publish {
                // simulation case: updating docs for older version 
                docPath = '0.9'
                rootRedirect = false
            }
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
        file('build/mkdocs/0.9/index.html').exists()
        !file('build/mkdocs/index.html').exists()

    }

    def "Check non strict build"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            python.scope = USER
            
            mkdocs.strict = false
        """
        file('src/doc/').mkdirs()

        when: "build site"
        BuildResult result = runFailed('mkdocsBuild')

        then: "command correct"
        result.task(':mkdocsBuild').outcome == TaskOutcome.FAILED
        !(result.output =~ /\[python] python(3)? -m mkdocs build -c -d ${isWin ? '"[^"]+"' : '[^(-)]+'} -s/)
    }

    def "Check different source folder"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            python.scope = USER
            
            mkdocs.sourcesDir = 'docs'
        """
        file('docs/').mkdirs()

        when: "build site"
        BuildResult result = runFailed('mkdocsBuild')

        then: "correct source path used"
        result.task(':mkdocsBuild').outcome == TaskOutcome.FAILED
        result.output.contains("Mkdocs config file not found: docs${isWin ? '\\' : '/'}mkdocs.yml")
    }


    def "Check stale index.html remove"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }      
            
            python.scope = USER      
        """
        file('docs/').mkdirs()

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "build site"
        result = run('mkdocsBuild')

        then: "redirect index generated"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        file('build/mkdocs/index.html').exists()

        when: "building without redirect"
        buildFile.delete()
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            python.scope = USER
            
            mkdocs.publish.rootRedirect = false            
        """
        file('src/doc/docs/index.md') << 'overwrite file'
        result = run('mkdocsBuild')

        then: "index.html removed"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        !file('build/mkdocs/index.html').exists()
    }

    def "Check up to date"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'
            
            python.scope = USER
        """
        file('src/doc/').mkdirs()

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "build site"
        result = run('mkdocsBuild')

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS

        when: "once again build"
        result = run('mkdocsBuild')

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.UP_TO_DATE

    }
}
