package ru.vyarus.gradle.plugin.mkdocs.util

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Project
import ru.vyarus.gradle.plugin.mkdocs.AbstractTest
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 07.12.2017
 */
class MkdocsConfigTest extends AbstractTest {

    def "Check mkdocs config file operations"() {

        setup:
        Project project = project()
        MkdocsConfig config = new MkdocsConfig(project, null)

        when: "config not exists"
        config.getConfig()
        then: "error"
        thrown(GradleException)

        when: "config exists"
        file('mkdocs.yml').createNewFile()
        File conf = config.getConfig()
        then: "ok"
        conf.exists()

        when: "creating backup"
        conf.text = 'sample'
        File back = config.backup()
        then: "ok"
        conf.exists()
        back.exists()
        back.text == 'sample'
        back.getParent() == conf.getParent()

        when: "restoring from backup"
        back.text = 'backup'
        config.restoreBackup(back)
        then: "ok"
        !back.exists()
        conf.exists()
        conf.text == 'backup'
    }

    def "Check special cases"() {

        setup:
        Project project = project()
        MkdocsConfig config = new MkdocsConfig(project, null)
        file('mkdocs.yml').createNewFile()

        when: "config exists"
        File conf = config.getConfig()
        then: "ok"
        conf.exists()

        when: "creating backup when backup already exists"
        def back = file('mkdocs.yml.bak')
        back.text = 'existing back'
        config.backup()
        then: "old file removed"
        conf.exists()
        back.exists()
        back.text != 'existing back'

        when: "can't restore from backup because it is not exist"
        back.delete()
        config.restoreBackup(back)
        then: "error"
        thrown(IllegalStateException)
    }

    @IgnoreIf({Os.isFamily(Os.FAMILY_UNIX)})
    def "Check impossible rename during backup restore"() {

        setup:
        Project project = project()
        MkdocsConfig config = new MkdocsConfig(project, null)
        file('mkdocs.yml').createNewFile()
        File back = config.backup()

        when: "can't restore from backup because backup can't be renamed"
        // keep config opened to prevent its deletion
        file('mkdocs.yml').withReader { reader ->
            config.restoreBackup(back)
        }
        then: "error"
        thrown(IllegalStateException)
    }

    def "Check config in dir"() {

        setup:
        Project project = project()
        MkdocsConfig config = new MkdocsConfig(project, 'dir/dr')

        when: "config not exists"
        config.getConfig()
        then: "error"
        thrown(GradleException)

        when: "config exists"
        file('dir/dr/').mkdirs()
        file('dir/dr/mkdocs.yml').createNewFile()
        File conf = config.getConfig()
        then: "ok"
        conf.exists()

        when: "creating backup"
        conf << 'sample'
        File back = config.backup()
        then: "ok"
        conf.exists()
        back.exists()
        back.getParent() == conf.getParent()
    }

    def "Check config search and modification ath the beginning"() {

        setup:
        Project project = project()
        File conf = file('mkdocs.yml')
        conf.createNewFile()
        MkdocsConfig config = new MkdocsConfig(project, null)

        when: "value not exists"
        String val = config.find('ex')
        then: "not found"
        !val

        when: "commented value"
        conf.delete()
        conf << """#ex: http://some-url.com
#line
"""
        val = config.find('ex')
        then: "not found"
        !val

        when: "value found"
        conf.delete()
        conf << """ex: http://some-url.com
#line
"""
        val = config.find('ex')
        then: "found"
        val == 'http://some-url.com'

        when: "empty value"
        conf.delete()
        conf << """ex:
#line
"""
        val = config.find('ex')
        then: "found, but empty"
        val == ''

        when: "override value"
        config.set('ex', 'http://other-url.com')
        val = config.find('ex')
        then: "found, but empty"
        val == 'http://other-url.com'
        conf.text == """ex: http://other-url.com
#line
"""
    }

    def "Check config search and modification at the middle"() {

        setup:
        Project project = project()
        File conf = file('mkdocs.yml')
        conf.createNewFile()
        MkdocsConfig config = new MkdocsConfig(project, null)

        when: "value not exists"
        String val = config.find('ex')
        then: "not found"
        !val

        when: "commented value"
        conf.delete()
        conf << """#multi
#ex: http://some-url.com
#line
"""
        val = config.find('ex')
        then: "not found"
        !val

        when: "value found"
        conf.delete()
        conf << """#multi
ex: http://some-url.com
#line
"""
        val = config.find('ex')
        then: "found"
        val == 'http://some-url.com'

        when: "empty value"
        conf.delete()
        conf << """#multi
ex:
#line
"""
        val = config.find('ex')
        then: "found, but empty"
        val == ''

        when: "override value"
        config.set('ex', 'http://other-url.com')
        val = config.find('ex')
        then: "found, but empty"
        val == 'http://other-url.com'
        conf.text == """#multi
ex: http://other-url.com
#line
"""
    }

    def "Check quotes support"() {

        setup:
        Project project = project()
        File conf = file('mkdocs.yml')
        conf.createNewFile()
        MkdocsConfig config = new MkdocsConfig(project, null)

        when: "quoted value"
        conf.delete()
        conf << 'ex: \'http://some-url.com\''
        def val = config.find('ex')
        then: "found"
        val == 'http://some-url.com'

        when: "quoted value 2"
        conf.delete()
        conf << 'ex: "http://some-url.com"'
        val = config.find('ex')
        then: "found"
        val == 'http://some-url.com'

        when: "write quoted"
        config.set('ex', '\'http://other-url.com\'')
        val = config.find('ex')
        then: "found, but empty"
        val == 'http://other-url.com'
        conf.text == 'ex: \'http://other-url.com\''
    }
}
