package io.github.clayclaw.lightcargo.kts.environment.bukkit

import dev.reactant.reactant.core.component.Component
import io.github.clayclaw.lightcargo.kts.definition.ScriptFailureReporter
import io.github.clayclaw.lightcargo.kts.definition.ScriptState
import io.github.clayclaw.lightcargo.kts.definition.discoverAllScriptRecursively
import io.github.clayclaw.lightcargo.kts.definition.manager.ScriptManager
import io.github.clayclaw.lightcargo.kts.environment.bukkit.classloading.ScriptClasspathPlans
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.reflect.KClass
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvmhost.JvmScriptCompiler

@Component
class BukkitScriptManager : ScriptManager {

    private val compiler = JvmScriptCompiler(BukkitScriptHostConfig)
    private val evaluator = BasicJvmScriptEvaluator()

    private val scriptState: HashMap<KClass<out ScriptState>, MutableList<ScriptState>> = hashMapOf()

    private fun discoverAllScript(): Sequence<ScriptState.Discovered> {
        bukkitScriptBaseDir.mkdirs()
        return bukkitScriptBaseDir.discoverAllScriptRecursively(BUKKIT_SCRIPT_DEFINITION_NAME)
            .map { ScriptState.Discovered(it) }
    }

    private suspend fun compileScript(scriptFile: File): ScriptState.Compiled {
        bukkitScriptCacheDir.mkdirs()
        ScriptClasspathPlans.reset(scriptFile)
        val compiledScript = compiler(scriptFile.toScriptSource(), BukkitScriptCompilationConfig).valueOrThrow()
        return ScriptState.Compiled(scriptFile, compiledScript, scriptFile.lastModified())
    }

    private suspend fun compileScriptCatching(scriptFile: File): ScriptState.Compiled? {
        return runCatching {
            compileScript(scriptFile)
        }.onFailure {
            ScriptFailureReporter.log(BootstrapPlugin.instance.logger, "compiling", scriptFile, it)
        }.getOrNull()
    }

    private suspend fun evaluateScript(compiledScript: ScriptState.Compiled): ScriptState.Evaluated {
        val classpathPlan = ScriptClasspathPlans.get(compiledScript.scriptFile)
        BootstrapPlugin.instance.logger.fine(
            "Script ${compiledScript.scriptFile.name} classloader routes: ${classpathPlan.describeRoutes()}"
        )
        evaluator(compiledScript.compiledScript, bukkitScriptEvaluationConfig(compiledScript.scriptFile)).valueOrThrow().let { result ->
            when (val returnValue = result.returnValue) {
                is ResultValue.NotEvaluated -> throw IllegalStateException("Script is not evaluated")
                is ResultValue.Error -> throw returnValue.error
                is ResultValue.Value -> returnValue.scriptClass to returnValue.scriptInstance
                is ResultValue.Unit -> returnValue.scriptClass to returnValue.scriptInstance
            }.let {
                return ScriptState.Evaluated(compiledScript.scriptFile, it.first, it.second)
            }
        }
    }

    private suspend fun evaluateScriptCatching(compiledScript: ScriptState.Compiled): ScriptState.Evaluated? {
        return runCatching {
            evaluateScript(compiledScript)
        }.onFailure {
            ScriptFailureReporter.log(
                BootstrapPlugin.instance.logger,
                "evaluating",
                compiledScript.scriptFile,
                it
            )
        }.getOrNull()
    }

    internal fun discoverScripts() {
        scriptState[ScriptState.Discovered::class] = discoverAllScript().toMutableList()
    }


    internal suspend fun compileScripts() {
        scriptState[ScriptState.Compiled::class] = scriptState[ScriptState.Discovered::class]?.mapNotNull {
            compileScriptCatching(it.scriptFile)
        }?.toMutableList() ?: LinkedList()
    }

    /**
     * @return Re-compiled scripts
     */
    internal suspend fun recompileScripts(): List<ScriptState.Compiled> {
        val groups = scriptState[ScriptState.Compiled::class]
            ?.groupBy { it.scriptFile.lastModified() != (it as ScriptState.Compiled).lastModified }
            ?: return emptyList()
        val recompiled = groups[true]?.mapNotNull { compileScriptCatching(it.scriptFile) } ?: emptyList()
        scriptState[ScriptState.Compiled::class] = recompiled.plus(groups[false] ?: LinkedList()).toMutableList()
        return recompiled
    }

    internal suspend fun evaluateScripts() {
        // reload all compiled script
        scriptState[ScriptState.Compiled::class]?.let {
            evaluateSelectedScripts(it.filterIsInstance<ScriptState.Compiled>())
        }
    }

    internal suspend fun evaluateSelectedScripts(list: List<ScriptState.Compiled>) {
        val paths = HashSet<String>()
        list.forEach { paths.add(it.scriptFile.absolutePath) }
        // Unload all script first
        scriptState[ScriptState.Evaluated::class]
            ?.filter { paths.contains(it.scriptFile.absolutePath) }
            ?.forEach {
                (it as? ScriptState.Evaluated)?.let { result ->
                    runCatching {
                        result.scriptClass?.java?.getDeclaredMethod("onDispose")?.invoke(result.scriptInstance)
                    }.onFailure {
                        BootstrapPlugin.instance.logger.warning("Script ${result.scriptFile.name} does not have onDispose method")
                    }
                }
            }

        scriptState[ScriptState.Evaluated::class]?.removeIf { paths.contains(it.scriptFile.absolutePath) }

        // evaluate all scripts; isolate failures so one bad script does not abort Reactant init
        list
            .sortedBy { it.scriptFile.name.firstOrNull() ?: 'Z' }
            .mapNotNull { evaluateScriptCatching(it) }
            .forEach {
                scriptState.getOrPut(ScriptState.Evaluated::class) { LinkedList() }.add(it)
            }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ScriptState> getByScriptState(state: KClass<out T>): List<T> {
        return scriptState[state]?.map { it as T } ?: emptyList()
    }

    private fun printClassLoader(clazz: KClass<*>?) {
        println("------------ ClassLoader Dump ------------ ")
        println("ScriptClassLoader: ${clazz?.java?.classLoader} | PluginLoader: ${BootstrapPlugin.pluginClassLoader}")
        var loader = clazz?.java?.classLoader
        while(loader != null) {
            println("ScriptLoader = $loader")
            // println("             => ${loader.definedPackages.joinToString(", ") { it.name }}")
            loader = loader.parent
        }
        loader = BootstrapPlugin.pluginClassLoader
        while(loader != null) {
            println("PluginLoader = $loader")
            loader = loader.parent
        }
        val func: () -> Unit = {}
        println("ExampleKotlinLoader = ${func.javaClass.classLoader}")
    }

}
