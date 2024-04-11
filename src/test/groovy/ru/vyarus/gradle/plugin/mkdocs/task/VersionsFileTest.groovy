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
        prjRepo.close()
    }

    void cleanup() {
        repo.close()
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
        result.output.contains("Version aliases added: one, two")
        result.output.contains("new versions: 1.0")
        !result.output.contains("WARNING: Publishing version")
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

    def "Check versions sorting correctness"() {

        setup:
        initVersions('1.1', '1.2', '1.11', '1.21', '1.2-rc.1', '1.2-rc.2', '1.1-SNAPSHOT', '1.21.1')
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.22'
            
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
        file('/build/mkdocs/versions.json').exists()
        with(new JsonSlurper().parse(file('/build/mkdocs/versions.json')) as List) {
            // yes, snapshots and rc go before released version - it's OK (because version format is unknown)
            it.collect {it['version']} == ['1.22', '1.21.1', '1.21', '1.11', '1.2-rc.2', '1.2-rc.1', '1.2', '1.1-SNAPSHOT', '1.1']
        }
    }

    def "Check old version warnings"() {

        setup:
        initVersions('1.1', 'latest', '1.x')
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0'
            
            python.scope = USER   
            
            mkdocs.publish {
                rootRedirect = true
                versionAliases = ['latest', '1.x']
            }        
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
        result.output.contains('WARNING: Publishing version \'1.0\' is older then the latest published \'1.1\' and the following overrides might not be desired: ')
        result.output.contains('root redirect override to \'1.0\'')
        result.output.contains('existing alias \'latest\' override')
        result.output.contains('existing alias \'1.x\' override')
    }


    def "Check version hiding"() {

        setup:
        initVersions('1.1', '1.2', '1.11', '1.21', '1.2-rc.1', '1.2-rc.2', '1.1-SNAPSHOT', '1.21.1')
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.22'
            
            mkdocs.publish.hideVersions '1.2-rc.1', '1.2-rc.2'
            
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
        result.output.contains('hidden: 1.2-rc.2, 1.2-rc.1')

        then: "versions file correct"
        file('/build/mkdocs/versions.json').exists()
        with(new JsonSlurper().parse(file('/build/mkdocs/versions.json')) as List) {
            // yes, snapshots and rc go before released version - it's OK (because version format is unknown)
            it.collect {it['version']} == ['1.22', '1.21.1', '1.21', '1.11', '1.2', '1.1-SNAPSHOT', '1.1']
        }
    }


    def "Check auto bugfix versions hiding"() {

        setup:
        initVersions('1.1', '1.2', '1.11', '1.21', '1.2-rc.1', '1.2-rc.2', '1.1-SNAPSHOT', '1.21.1', '1.21.2', '1.22.1.1', '1.22.1.2')
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.23'
            
            mkdocs.publish.hideOldBugfixVersions = true
            
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
        result.output.contains('hidden: 1.22.1.1, 1.21.1, 1.2-rc.1')

        then: "versions file correct"
        file('/build/mkdocs/versions.json').exists()
        with(new JsonSlurper().parse(file('/build/mkdocs/versions.json')) as List) {
            // yes, snapshots and rc go before released version - it's OK (because version format is unknown)
            it.collect {it['version']} == ['1.23', '1.22.1.2', '1.21.2', '1.21', '1.11', '1.2-rc.2', '1.2', '1.1-SNAPSHOT', '1.1']
        }
    }

    private void initVersions(String... paths) {
        Grgit prjRepo = Grgit.init(dir: initDir)
        prjRepo.remote.add(name: 'origin', url: repoDir.canonicalPath, pushUrl: repoDir.canonicalPath)
        prjRepo.checkout(branch: 'gh-pages', orphan: true)
        paths.each {
            File dir = new File(initDir, it)
            // marker file used for version directory detection
            File file = new File(dir, '404.html')
            file.parentFile.mkdirs()
            file << "sample content"
        }
        prjRepo.add(patterns: ['.'])
        assert prjRepo.status().staged.allChanges.size() > 0
        prjRepo.commit(message: 'initial versions')
        prjRepo.push()
        prjRepo.close()

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
        new File(initDir, 'versions.json').newWriter().withWriter { w ->
            w << JsonOutput.toJson(file)
        }
        prjRepo.add(patterns: ['versions.json'])
        assert prjRepo.status().staged.allChanges.size() > 0
        prjRepo.commit(message: 'add versions file')
        prjRepo.push()
        prjRepo.close()
    }
}
