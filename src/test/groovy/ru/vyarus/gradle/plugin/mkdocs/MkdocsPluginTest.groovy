package ru.vyarus.gradle.plugin.mkdocs

import org.ajoberstar.gradle.git.publish.GitPublishPlugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.Task
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
        def task = {project.tasks.findByName(it)}
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

}