package io.github.clayclaw.lightcargo.kts.environment.bukkit.command

import io.github.clayclaw.lightcargo.kts.environment.bukkit.BukkitScriptManager
import kotlinx.coroutines.runBlocking
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import kotlin.system.measureTimeMillis

class RsCommandHandler(
    private val scriptManager: BukkitScriptManager
): CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(!sender.hasPermission("lightcargo.ktsengine.control")) return false
        sender.sendMessage("Warning: RS should not be used in production.")
        val time = measureTimeMillis {
            runBlocking {
                val recompileList = scriptManager.recompileScripts()
                sender.sendWithConsole("=> Detected ${recompileList.size} changed scripts.")
                sender.sendWithConsole("=> Evaluating..")
                scriptManager.evaluateSelectedScripts(recompileList)
            }
        }
        sender.sendMessage("Reloaded scripts in $time ms")
        return true
    }
}