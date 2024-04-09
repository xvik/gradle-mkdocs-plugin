package ru.vyarus.gradle.plugin.mkdocs

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import ru.vyarus.gradle.plugin.python.PythonExtension

/**
 * @author Vyacheslav Rusakov
 * @since 28.10.2022
 */
class MkdocsBuildPluginTest extends AbstractTest {

    def "Check extension registration"() {

        when: "plugin applied"
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply "ru.vyarus.mkdocs-build"

        then: "extension registered"
        project.extensions.findByType(MkdocsExtension)

        then: "default modules registered"
        project.extensions.findByType(PythonExtension).modules == MkdocsExtension.DEFAULT_MODULES as List

        then: "mkdocs tasks registered"
        def task = { project.tasks.findByName(it) }
        task('mkdocsBuild')
        task('mkdocsServe')
        task('mkdocsInit')
        !task('mkdocsPublish')

        then: "publish extension not applied"
        !project.extensions.findByType(GitPublishExtension)
    }
}
