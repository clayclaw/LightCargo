package io.github.clayclaw.lightcargo.kts.environment.bukkit.command

import dev.reactant.reactant.core.component.lifecycle.LifeCycleHook
import io.github.clayclaw.lightcargo.kts.environment.bukkit.BukkitScriptManager
import org.bukkit.Bukkit

class CommandRegistry(
    private val scriptManager: BukkitScriptManager
): LifeCycleHook {

    override fun onEnable() {
        Bukkit.getPluginCommand("rds")!!.setExecutor(RdsCommandHandler(scriptManager))
        Bukkit.getPluginCommand("rcs")!!.setExecutor(RcsCommandHandler(scriptManager))
        Bukkit.getPluginCommand("rs")!!.setExecutor(RsCommandHandler(scriptManager))
        Bukkit.getPluginCommand("fullrs")!!.setExecutor(FullRsCommandHandler(scriptManager))
    }

}