package io.github.clayclaw.lightcargo.kts.definition.annotation

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.dependencies.resolveFromScriptSourceAnnotations
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.jvm.updateClasspath

private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

// RefineScriptCompilationConfigurationHandler
fun resolveAnnotations(
    scriptFolder: File? = null,
    context: ScriptConfigurationRefinementContext,
    body: (ScriptCompilationConfiguration.Builder) -> Unit = {}
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
        ?: return context.compilationConfiguration.asSuccess()
    return context.compilationConfiguration.with {
        val scriptBaseDir = (context.script as? FileBasedScriptSource)?.file?.parentFile
        annotations.forEach { (annotation, _) ->
            when(annotation) {
                is Import -> annotation.scriptPaths
                    .map { FileScriptSource((scriptFolder?.resolve(it) ?: File(it))) }
                    .let { importScripts.append(it) }
                else -> {}
            }
        }

        body(this)

        runBlocking {
            resolver.resolveFromScriptSourceAnnotations(annotations)
        }.onSuccess {
            updateClasspath(it)
            asSuccess()
        }
    }.asSuccess()
}