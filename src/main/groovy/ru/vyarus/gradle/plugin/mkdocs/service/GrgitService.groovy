package ru.vyarus.gradle.plugin.mkdocs.service

import groovy.transform.CompileStatic
import org.ajoberstar.grgit.Grgit
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

/**
 * Git service to manage single grgit instance between publish tasks.
 *
 * @author Vyacheslav Rusakov
 * @since 09.04.2024
 */
@CompileStatic
@SuppressWarnings(['AbstractClassWithoutAbstractMethod', 'ConfusingMethodName'])
abstract class GrgitService implements BuildService<BuildServiceParameters.None>,
        OperationCompletionListener, AutoCloseable {

    /**
     * Grgit instance. Initiated by gitReset task and used by other git tasks.
     */
    Grgit grgit

    @Override
    @SuppressWarnings('EmptyMethodInAbstractClass')
    void onFinish(FinishEvent finishEvent) {
        // not used, just to keep service alive
    }

    @Override
    void close() throws Exception {
        if (grgit != null) {
            grgit.close()
        }
    }
}
