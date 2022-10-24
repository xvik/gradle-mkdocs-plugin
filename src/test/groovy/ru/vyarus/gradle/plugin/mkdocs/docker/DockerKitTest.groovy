package ru.vyarus.gradle.plugin.mkdocs.docker

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.mkdocs.AbstractKitTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 13.10.2022
 */
// testcontainers doesn't work on windows server https://github.com/testcontainers/testcontainers-java/issues/2960
@IgnoreIf({ System.getProperty("os.name").toLowerCase().contains("windows") })
class DockerKitTest extends AbstractKitTest {

    def "Check workflow"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0-SNAPSHOT'
            python.docker.use = true
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
        file('build/mkdocs/1.0-SNAPSHOT/index.html').exists()

    }

    def "Check build"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            version = '1.0'  
            
            python.docker.use = true
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS

        when: "build site"
        File conf = file('src/doc/mkdocs.yml')
        conf.text = conf.text.replaceAll(/(?m)^site_url:.*/, "site_url: http://localhost")
        result = run('mkdocsBuild')

        then: "built"
        result.task(':mkdocsBuild').outcome == TaskOutcome.SUCCESS
        result.output =~ /python -m mkdocs build -c -d [^(-)]+ -s/

        file('build/mkdocs/1.0/index.html').exists()
        // site_url wasn't modified
        file('build/mkdocs/1.0/sitemap.xml').text.contains('<loc>http://localhost/1.0/guide/installation/</loc>')
        file('build/mkdocs/index.html').exists()
        file('build/mkdocs/index.html').text.contains('URL=\'1.0\'')

        when: "up to date check"
        result = run('mkdocsBuild')

        then: "ok"
        result.task(':mkdocsBuild').outcome == TaskOutcome.UP_TO_DATE

    }

    def "Check serve"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                                              
            }
            
            python.docker.use = true
        """
        file('src/doc/').mkdirs()

        when: "serve site"
        BuildResult result = runFailed('mkdocsServe')

        then: "command correct"
        result.task(':mkdocsServe').outcome == TaskOutcome.FAILED
        result.output =~ /python -m mkdocs serve --dev-addr 0.0.0.0:3000/
    }

}
