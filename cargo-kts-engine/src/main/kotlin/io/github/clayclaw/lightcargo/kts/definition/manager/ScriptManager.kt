package io.github.clayclaw.lightcargo.kts.definition.manager

import io.github.clayclaw.lightcargo.kts.definition.ScriptState
import kotlin.reflect.KClass

interface ScriptManager {
    fun <T: ScriptState> getByScriptState(state: KClass<out T>): List<T>
}

inline fun <reified T: ScriptState> ScriptManager.getByScriptState(): List<T> = getByScriptState(T::class)