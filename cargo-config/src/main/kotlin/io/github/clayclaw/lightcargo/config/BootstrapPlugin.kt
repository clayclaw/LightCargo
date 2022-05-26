package io.github.clayclaw.lightcargo.config

import io.github.clayclaw.lightcargo.config.model.readConfig
import org.bukkit.plugin.java.JavaPlugin

class BootstrapPlugin: JavaPlugin() {
    companion object {
        lateinit var instance: BootstrapPlugin
    }
    init {
        instance = this
    }
    override fun onEnable() {
        val config = readConfig<BootstrapConfig>("plugins/LightCargo/module-config/config.json")
        logger.info("Debug = ${config.content.debug}")
    }
}

data class BootstrapConfig(
    var debug: Boolean = false
)