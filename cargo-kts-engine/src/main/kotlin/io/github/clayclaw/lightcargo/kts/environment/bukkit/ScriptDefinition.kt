package io.github.clayclaw.lightcargo.kts.environment.bukkit

import dev.reactant.reactant.core.ReactantCore
import io.github.clayclaw.lightcargo.kts.definition.*
import io.github.clayclaw.lightcargo.kts.definition.annotation.*
import io.github.clayclaw.lightcargo.kts.definition.kotlin.FileBasedScriptCache
import io.github.clayclaw.lightcargo.kts.environment.bukkit.annotation.*
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.jvm.*

val bukkitScriptBaseDir = File("lightcargo-kts/scripts")
val bukkitScriptCacheDir = File("lightcargo-kts/cache")
const val BUKKIT_SCRIPT_DEFINITION_NAME = "lc.kts"

@KotlinScript(
    displayName = "LightCargo Bukkit Script",
    fileExtension = BUKKIT_SCRIPT_DEFINITION_NAME,
    compilationConfiguration = BukkitScriptCompilationConfig::class,
    evaluationConfiguration = BukkitScriptEvaluationConfig::class,
    hostConfiguration = BukkitScriptHostConfig::class
)
abstract class BukkitScriptBase: ScriptBase

object BukkitScriptCompilationConfig: ScriptCompilationConfiguration({
    defaultImports(javaImports + kotlinCoroutinesImports + annotationsImports + bukkitAnnotationsImports + bukkitImports)
    jvm {
        dependenciesFromClassloader(classLoader = ReactantCore.instance.javaClass.classLoader, wholeClasspath = true)
        // dependenciesFromClassContext(ReactantCore::class, wholeClasspath = true)
        compilerOptions.append("-Xadd-modules=ALL-MODULE-PATH")
    }
    refineConfiguration {
        onAnnotations(
            Import::class, RequiredPlugins::class, Repository::class, DependsOn::class,
            handler=::resolveBukkitScriptAnnotations
        )
    }
    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
}) {
    private fun readResolve(): Any = BukkitScriptCompilationConfig
}

object BukkitScriptEvaluationConfig: ScriptEvaluationConfiguration({
    jvm {
        baseClassLoader(ReactantCore.instance.javaClass.classLoader)
        loadDependencies(false)
    }
}) {
    private fun readResolve(): Any = BukkitScriptEvaluationConfig
}

object BukkitScriptHostConfig: ScriptingHostConfiguration({
    jvm {
        compilationCache(FileBasedScriptCache(bukkitScriptCacheDir))
        baseClassLoader(ReactantCore.instance.javaClass.classLoader)
    }
    getScriptingClass(JvmGetScriptingClass())
}) {
    private fun readResolve(): Any = BukkitScriptHostConfig
}

private fun resolveBukkitScriptAnnotations(context: ScriptConfigurationRefinementContext) = resolveAnnotations(
    bukkitScriptBaseDir, context
) {
    it.resolveBukkitAnnotations(context)
}

val bukkitImports = listOf(
    "org.bukkit.*",
    "org.bukkit.block.*",
    "org.bukkit.block.banner.*",
    "org.bukkit.command.*",
    "org.bukkit.configuration.*",
    "org.bukkit.configuration.file.*",
    "org.bukkit.configuration.serialization.*",
    "org.bukkit.enchantments.*",
    "org.bukkit.entity.*",
    "org.bukkit.entity.minecart.*",
    "org.bukkit.event.*",
    "org.bukkit.event.block.*",
    "org.bukkit.event.enchantment.*",
    "org.bukkit.event.entity.*",
    "org.bukkit.event.hanging.*",
    "org.bukkit.event.inventory.*",
    "org.bukkit.event.painting.*",
    "org.bukkit.event.player.*",
    "org.bukkit.event.server.*",
    "org.bukkit.event.weather.*",
    "org.bukkit.event.world.*",
    "org.bukkit.generator.*",
    "org.bukkit.help.*",
    "org.bukkit.inventory.*",
    "org.bukkit.inventory.meta.*",
    "org.bukkit.map.*",
    "org.bukkit.material.*",
    "org.bukkit.metadata.*",
    "org.bukkit.permissions.*",
    "org.bukkit.plugin.*",
    "org.bukkit.plugin.messaging.*",
    "org.bukkit.potion.*",
    "org.bukkit.projectiles.*",
    "org.bukkit.scheduler.*",
    "org.bukkit.scoreboard.*",
    "org.bukkit.util.*",
    "org.bukkit.util.io.*",
    "org.bukkit.util.noise.*",
    "org.bukkit.util.permissions.*",
)

val bukkitAnnotationsImports = listOf(
    "io.github.clayclaw.lightcargo.kts.environment.bukkit.annotation.*"
)