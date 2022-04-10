package io.github.clayclaw.lightcargo.kts.environment.bukkit.annotation

import org.bukkit.Bukkit
import java.io.File
import java.net.URLClassLoader
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
                    Bukkit.getPluginManager().getPlugin(it)!!.javaClass.classLoader.getFiles()
                }.flatten().let {
                    updateClasspath(it)
                }
            }
            else -> {}
        }
    }
}

fun ClassLoader.getFiles(): List<File> {
    return (this as? URLClassLoader?)?.urLs?.mapNotNull { File(it.toURI().schemeSpecificPart) } ?: emptyList()
}