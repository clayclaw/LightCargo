package io.github.clayclaw.lightcargo.kts.environment.bukkit.command

import io.github.clayclaw.lightcargo.kts.environment.bukkit.BukkitScriptManager
import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CommandRegistry: KoinComponent {
    private val scriptManager: BukkitScriptManager by inject()
    fun registerCommands() {
        Bukkit.getPluginCommand("rds")!!.setExecutor(RdsCommandHandler(scriptManager))
        Bukkit.getPluginCommand("rcs")!!.setExecutor(RcsCommandHandler(scriptManager))
        Bukkit.getPluginCommand("rs")!!.setExecutor(RsCommandHandler(scriptManager))
    }
}