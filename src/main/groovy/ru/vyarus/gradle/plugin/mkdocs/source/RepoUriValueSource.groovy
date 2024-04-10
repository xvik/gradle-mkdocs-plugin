package ru.vyarus.gradle.plugin.mkdocs.source

import groovy.transform.CompileStatic
import org.ajoberstar.grgit.Configurable
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.OpenOp
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * Git repository detection in project root. Custom value source is required for configuration cache support because
 * all external processes must be wrapped (even if this values could be easily cached).
 *
 * @author Vyacheslav Rusakov
 * @since 10.04.2024
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class RepoUriValueSource implements ValueSource<String, Params> {

    @SuppressWarnings(['UnnecessaryCast', 'CatchException'])
    String obtain() {
        try {
            Grgit repo = Grgit.open({ OpenOp op -> op.dir = parameters.rootDir.get() } as Configurable<OpenOp>)
            return repo.remote.list().find { it.name == 'origin' }?.url
        } catch (Exception ignored) {
            // repository not initialized case - do nothing (most likely user is just playing with the plugin)
        }
        return null
    }

    interface Params extends ValueSourceParameters {
        Property<File> getRootDir()
    }
}
