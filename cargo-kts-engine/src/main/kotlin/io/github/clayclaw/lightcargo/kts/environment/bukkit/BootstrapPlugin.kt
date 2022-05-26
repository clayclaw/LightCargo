package io.github.clayclaw.lightcargo.kts.environment.bukkit

import io.github.clayclaw.lightcargo.kts.definition.ScriptState
import io.github.clayclaw.lightcargo.kts.definition.manager.getByScriptState
import io.github.clayclaw.lightcargo.kts.environment.bukkit.command.CommandRegistry
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext.startKoin
import org.koin.ksp.generated.module
import kotlin.system.measureTimeMillis

class BootstrapPlugin: JavaPlugin() {

    private lateinit var koin: KoinApplication

    init {
        instance = this
        pluginClassLoader = this.classLoader
    }

    override fun onEnable() {
        logger.warning("Classloader = ${classLoader.definedPackages.map { it.name }}")
        koin = startKoin {
            printLogger()
            modules(KoinModuleKts().module)
        }

        CommandRegistry().registerCommands()

        server.scheduler.runTask(this, Runnable {
            ScriptLoader().loadAll()
        })
    }

    override fun onDisable() {
        koin.close()
    }

    companion object {
        lateinit var instance: BootstrapPlugin
        internal lateinit var pluginClassLoader: ClassLoader
    }

}

class ScriptLoader: KoinComponent {

    private val scriptManager: BukkitScriptManager by inject()

    fun loadAll() {
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