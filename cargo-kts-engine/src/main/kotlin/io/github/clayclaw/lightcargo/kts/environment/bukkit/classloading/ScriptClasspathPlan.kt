package io.github.clayclaw.lightcargo.kts.environment.bukkit.classloading

import dev.reactant.reactant.core.ReactantCore
import io.github.clayclaw.lightcargo.kts.environment.bukkit.BootstrapPlugin
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.host.FileBasedScriptSource

data class ScriptClasspathPlan(
    val scriptFile: File,
    val compileClasspath: LinkedHashSet<File> = linkedSetOf(),
    val runtimePrivateClasspath: LinkedHashSet<File> = linkedSetOf(),
    val pluginRoutes: LinkedHashMap<String, PluginClassLoaderRoute> = linkedMapOf(),
    val diagnostics: MutableList<String> = mutableListOf()
) {
    fun createClassLoader(): ScriptClassLoader {
        val runtimeUrls = runtimePrivateClasspath.map { it.toURI().toURL() }
        return ScriptClassLoader(runtimeUrls, createPolicy())
    }

    fun createPolicy(): ClassLoadingPolicy {
        val reactantClassLoader = ReactantCore.instance.javaClass.classLoader
        val engineClassLoader = BootstrapPlugin.pluginClassLoader
        return ClassLoadingPolicy(
            reactantClassLoader = reactantClassLoader,
            engineClassLoader = engineClassLoader,
            serverClassLoader = engineClassLoader.parent,
            pluginRoutes = pluginRoutes.values.toList(),
            // Index fat-jar contents so shaded deps (e.g. RxJava) route to the owning plugin loader.
            reactantClassIndex = HostClassPathIndexes.forLoader(reactantClassLoader),
            engineClassIndex = HostClassPathIndexes.forLoader(engineClassLoader)
        )
    }

    fun describeRoutes(): String {
        val plugins = pluginRoutes.keys.takeIf { it.isNotEmpty() }?.joinToString() ?: "none"
        val privateFiles = runtimePrivateClasspath
            .map { it.name }
            .takeIf { it.isNotEmpty() }
            ?.joinToString()
            ?: "none"

        return "reactant=shared, engine=LightCargo-KtsEngine, plugins=$plugins, private=$privateFiles"
    }

    fun recordRequiredPlugin(pluginName: String, classLoader: ClassLoader, files: List<File>) {
        val index = files.toClassPathIndex()
        if (index.classNames.isEmpty()) {
            val message = "Required plugin $pluginName produced an empty class index " +
                "(files=${files.map { it.name }.ifEmpty { listOf("<none>") }}); " +
                "script classes from that plugin may fail at runtime with NoClassDefFoundError."
            if (message !in diagnostics) {
                diagnostics.add(message)
            }
        }
        pluginRoutes[pluginName] = PluginClassLoaderRoute(pluginName, classLoader, index)
        compileClasspath.addAll(files)
    }

    fun recordResolvedDependencies(files: List<File>, policy: ClassLoadingPolicy = createPolicy()): List<File> {
        files.forEach { file ->
            val index = listOf(file).toClassPathIndex()
            val duplicateOwner = findProtectedOwner(index, policy)
            if (duplicateOwner != null) {
                val message = "Script ${scriptFile.name} dependency ${file.name} contains classes owned by $duplicateOwner; " +
                    "declare the owning plugin with @RequiredPlugins or use Reactant's shared runtime instead."
                diagnostics.add(message)
                throw IllegalArgumentException(message)
            }
        }

        compileClasspath.addAll(files)
        runtimePrivateClasspath.addAll(files)
        return files
    }

    private fun findProtectedOwner(index: ClassPathIndex, policy: ClassLoadingPolicy): String? {
        index.classNames.firstNotNullOfOrNull { policy.protectedOwnerFor(it) }?.let { return it }
        return index.packageNames.firstNotNullOfOrNull { policy.protectedPackageOwnerFor(it) }
    }
}

/**
 * Cached indexes of host plugin jar contents (Reactant / engine), not parent/server loaders.
 */
object HostClassPathIndexes {
    private val indexes = ConcurrentHashMap<ClassLoader, ClassPathIndex>()

    fun forLoader(classLoader: ClassLoader): ClassPathIndex {
        return indexes.computeIfAbsent(classLoader) {
            classLoader.classpathFiles().toClassPathIndex()
        }
    }

    fun clear() {
        indexes.clear()
    }
}

object ScriptClasspathPlans {
    private val plans = ConcurrentHashMap<String, ScriptClasspathPlan>()

    fun getOrCreate(context: ScriptConfigurationRefinementContext): ScriptClasspathPlan {
        return getOrCreate(context.scriptFile())
    }

    fun getOrCreate(scriptFile: File): ScriptClasspathPlan {
        val key = scriptFile.planKey()
        return plans.computeIfAbsent(key) {
            ScriptClasspathPlan(scriptFile.canonicalFile)
        }
    }

    fun get(scriptFile: File): ScriptClasspathPlan {
        return plans[scriptFile.planKey()] ?: ScriptClasspathPlan(scriptFile.canonicalFile)
    }

    fun reset(scriptFile: File): ScriptClasspathPlan {
        val plan = ScriptClasspathPlan(scriptFile.canonicalFile)
        plans[scriptFile.planKey()] = plan
        return plan
    }

    private fun ScriptConfigurationRefinementContext.scriptFile(): File {
        return (script as? FileBasedScriptSource)?.file
            ?: throw IllegalArgumentException("Only file-based Bukkit scripts can declare dependencies")
    }

    private fun File.planKey(): String = canonicalFile.absolutePath
}

fun Iterable<File>.toUrls(): List<URL> = map { it.toURI().toURL() }
