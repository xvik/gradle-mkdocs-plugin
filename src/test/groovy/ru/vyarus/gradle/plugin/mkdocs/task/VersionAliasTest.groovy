package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.json.JsonSlurper
import org.ajoberstar.grgit.Grgit
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.mkdocs.AbstractKitTest
import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 05.12.2021
 */
class VersionAliasTest extends AbstractKitTest {

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

    def "Check alias publishing"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'
            
            python.scope = USER
            
            mkdocs.publish.versionAliases = ['latest']
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
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/latest/index.html').exists()
        file('/.gradle/gh-pages/latest/index.html').size() == file('/.gradle/gh-pages/1.0/index.html').size()
        file('/.gradle/gh-pages/index.html').exists()

        then: "versions file correct"
        result.output.contains("Versions file generated with 1 versions:")
        file('/.gradle/gh-pages/versions.json').exists()
        with(new JsonSlurper().parse(file('/.gradle/gh-pages/versions.json')) as List) {
            it.size() == 1
            it[0]['title'] == '1.0'
            it[0]['aliases'] == ['latest']
        }


        when: "publish another version"
        buildFile.delete()
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.1'
            
            python.scope = USER
            
            mkdocs.publish.versionAliases = ['latest']
        """
        // change to validate aliased version correctness
        file('src/doc/docs/index.md') << 'different part'
        result = run('mkdocsPublish')

        then: "version published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        file('/.gradle/gh-pages/1.1/index.html').exists()
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/latest/index.html').exists()
        file('/.gradle/gh-pages/latest/index.html').size() == file('/.gradle/gh-pages/1.1/index.html').size()
        file('/.gradle/gh-pages/index.html').exists()
        file('/.gradle/gh-pages/index.html').text.contains('URL=\'1.1\'')

        then: "versions file correct"
        result.output.contains("Versions file generated with 2 versions:")
        file('/.gradle/gh-pages/versions.json').exists()
        with(new JsonSlurper().parse(file('/.gradle/gh-pages/versions.json')) as List) {
            it.size() == 2
            it[0]['title'] == '1.1'
            it[0]['aliases'] == ['latest']
            it[1]['title'] == '1.0'
            it[1]['aliases'] == []
        }
    }

    def "Check root redirection to alias"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'
            
            python.scope = USER
            
            mkdocs.publish {
              versionAliases = ['latest']
              rootRedirect = true
              rootRedirectTo = 'latest'
            }
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
        file('/.gradle/gh-pages/latest/index.html').exists()
        file('/.gradle/gh-pages/index.html').exists()

        and: "redirection correct"
        file('/.gradle/gh-pages/index.html').text.contains('URL=\'latest\'')
    }

    def "Check invalid root redirection to alias"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'
            
            python.scope = USER
            
            mkdocs.publish {
              versionAliases = ['latest']
              rootRedirect = true
              rootRedirectTo = 'dummy'
            }
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "publish"
        result = runFailed('mkdocsPublish')

        then: "failed"
        result.output.contains("Invalid mkdocs.publish.rootRedirectTo option value: 'dummy'. Possible values are: 1.0, latest ('\$docPath' for actual version)")
    }

    def "Check version update publishing"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'
            
            python.scope = USER
            
            mkdocs.publish.versionAliases = ['latest']
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
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/latest/index.html').exists()
        file('/.gradle/gh-pages/latest/index.html').size() == file('/.gradle/gh-pages/1.0/index.html').size()
        file('/.gradle/gh-pages/index.html').exists()

        then: "versions file correct"
        result.output.contains("Versions file generated with 1 versions:")
        file('/.gradle/gh-pages/versions.json').exists()
        with(new JsonSlurper().parse(file('/.gradle/gh-pages/versions.json')) as List) {
            it.size() == 1
            it[0]['title'] == '1.0'
            it[0]['aliases'] == ['latest']
        }


        when: "publish again version"
        // change to validate aliased version correctness
        file('src/doc/docs/index.md') << 'different part'
        result = run('mkdocsPublish')

        then: "version published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/latest/index.html').exists()
        file('/.gradle/gh-pages/latest/index.html').size() == file('/.gradle/gh-pages/1.0/index.html').size()
        file('/.gradle/gh-pages/index.html').exists()

        then: "versions file correct"
        result.output.contains("Versions file generated with 1 versions:")
        file('/.gradle/gh-pages/versions.json').exists()
        with(new JsonSlurper().parse(file('/.gradle/gh-pages/versions.json')) as List) {
            it.size() == 1
            it[0]['title'] == '1.0'
            it[0]['aliases'] == ['latest']
        }
    }

    def "Check alias published when versions file publishing disabled"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'
            
            python.scope = USER
            
            mkdocs.publish {
                versionAliases = ['latest']
                generateVersionsFile = false
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
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/latest/index.html').exists()
        file('/.gradle/gh-pages/latest/index.html').size() == file('/.gradle/gh-pages/1.0/index.html').size()
        file('/.gradle/gh-pages/index.html').exists()
        !file('/.gradle/gh-pages/versions.json').exists()
    }
}
