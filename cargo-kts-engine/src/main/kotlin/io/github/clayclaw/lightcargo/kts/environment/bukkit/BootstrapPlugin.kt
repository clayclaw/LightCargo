package io.github.clayclaw.lightcargo.kts.environment.bukkit

import dev.reactant.reactant.core.ReactantPlugin
import dev.reactant.reactant.core.component.Component
import dev.reactant.reactant.core.component.lifecycle.LifeCycleHook
import io.github.clayclaw.lightcargo.kts.definition.ScriptState
import io.github.clayclaw.lightcargo.kts.definition.manager.getByScriptState
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin
import kotlin.system.measureTimeMillis

@ReactantPlugin(servicePackages = ["io.github.clayclaw.lightcargo.kts"])
class BootstrapPlugin: JavaPlugin() {

    init {
        instance = this
        pluginClassLoader = this.classLoader
    }

    override fun onEnable() {
        logger.warning("Classloader = ${classLoader.definedPackages.map { it.name }}")
    }

    companion object {
        lateinit var instance: BootstrapPlugin
        internal lateinit var pluginClassLoader: ClassLoader
    }

}

@Component
class ScriptLoader(
    private val scriptManager: BukkitScriptManager,
): LifeCycleHook {

    override fun onEnable() {
        loadAll()
    }

    private fun loadAll() {
        scriptManager.run {
            runBlocking {
                BootstrapPlugin.instance.logger.info("Searching for scripts..")
                discoverScripts()
                BootstrapPlugin.instance.logger.info("Found ${scriptManager.getByScriptState<ScriptState.Discovered>().size} scripts! Compiling...")
                measureTimeMillis {
                    compileScripts()
                }.let { BootstrapPlugin.instance.logger.info("Took ${it}ms to compile scripts") }
                BootstrapPlugin.instance.logger.info("Evaluating...")
                evaluateScripts()
                BootstrapPlugin.instance.logger.info("Completed.")
            }
        }
    }
}