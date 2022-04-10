package io.github.clayclaw.lightcargo.kts.environment.bukkit.command

import io.github.clayclaw.lightcargo.kts.definition.ScriptState
import io.github.clayclaw.lightcargo.kts.definition.manager.getByScriptState
import io.github.clayclaw.lightcargo.kts.environment.bukkit.BukkitScriptManager
import kotlinx.coroutines.runBlocking
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import kotlin.system.measureTimeMillis

class RcsCommandHandler(
    private val scriptManager: BukkitScriptManager
): CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(!sender.hasPermission("lightcargo.ktsengine.control")) return false
        sender.sendMessage("Warning: RCS should not be used in production.")
        val time = measureTimeMillis {
            runBlocking {
                scriptManager.compileScripts()
                sender.sendWithConsole("=> Recompiled ${scriptManager.getByScriptState<ScriptState.Compiled>().size} scripts.")
                sender.sendWithConsole("=> Evaluating..")
                scriptManager.evaluateScripts()
            }
        }
        sender.sendMessage("Recompiled and evaluated scripts in $time ms")
        return true
    }
}