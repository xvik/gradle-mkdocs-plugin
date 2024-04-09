package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.json.JsonSlurper
import org.ajoberstar.grgit.Grgit
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.mkdocs.AbstractKitTest
import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 28.11.2017
 */
class PushTaskKitTest extends AbstractKitTest {

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
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "publish"
        result = run('mkdocsPublish')

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
        result = run('mkdocsPublish')

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
    }

    def "Check no multi-version"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'
            
            python.scope = USER
            
            mkdocs.publish.docPath = null
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "publish"
//        debug()
//        println 'wait for debugger'
        result = run('mkdocsPublish')

        then: "published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        repo.branch.list().size() == 2

        then: "content available"
        file('/.gradle/gh-pages/index.html').exists()
        !file('/.gradle/gh-pages/versions.json').exists()
        !file('/.gradle/gh-pages/1.0/index.html').exists()
        // redirect file not generated
        !file('/.gradle/gh-pages/index.html').text.contains('meta http-equiv="refresh"')


        when: "publish another version"
        buildFile.delete()
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.1'
            
            python.scope = USER
            
            mkdocs.publish.docPath = null
        """
        result = run('mkdocsPublish')

        then: "no need publishing"
        result.task(':mkdocsPublish').outcome == TaskOutcome.UP_TO_DATE

        when: "publish with changes"
        file('src/doc/docs/index.md') << 'dummy file to indicate changes'
        result = run('mkdocsPublish')

        then: "version published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        !file('/.gradle/gh-pages/1.1/index.html').exists()
        !file('/.gradle/gh-pages/1.0/index.html').exists()
        !file('/.gradle/gh-pages/versions.json').exists()
        file('/.gradle/gh-pages/index.html').exists()
        !file('/.gradle/gh-pages/index.html').text.contains('meta http-equiv="refresh"')
    }

    def "Check complex path"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'
            
            python.scope = USER
            
            mkdocs.publish {
                docPath = 'en/1.0'
                versionTitle = '1.0'
            }
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "publish"
//        debug()
//        println 'wait for debugger'
        result = run('mkdocsPublish')

        then: "published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        repo.branch.list().size() == 2

        then: "content available"
        file('/.gradle/gh-pages/index.html').exists()
        file('/.gradle/gh-pages/en/1.0/index.html').exists()
        // redirect file generated
        file('/.gradle/gh-pages/index.html').text.contains('URL=\'en/1.0\'')

        then: "versions file correct"
        result.output.contains("Versions file generated with 1 versions:")
        file('/.gradle/gh-pages/versions.json').exists()
        with(new JsonSlurper().parse(file('/.gradle/gh-pages/versions.json')) as List) {
            it.size() == 1
            it[0]['version'] == 'en/1.0'
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
            
            mkdocs.publish {
                docPath = 'en/1.1'
                versionTitle = '1.1'
            }
        """
        result = run('mkdocsPublish')

        then: "version published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        file('/.gradle/gh-pages/en/1.1/index.html').exists()
        file('/.gradle/gh-pages/en/1.0/index.html').exists()
        file('/.gradle/gh-pages/index.html').exists()
        file('/.gradle/gh-pages/index.html').text.contains('URL=\'en/1.1\'')

        then: "versions file correct"
        result.output.contains("Versions file generated with 2 versions:")
        file('/.gradle/gh-pages/versions.json').exists()
        with(new JsonSlurper().parse(file('/.gradle/gh-pages/versions.json')) as List) {
            it.size() == 2
            it[0]['version'] == 'en/1.1'
            it[0]['title'] == '1.1'
            it[1]['version'] == 'en/1.0'
            it[1]['title'] == '1.0'
        }
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

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "publish"
        result = run('mkdocsPublish')

        then: "published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        repo.branch.list().size() == 2

        then: "content available"
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/index.html').exists()

        when: "publish again"
        result = run('mkdocsPublish')

        then: "version published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.UP_TO_DATE
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/index.html').exists()
    }

    def "Check auth props bind"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
                
            ext['org.ajoberstar.grgit.auth.username'] = 'user'
            ext['org.ajoberstar.grgit.auth.password'] = 'pass'                        
            
            mkdocsPublish.doLast {
                println 'check system properties set'
                assert System.getProperty('org.ajoberstar.grgit.auth.username') == 'user'
                assert System.getProperty('org.ajoberstar.grgit.auth.password') == 'pass'
            }
            
            version = '1.0'
            
            python.scope = USER
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "publish"
        result = run('mkdocsPublish')

        then: "published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        repo.branch.list().size() == 2
    }

}
