package io.github.clayclaw.lightcargo.kts.environment.bukkit.annotation

import io.github.clayclaw.lightcargo.kts.environment.bukkit.classloading.ScriptClasspathPlans
import io.github.clayclaw.lightcargo.kts.environment.bukkit.classloading.pluginClasspathFiles
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.collectedAnnotations
import kotlin.script.experimental.jvm.updateClasspath

fun ScriptCompilationConfiguration.Builder.resolveBukkitAnnotations(context: ScriptConfigurationRefinementContext) {
    val annotations = context.collectedData
        ?.get(ScriptCollectedData.collectedAnnotations)
        ?.takeIf { it.isNotEmpty() }
        ?: return
    annotations.forEach { (annotation, _) ->
        when(annotation) {
            is RequiredPlugins -> {
                annotation.plugins.map {
                    val plugin = requirePlugin(it)
                    val files = pluginClasspathFiles(plugin)
                    ScriptClasspathPlans.getOrCreate(context)
                        .recordRequiredPlugin(plugin.name, plugin.javaClass.classLoader, files)
                    files
                }.flatten().let { files ->
                    if (files.isNotEmpty()) updateClasspath(files)
                }
            }
            else -> {}
        }
    }
}
