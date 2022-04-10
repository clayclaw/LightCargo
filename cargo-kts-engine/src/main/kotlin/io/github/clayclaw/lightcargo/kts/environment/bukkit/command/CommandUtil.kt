package io.github.clayclaw.lightcargo.kts.environment.bukkit.command

import io.github.clayclaw.lightcargo.kts.environment.bukkit.BootstrapPlugin
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender

fun CommandSender.sendWithConsole(msg: Any) {
    if(this !is ConsoleCommandSender) {
        BootstrapPlugin.instance.logger.warning("[SCRIPTING] $msg")
    }
    sendMessage(msg.toString())
}