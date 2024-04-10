package ru.vyarus.gradle.plugin.mkdocs

import groovy.json.JsonSlurper
import org.ajoberstar.grgit.Grgit
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 10.04.2024
 */
class ConfigurationCacheSupportPushKitTest extends AbstractKitTest {

    @TempDir File repoDir

    Grgit repo

    @Override
    def setup() {
        // local repo used for push
        println 'init fake gh-pages repo'
        repo = Grgit.init(dir: repoDir, bare: true)
        assert repo.branch.list().size() == 0
        // connect with project folder
        println 'associate project with fake repo'
        Grgit prjRepo = Grgit.init(dir: testProjectDir)
        prjRepo.remote.add(name: 'origin', url: repoDir.canonicalPath, pushUrl: repoDir.canonicalPath)
        // init remote repo
        file('readme.txt') << 'sample'
        prjRepo.add(patterns: ['*'])
        prjRepo.commit(message: 'initial commit')
        prjRepo.push()
        prjRepo.close()

        assert repo.log().size() == 1
        assert repo.branch.list().size() == 1
    }

    void cleanup() {
        repo.close()
    }

    def "Check default publish"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'
            
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

        when: "publish"
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'mkdocsPublish')

        then: "no configuration cache incompatibilities"
        result.output.contains("1 problem was found storing the configuration cache")
        result.output.contains('Gradle runtime: support for using a Java agent with TestKit')
        result.output.contains('Calculating task graph as no cached configuration is available for tasks:')

        then: "published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        repo.branch.list().size() == 2

        then: "content available"
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/index.html').exists()

        then: "versions file correct"
        result.output.contains("Versions file generated with 1 versions:")
        file('/.gradle/gh-pages/versions.json').exists()
        with(new JsonSlurper().parse(file('/.gradle/gh-pages/versions.json')) as List) {
            it.size() == 1
            it[0]['title'] == '1.0'
        }


        when: "publish another version"
        buildFile.delete()
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.1'
            
            python.scope = USER
        """
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'mkdocsPublish')

        then: "cache used"
        result.output.contains('Calculating task graph as configuration cache cannot be reused because file \'build.gradle\' has changed.')

        then: "version published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        file('/.gradle/gh-pages/1.1/index.html').exists()
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/index.html').exists()
        file('/.gradle/gh-pages/index.html').text.contains('URL=\'1.1\'')

        then: "versions file correct"
        result.output.contains("Versions file generated with 2 versions:")
        file('/.gradle/gh-pages/versions.json').exists()
        with(new JsonSlurper().parse(file('/.gradle/gh-pages/versions.json')) as List) {
            it.size() == 2
            it[0]['title'] == '1.1'
            it[1]['title'] == '1.0'
        }



        when: "run from cache"
        println '\n\n------------------- FROM CACHE ----------------------------------------'
        result = run('--configuration-cache', '--configuration-cache-problems=warn', 'mkdocsPublish')

        then: "cache used"
        result.output.contains('Reusing configuration cache.')
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        file('/.gradle/gh-pages/1.1/index.html').exists()
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/index.html').exists()
        file('/.gradle/gh-pages/index.html').text.contains('URL=\'1.1\'')
    }
}
