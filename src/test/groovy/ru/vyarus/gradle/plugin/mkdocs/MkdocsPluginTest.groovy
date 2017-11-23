package ru.vyarus.gradle.plugin.mkdocs

import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.testfixtures.ProjectBuilder

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

    }

    def "Check extension validation"() {

        when: "plugin configured"
        Project project = project {
            apply plugin: "ru.vyarus.mkdocs"

            mkdocs {
                foo '1'
                bar '2'
            }
        }

        then: "validation pass"
        def mkdocs = project.extensions.mkdocs;
        mkdocs.foo == '1'
        mkdocs.bar == '2'
    }


    def "Check extension validation failure"() {

        when: "plugin configured"
        Project project = project {
            apply plugin: "ru.vyarus.mkdocs"

            mkdocs {
                foo '1'
            }
        }

        then: "validation failed"
        def ex = thrown(ProjectConfigurationException)
        ex.cause.message == 'mkdocs.bar configuration required'
    }

}