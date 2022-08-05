package ru.vyarus.gradle.plugin.mkdocs

import org.ajoberstar.gradle.git.publish.GitPublishPlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import ru.vyarus.gradle.plugin.python.PythonExtension

/**
 * @author Vyacheslav Rusakov
 * @since 29.10.2017
 */
class MkdocsPluginTest extends AbstractTest {

    def "Check extension registration"() {

        when: "plugin applied"
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply "ru.vyarus.mkdocs"

        then: "extension registered"
        project.extensions.findByType(MkdocsExtension)

        then: "default modules registered"
        project.extensions.findByType(PythonExtension).modules == MkdocsExtension.DEFAULT_MODULES as List

        then: "mkdocs tasks registered"
        def task = { project.tasks.findByName(it) }
        task('mkdocsBuild')
        task('mkdocsServe')
        task('mkdocsInit')
        task('mkdocsPublish')

        then: "publish plugin applied"
        project.plugins.findPlugin(GitPublishPlugin)

        then: "task graph valid"
        task('mkdocsPublish').dependsOn.contains('gitPublishPush')
        task('gitPublishReset').dependsOn.contains('mkdocsBuild')
    }

    def "Check default resolution"() {

        when: "plugin applied"
        Project project = ProjectBuilder.builder().build()
        project.version = '1.0'
        project.plugins.apply "ru.vyarus.mkdocs"
        MkdocsExtension ext = project.extensions.findByType(MkdocsExtension)

        then: 'resolution correct'
        ext.resolveDocPath() == '1.0'
        ext.resolveComment() == 'Publish 1.0 documentation'

    }

    def "Check extra slashes resolution"() {

        when: "plugin applied"
        Project project = ProjectBuilder.builder().build()
        project.version = '1.0'
        project.plugins.apply "ru.vyarus.mkdocs"
        MkdocsExtension ext = project.extensions.findByType(MkdocsExtension)
        ext.publish.docPath = '/1.0/'

        then: 'resolution correct'
        ext.resolveDocPath() == '1.0'
        ext.resolveComment() == 'Publish 1.0 documentation'

    }

    def "Check complex path resolution"() {

        when: "plugin applied"
        Project project = ProjectBuilder.builder().build()
        project.version = '1.0'
        project.plugins.apply "ru.vyarus.mkdocs"
        MkdocsExtension ext = project.extensions.findByType(MkdocsExtension)
        ext.publish.docPath = '/en/$version/'

        then: 'resolution correct'
        ext.resolveDocPath() == 'en/1.0'
        ext.resolveComment() == 'Publish en/1.0 documentation'

    }

    def "Check no multi-version"() {

        when: "plugin applied"
        Project project = ProjectBuilder.builder().build()
        project.version = '1.0'
        project.plugins.apply "ru.vyarus.mkdocs"
        MkdocsExtension ext = project.extensions.findByType(MkdocsExtension)
        ext.publish.docPath = null

        then: 'resolution correct'
        ext.resolveDocPath() == null
        ext.resolveComment() == 'Publish  documentation'

    }

    def "Check no multi-version 2"() {

        when: "plugin applied"
        Project project = ProjectBuilder.builder().build()
        project.version = '1.0'
        project.plugins.apply "ru.vyarus.mkdocs"
        MkdocsExtension ext = project.extensions.findByType(MkdocsExtension)
        ext.publish.docPath = ''

        then: 'resolution correct'
        ext.resolveDocPath() == null
        ext.resolveComment() == 'Publish  documentation'

    }

    def "Check extra variables"() {

        when: "plugin applied"
        Project project = project {
            apply plugin: 'ru.vyarus.mkdocs'

            version = 1.0
            mkdocs {
                extras = [
                        'foo':  project.version,
                        'bar': "${-> project.version }",
                ]
            }

            version = 1.1
        }
        MkdocsExtension ext = project.extensions.findByType(MkdocsExtension)

        then: 'resolution correct'
        ext.extras.foo == 1.0
        ext.extras.bar == 1.1

    }

    def "Check version validation"() {

        when: "plugin applied"
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply "ru.vyarus.mkdocs"
        MkdocsExtension ext = project.extensions.findByType(MkdocsExtension)
        
        then: 'failed'
        ext.resolveDocPath() == 'unspecified'

    }
}