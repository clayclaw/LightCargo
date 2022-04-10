package io.github.clayclaw.lightcargo.kts.definition

import java.io.File
import kotlin.reflect.KClass
import kotlin.script.experimental.api.CompiledScript

sealed class ScriptState {
    abstract val scriptFile: File

    class Discovered(override val scriptFile: File) : ScriptState()

    class Compiled(
        override val scriptFile: File,
        val compiledScript: CompiledScript,
        var lastModified: Long
    ) : ScriptState()

    class Evaluated(
        override val scriptFile: File,
        val scriptClass: KClass<*>?,
        val scriptInstance: Any? // nullable since some platform may not have any actual instance (e.g. JS)
    ): ScriptState()
}