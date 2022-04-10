package io.github.clayclaw.lightcargo.kts.environment.bukkit.command

import io.github.clayclaw.lightcargo.kts.definition.ScriptState
import io.github.clayclaw.lightcargo.kts.definition.manager.getByScriptState
import io.github.clayclaw.lightcargo.kts.environment.bukkit.BukkitScriptManager
import kotlinx.coroutines.runBlocking
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import kotlin.system.measureTimeMillis

class RdsCommandHandler(
    private val scriptManager: BukkitScriptManager
): CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(!sender.hasPermission("lightcargo.ktsengine.control")) return false
        sender.sendWithConsole("Warning: RDS should not be used in production.")
        val time = measureTimeMillis {
            runBlocking {
                scriptManager.discoverScripts()
                sender.sendWithConsole("=> Discovered ${scriptManager.getByScriptState<ScriptState.Discovered>().size} scripts.")
                sender.sendWithConsole("=> Compiling..")
                scriptManager.compileScripts()
            }
        }
        sender.sendWithConsole("Rediscovered and compiled scripts in $time ms")
        return true
    }

}