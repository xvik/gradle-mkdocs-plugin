package ru.vyarus.gradle.plugin.mkdocs.util

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import ru.vyarus.gradle.plugin.mkdocs.AbstractTest

/**
 * @author Vyacheslav Rusakov
 * @since 05.12.2017
 */
class TemplateUtilsTest extends AbstractTest {

    def "Check direct access"() {

        when: "copy from classpath"
        Project project = project()
        TemplateUtils.copy((project as ProjectInternal).fileOperations, '/ru/vyarus/gradle/plugin/mkdocs/template/init/', 'tp', [:])

        then: "copied"
        file('tp/mkdocs.yml').exists()
        file('tp/docs/index.md').exists()

        when: "no trailing slash"
        TemplateUtils.copy((project as ProjectInternal).fileOperations, '/ru/vyarus/gradle/plugin/mkdocs/template/init', 'tp2', [:])

        then: "copied"
        file('tp2/mkdocs.yml').exists()
        file('tp2/docs/index.md').exists()

        when: "relative path"
        TemplateUtils.copy((project as ProjectInternal).fileOperations, 'relative/path', 'tp', [:])
        then: "error"
        thrown(IllegalArgumentException)

        when: "not existing path"
        TemplateUtils.copy((project as ProjectInternal).fileOperations, '/not/existing', 'tp', [:])
        then: "error"
        thrown(IllegalArgumentException)
    }

    def "Check zip access"() {

        when: "build jar"
        File templates = new File('src/main/resources/ru/vyarus/gradle/plugin/mkdocs/')
        Project project = project()
        project.ant.zip(destfile: 'test.jar') {
            fileset(dir: templates.absolutePath) {
                include(name: '**/*.md')
                include(name: '**/*.yml')
                include(name: '**/*.html')
            }
        }

        then: 'built'
        File jar = file('test.jar')
        jar.exists()

        when: "copy template from jar"
        // add to current classpath
        URLClassLoader extendedLoader = new URLClassLoader([jar.toURI().toURL()] as  URL[], TemplateUtils.getClassLoader())
        Thread.currentThread().setContextClassLoader(extendedLoader)

        TemplateUtils.copy((project as ProjectInternal).fileOperations, '/template/init/', 'tp', [:])

        then: "copied"
        file('tp/mkdocs.yml').exists()
        file('tp/docs/index.md').exists()

        when: "no trailing slash"
        TemplateUtils.copy((project as ProjectInternal).fileOperations, '/template/init', 'tp2', [:])

        then: "copied"
        file('tp2/mkdocs.yml').exists()
        file('tp2/docs/index.md').exists()
    }
}