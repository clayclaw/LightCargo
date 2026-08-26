package io.github.clayclaw.lightcargo.kts.environment.bukkit.annotation

import io.github.clayclaw.lightcargo.kts.environment.bukkit.classloading.ScriptClasspathPlan
import io.github.clayclaw.lightcargo.kts.environment.bukkit.classloading.ScriptClasspathPlans
import io.github.clayclaw.lightcargo.kts.environment.bukkit.classloading.pluginClasspathFiles
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.io.File

private val requiredPluginsAnnotation = Regex(
    """@file\s*:\s*RequiredPlugins\s*\(([^)]*)\)"""
)
private val annotationStringLiteral = Regex("\"([^\"]+)\"")

/**
 * Compile-cache hits skip annotation refinement, which would leave [ScriptClasspathPlan.pluginRoutes]
 * empty after [ScriptClasspathPlans.reset]. Re-read file RequiredPlugins annotation from source and
 * register plugin classloader routes before evaluation.
 */
fun rehydrateRequiredPluginRoutes(scriptFile: File): ScriptClasspathPlan {
    val plan = ScriptClasspathPlans.getOrCreate(scriptFile)
    parseRequiredPluginNames(scriptFile.readText()).forEach { pluginName ->
        val plugin = requirePlugin(pluginName)
        plan.recordRequiredPlugin(plugin.name, plugin.javaClass.classLoader, pluginClasspathFiles(plugin))
    }
    return plan
}

fun parseRequiredPluginNames(scriptText: String): List<String> {
    return requiredPluginsAnnotation.findAll(scriptText)
        .flatMap { match ->
            annotationStringLiteral.findAll(match.groupValues[1]).map { it.groupValues[1] }
        }
        .distinct()
        .toList()
}

internal fun requirePlugin(pluginName: String): Plugin {
    return Bukkit.getPluginManager().getPlugin(pluginName)
        ?: throw IllegalArgumentException("Plugin " + pluginName + " is required but not found")
}
