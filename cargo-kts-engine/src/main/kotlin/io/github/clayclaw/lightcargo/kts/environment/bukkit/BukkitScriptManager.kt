package io.github.clayclaw.lightcargo.kts.environment.bukkit

import io.github.clayclaw.lightcargo.kts.definition.ScriptState
import io.github.clayclaw.lightcargo.kts.definition.discoverAllScriptRecursively
import io.github.clayclaw.lightcargo.kts.definition.manager.ScriptManager
import org.koin.core.annotation.Single
import java.io.File
import kotlin.reflect.KClass
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvmhost.JvmScriptCompiler

@Single
class BukkitScriptManager : ScriptManager {

    private val compiler = JvmScriptCompiler(BukkitScriptHostConfig)
    private val evaluator = BasicJvmScriptEvaluator()

    private val scriptState: HashMap<KClass<out ScriptState>, List<ScriptState>> = hashMapOf()

    private fun discoverAllScript(): Sequence<ScriptState.Discovered> {
        bukkitScriptBaseDir.mkdirs()
        return bukkitScriptBaseDir.discoverAllScriptRecursively(BUKKIT_SCRIPT_DEFINITION_NAME)
            .map { ScriptState.Discovered(it) }
    }

    private suspend fun compileScript(scriptFile: File): ScriptState.Compiled {
        val compiledScript = compiler(scriptFile.toScriptSource(), BukkitScriptCompilationConfig).valueOrThrow()
        return ScriptState.Compiled(scriptFile, compiledScript, scriptFile.lastModified())
    }

    private suspend fun compileScriptCatching(scriptFile: File): ScriptState.Compiled? {
        runCatching {
            compileScript(scriptFile)
        }.onSuccess {
            return it
        }.onFailure {
            it.printStackTrace()
            BootstrapPlugin.instance.logger.warning("Error while compiling script: ${scriptFile.name}")
        }
        return null
    }

    private suspend fun evaluateScript(compiledScript: ScriptState.Compiled): ScriptState.Evaluated {
        evaluator(compiledScript.compiledScript, BukkitScriptEvaluationConfig).valueOrThrow().let { result ->
            when (result.returnValue) {
                is ResultValue.NotEvaluated -> throw IllegalStateException("Script is not evaluated")
                is ResultValue.Error -> throw (result.returnValue as ResultValue.Error).error
                is ResultValue.Value -> (result.returnValue as ResultValue.Value).let { it.scriptClass to it.scriptInstance }
                is ResultValue.Unit -> (result.returnValue as ResultValue.Unit).let { it.scriptClass to it.scriptInstance }
            }.let {
                println("Methods from script class ${it.first?.java?.canonicalName}: ${it.first?.java?.declaredMethods?.map { f -> f.name }}")
                return ScriptState.Evaluated(compiledScript.scriptFile, it.first, it.second)
            }
        }
    }

    internal fun discoverScripts() {
        scriptState[ScriptState.Discovered::class] = discoverAllScript().toList()
    }


    internal suspend fun compileScripts() {
        scriptState[ScriptState.Compiled::class] = scriptState[ScriptState.Discovered::class]?.mapNotNull {
            compileScriptCatching(it.scriptFile)
        }?.toList() ?: emptyList()
    }

    /**
     * @return Re-compiled scripts
     */
    internal suspend fun recompileScripts(): List<ScriptState.Compiled> {
        val groups = scriptState[ScriptState.Compiled::class]
            ?.groupBy { it.scriptFile.lastModified() == (it as ScriptState.Compiled).lastModified }
            ?: return emptyList()
        val recompiled = groups[true]?.mapNotNull { compileScriptCatching(it.scriptFile) } ?: emptyList()
        scriptState[ScriptState.Compiled::class] = recompiled.plus(groups[false] ?: emptyList())
        return recompiled
    }

    internal suspend fun evaluateScripts() {
        scriptState[ScriptState.Evaluated::class]
            ?.forEach {
                (it as? ScriptState.Evaluated)?.let { result ->
                    runCatching {
                        result.scriptClass?.java?.getDeclaredMethod("onDispose")?.invoke(result.scriptInstance)
                    }.onFailure {
                        BootstrapPlugin.instance.logger.warning("Script ${result.scriptFile.name} does not have onDispose method")
                    }
                }
            }

        scriptState[ScriptState.Evaluated::class] = scriptState[ScriptState.Compiled::class]?.map {
            evaluateScript(it as ScriptState.Compiled)
        }?.toList() ?: emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ScriptState> getByScriptState(state: KClass<out T>): List<T> {
        return scriptState[state]?.map { it as T } ?: emptyList()
    }

}