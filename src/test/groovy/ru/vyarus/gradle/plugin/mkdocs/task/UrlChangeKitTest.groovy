package ru.vyarus.gradle.plugin.mkdocs.task

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.mkdocs.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 07.12.2017
 */
class UrlChangeKitTest extends AbstractKitTest {

    def "Check site url change"() {
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
        File conf = file('src/doc/mkdocs.yml')
        conf.exists()

        when: "build site"
        conf.text = 'site_url: http://some-url.com\r\n' + conf.text
        result = run('mkdocsBuild')

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        result.output =~ /\[python] python(3)? -m mkdocs build -d ${isWin ? '"[^"]+"' : '[^(-)]+'} -c -s/

        file('build/mkdocs/1.0/index.html').exists()
        // site_url modified
        file('build/mkdocs/1.0/sitemap.xml').text.contains('<loc>http://some-url.com/1.0/guide/installation/</loc>')
        file('build/mkdocs/index.html').exists()
        file('build/mkdocs/index.html').text.contains('URL=\'1.0\'')

        then: "config was reverted"
        !conf.text.contains('http://some-url.com/1.0')
        !file('src/doc/mkdocs.yml.bak').exists()
    }

    def "Check disabled site url change"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'  
            
            python.scope = USER
            
            mkdocs.updateSiteUrl = false
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        File conf = file('src/doc/mkdocs.yml')
        conf.exists()

        when: "build site"
        conf.text = 'site_url: http://some-url.com\n' + conf.text
        result = run('mkdocsBuild')

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        result.output =~ /\[python] python(3)? -m mkdocs build -d ${isWin ? '"[^"]+"' : '[^(-)]+'} -c -s/

        file('build/mkdocs/1.0/index.html').exists()
        // site_url wasn't modified
        file('build/mkdocs/1.0/sitemap.xml').text.contains('<loc>http://some-url.com/guide/installation/</loc>')
        file('build/mkdocs/index.html').exists()
        file('build/mkdocs/index.html').text.contains('URL=\'1.0\'')

        then: "config was reverted"
        !conf.text.contains('http://some-url.com/1.0')
        !file('src/doc/mkdocs.yml.bak').exists()
    }

}
