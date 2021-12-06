package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.ajoberstar.grgit.Grgit
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.mkdocs.AbstractKitTest
import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 06.12.2021
 */
class VersionsFileTest extends AbstractKitTest {

    @TempDir
    File repoDir

    @TempDir
    File initDir

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
    }

    def "Check first version publishing"() {

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

        when: "build versions"
        result = run('mkdocsVersionsFile')

        then: "published"
        result.task(':mkdocsVersionsFile').outcome == TaskOutcome.SUCCESS

        then: "versions file correct"
        result.output.contains("Versions file generated with 1 versions:")
        result.output.contains("new versions: 1.0")
        file('/build/mkdocs/versions.json').exists()
        with(new JsonSlurper().parse(file('/build/mkdocs/versions.json')) as List) {
            it.size() == 1
            it[0]['version'] == '1.0'
            it[0]['title'] == '1.0'
            it[0]['aliases'] == []
        }
    }

    def "Check first version publishing disabled"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'
            
            python.scope = USER      
            
            mkdocs.publish.generateVersionsFile = false     
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "build versions"
        result = run('mkdocsVersionsFile')

        then: "published"
        result.task(':mkdocsVersionsFile').outcome == TaskOutcome.SUCCESS

        then: "versions not created"
        !file('/build/mkdocs/versions.json').exists()
    }

    def "Check first version with title publishing"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'
            
            python.scope = USER   
            mkdocs.publish.versionTitle = 'custom title'        
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "build versions"
        result = run('mkdocsVersionsFile')

        then: "published"
        result.task(':mkdocsVersionsFile').outcome == TaskOutcome.SUCCESS

        then: "versions file correct"
        result.output.contains("Versions file generated with 1 versions:")
        result.output.contains("new versions: 1.0")
        file('/build/mkdocs/versions.json').exists()
        with(new JsonSlurper().parse(file('/build/mkdocs/versions.json')) as List) {
            it.size() == 1
            it[0]['version'] == '1.0'
            it[0]['title'] == 'custom title'
            it[0]['aliases'] == []
        }
    }

    def "Check first version with aliases publishing"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'
            
            python.scope = USER   
            mkdocs.publish.versionAliases = ['one', 'two']      
        """

        when: "run init"
        BuildResult result = run('mkdocsInit')

        then: "docs created"
        result.task(':mkdocsInit').outcome == TaskOutcome.SUCCESS
        file('src/doc/mkdocs.yml').exists()

        when: "build versions"
        result = run('mkdocsVersionsFile')

        then: "published"
        result.task(':mkdocsVersionsFile').outcome == TaskOutcome.SUCCESS

        then: "versions file correct"
        result.output.contains("Versions file generated with 1 versions:")
        result.output.contains("new versions: 1.0")
        file('/build/mkdocs/versions.json').exists()
        with(new JsonSlurper().parse(file('/build/mkdocs/versions.json')) as List) {
            it.size() == 1
            it[0]['version'] == '1.0'
            it[0]['title'] == '1.0'
            it[0]['aliases'] == ['one', 'two']
        }
    }

    def "Check versions init from repository"() {

        setup:
        initVersions('0.9', '0.8')
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

        when: "build versions"
        result = run('mkdocsVersionsFile')
        println 'project repo copy: ' + file('/.gradle/gh-pages/').list()

        then: "published"
        result.task(':mkdocsVersionsFile').outcome == TaskOutcome.SUCCESS

        then: "versions file correct"
        result.output.contains("Versions file generated with 3 versions:")
        result.output.contains("new versions: 1.0, 0.9, 0.8")
        file('/build/mkdocs/versions.json').exists()
        with(new JsonSlurper().parse(file('/build/mkdocs/versions.json')) as List) {
            it.size() == 3
            it[0]['version'] == '1.0'
            it[0]['title'] == '1.0'
            it[0]['aliases'] == []

            it[1]['version'] == '0.9'
            it[1]['title'] == '0.9'
            it[1]['aliases'] == []

            it[2]['version'] == '0.8'
            it[2]['title'] == '0.8'
            it[2]['aliases'] == []
        }
    }

    def "Check version in file missed in repository"() {

        setup:
        initVersions('0.9')
        initVersionsFile('0.9', '0.8')
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

        when: "build versions"
        result = run('mkdocsVersionsFile')
        println 'project repo copy: ' + file('/.gradle/gh-pages/').list()

        then: "published"
        result.task(':mkdocsVersionsFile').outcome == TaskOutcome.SUCCESS

        then: "versions file correct"
        result.output.contains("Versions file generated with 2 versions:")
        result.output.contains("new versions: 1.0")
        result.output.contains("removed from file: 0.8")
        result.output.contains("remains the same: 0.9")
        file('/build/mkdocs/versions.json').exists()
        with(new JsonSlurper().parse(file('/build/mkdocs/versions.json')) as List) {
            it.size() == 2
            it[0]['version'] == '1.0'
            it[0]['title'] == '1.0'
            it[0]['aliases'] == []

            it[1]['version'] == '0.9'
            it[1]['title'] == '0.9'
            it[1]['aliases'] == []
        }
    }

    private void initVersions(String... paths) {
        Grgit prjRepo = Grgit.init(dir: initDir)
        prjRepo.remote.add(name: 'origin', url: repoDir.canonicalPath, pushUrl: repoDir.canonicalPath)
        prjRepo.checkout(branch: 'gh-pages', orphan: true)
        paths.each {
            File dir = new File(it, initDir)
            // marker file used for version directory detection
            File file = new File('404.html', dir)
            file.parentFile.mkdirs()
            file << "sample content"
            println "created: $file.absolutePath"
        }
        prjRepo.add(patterns: ['.'])
        assert prjRepo.status().staged.allChanges.size() > 0
        prjRepo.commit(message: 'initial versions')
        prjRepo.push()

        assert repo.branch.list().size() == 2
    }

    private void initVersionsFile(String... paths) {
        Grgit prjRepo = Grgit.init(dir: initDir)
        prjRepo.checkout(branch: 'gh-pages')
        List<Map<String, Object>> file = []
        paths.each {
            file.add([
                    'version': it,
                    'title': it,
                    'aliases': []
            ])
        }
        new File('versions.json', initDir).newWriter().withWriter { w ->
            w << JsonOutput.toJson(file)
        }
        prjRepo.add(patterns: ['versions.json'])
        assert prjRepo.status().staged.allChanges.size() > 0
        prjRepo.commit(message: 'add versions file')
        prjRepo.push()
    }
}