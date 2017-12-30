package ru.vyarus.gradle.plugin.mkdocs.util

import org.gradle.api.Project
import ru.vyarus.gradle.plugin.mkdocs.AbstractTest

import java.lang.reflect.Method

/**
 * @author Vyacheslav Rusakov
 * @since 05.12.2017
 */
class TemplateUtilsTest extends AbstractTest {

    def "Check direct access"() {

        when: "copy from classpath"
        Project project = project()
        TemplateUtils.copy(project, '/ru/vyarus/gradle/plugin/mkdocs/template/init/', 'tp', [:])

        then: "copied"
        file('tp/mkdocs.yml').exists()
        file('tp/docs/index.md').exists()

        when: "no trailing slash"
        TemplateUtils.copy(project, '/ru/vyarus/gradle/plugin/mkdocs/template/init', 'tp2', [:])

        then: "copied"
        file('tp2/mkdocs.yml').exists()
        file('tp2/docs/index.md').exists()

        when: "relative path"
        TemplateUtils.copy(project, 'relative/path', 'tp', [:])
        then: "error"
        thrown(IllegalArgumentException)

        when: "not existing path"
        TemplateUtils.copy(project, '/not/existing', 'tp', [:])
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
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", [URL] as Class[])
        method.setAccessible(true);
        method.invoke(ClassLoader.getSystemClassLoader(), [jar.toURI().toURL()] as Object[]);

        TemplateUtils.copy(project, '/template/init/', 'tp', [:])

        then: "copied"
        file('tp/mkdocs.yml').exists()
        file('tp/docs/index.md').exists()

        when: "no trailing slash"
        TemplateUtils.copy(project, '/template/init', 'tp2', [:])

        then: "copied"
        file('tp2/mkdocs.yml').exists()
        file('tp2/docs/index.md').exists()
    }
}