package ru.vyarus.gradle.plugin.mkdocs.task

import org.ajoberstar.grgit.Grgit
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ru.vyarus.gradle.plugin.mkdocs.AbstractKitTest
import ru.vyarus.gradle.plugin.mkdocs.MkdocsExtension

/**
 * @author Vyacheslav Rusakov
 * @since 01.12.2017
 */
class PublishJavadocKitTest extends AbstractKitTest {

    @Rule
    TemporaryFolder repoDir = new TemporaryFolder()

    Grgit repo

    @Override
    def setup() {
        // local repo used for push
        println 'init fake gh-pages repo'
        repo = Grgit.init(dir: repoDir.root, bare: true)
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
    }

    def "Check additional resources publication"() {

        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'

            python.scope = USER
            
            gitPublish.contents {
                from(javadoc) {
                    into "\${mkdocs.resolveDocPath()}/javadoc"
                }
            }
            
            // dependency will not be set automatically 
            gitPublishReset.dependsOn javadoc
   
        """
        file('src/main/java/').mkdirs()
        file('src/main/java/Sample.java') << """
/**
 * Sample class.
 */
public class Sample {}
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

        then: "published with javadoc"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS

        then: "content available"
        file('/.gradle/gh-pages/1.0/index.html').exists()
        file('/.gradle/gh-pages/1.0/javadoc/index.html').exists()
    }

}
