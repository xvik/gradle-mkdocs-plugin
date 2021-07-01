package ru.vyarus.gradle.plugin.mkdocs

import org.ajoberstar.grgit.Grgit
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 02.03.2021
 */
class UpstreamPublishKitTest extends AbstractKitTest {

    String GRADLE_VERSION = '7.1'

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

        assert repo.log().size() == 1
        assert repo.branch.list().size() == 1
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
        BuildResult result = runVer(GRADLE_VERSION, 'mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "publish"
//        debug()
//        println 'wait for debugger'
        result = runVer(GRADLE_VERSION, 'mkdocsPublish')

        then: "published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        repo.branch.list().size() == 2

        then: "content available"
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/index.html').exists()


        when: "publish another version"
        buildFile.delete()
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.1'
            
            python.scope = USER
        """
        result = runVer(GRADLE_VERSION, 'mkdocsPublish')

        then: "version published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        file('/.gradle/gh-pages/1.1/index.html').exists()
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/index.html').exists()
        file('/.gradle/gh-pages/index.html').text.contains('URL=\'1.1\'')
    }
}
