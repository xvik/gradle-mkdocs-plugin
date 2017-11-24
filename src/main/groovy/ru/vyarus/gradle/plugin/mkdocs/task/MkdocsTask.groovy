package ru.vyarus.gradle.plugin.mkdocs.task

import groovy.transform.CompileStatic
import ru.vyarus.gradle.plugin.mkdocs.MkdocsExtension
import ru.vyarus.gradle.plugin.python.task.PythonTask

/**
 * General mkdocs task.
 *
 * @author Vyacheslav Rusakov
 * @since 13.11.2017
 */
@CompileStatic
class MkdocsTask extends PythonTask {

    @Override
    @SuppressWarnings('GetterMethodCouldBeProperty')
    String getModule() {
        // restrict commands to mkdocs module
        return 'mkdocs'
    }

    /**
     * For use in specialized tasks.
     *
     * @return mkdocs extensions object
     */
    protected MkdocsExtension getExtension() {
        project.extensions.findByType(MkdocsExtension)
    }
}
