package ru.vyarus.gradle.plugin.mkdocs.task

import org.ajoberstar.grgit.Grgit
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ru.vyarus.gradle.plugin.mkdocs.AbstractKitTest

/**
 * @author Vyacheslav Rusakov
 * @since 28.11.2017
 */
class PushKitTest extends AbstractKitTest {

    @Rule
    TemporaryFolder repoDir = new TemporaryFolder()

    def "Check workflow"() {

        setup:
        // local repo used for push
        println 'init fake gh-pages repo'
        Grgit repo = Grgit.init(dir: repoDir.root, bare: true)
        assert repo.branch.list().size() == 0
        // connect with project folder
        println 'associate project with fake repo'
        Grgit prjRepo = Grgit.init(dir: testProjectDir.root)
        prjRepo.remote.add(name: 'origin', url: repoDir.root.canonicalPath, pushUrl: repoDir.root.canonicalPath)
        // init remote repo
        file('readme.txt') << 'sample'
        prjRepo.add(patterns: ['*'])
        prjRepo.commit(message: 'initial commit')
        prjRepo.push()

        assert repo.log().size() == 1
        assert repo.branch.list().size() == 1

        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'
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
        file('/.gradle/gh-pages/index.html').exists()


        when: "publish another version"
        buildFile.delete()
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.1'
        """
        result = run('mkdocsPublish')

        then: "version published"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        file('/.gradle/gh-pages/1.1/index.html').exists()
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/index.html').exists()
        file('/.gradle/gh-pages/index.html').text.contains('URL=\'1.1\'')
    }
}
