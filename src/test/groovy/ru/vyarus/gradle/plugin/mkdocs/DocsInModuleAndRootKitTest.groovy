package ru.vyarus.gradle.plugin.mkdocs

import org.ajoberstar.grgit.Grgit
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 04.08.2022
 */
class DocsInModuleAndRootKitTest extends AbstractKitTest {

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

    def "Check docs in root and submodule"() {

        // both published into the same git, but with different naming scheme

        setup:
        file('settings.gradle') << ' include "doc"'
        file('doc').mkdir()
        build """
            plugins {
                id 'ru.vyarus.mkdocs'                                
            }
            
            version = '1.0-SNAPSHOT'
            
            mkdocs {
                sourcesDir = 'docs'
            }
            
            project(':doc') {
                apply plugin: 'ru.vyarus.mkdocs' 
                
                mkdocs {
                    sourcesDir = 'src'
                    
                    publish {
                        docPath = 'sub-\$version'
                        rootRedirect = false
                    }
                }
            }
                        
        """

        // root docs
        file('docs').mkdir()
        file('docs/mkdocs.yml') << """
site_name: root

nav:
  - Home: index.md    
"""
        file('docs/docs').mkdir()
        file('docs/docs/index.md') << """
root index page
"""

        // module docs
        file('doc').mkdir()
        file('doc/src').mkdir()
        file('doc/src/mkdocs.yml') << """
site_name: sub

nav:
  - Home: index.md    
"""
        
        file('doc/src/docs').mkdir()
        file('doc/src/docs/index.md') << """
submodule index page
"""

        when: "build site"
        BuildResult result = run('mkdocsPublish')

        then: "built"
        result.task(':mkdocsPublish').outcome == TaskOutcome.SUCCESS
        result.task(':doc:mkdocsPublish').outcome == TaskOutcome.SUCCESS
        file('build/mkdocs/1.0-SNAPSHOT/index.html').text.contains('root index page')
        file('doc/build/mkdocs/sub-1.0-SNAPSHOT/index.html').text.contains('submodule index page')
    }
}
