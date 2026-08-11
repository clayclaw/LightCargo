package io.github.clayclaw.lightcargo.kts.environment.bukkit.classloading

import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.ToolProvider
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ScriptClassLoaderTest {

    @Test
    fun `protected Reactant classes are delegated before script private classes`() {
        val fqcn = "dev.reactant.reactant.core.SharedType"
        val reactantClasses = compileClass(fqcn)
        val scriptClasses = compileClass(fqcn)
        val reactantLoader = classLoaderFor(reactantClasses)
        val scriptLoader = ScriptClassLoader(
            listOf(scriptClasses.toUri().toURL()),
            ClassLoadingPolicy(
                reactantClassLoader = reactantLoader,
                engineClassLoader = emptyLoader(),
                serverClassLoader = null,
                reactantPackagePrefixes = setOf("dev.reactant.reactant."),
                enginePackagePrefixes = emptySet(),
                serverPackagePrefixes = emptySet()
            )
        )

        assertSame(reactantLoader.loadClass(fqcn), scriptLoader.loadClass(fqcn))
    }

    @Test
    fun `shaded Reactant fat-jar dependencies are delegated via class index`() {
        val fqcn = "io.reactivex.rxjava3.disposables.CompositeDisposable"
        val reactantClasses = compileClass(fqcn)
        val scriptClasses = compileClass(fqcn)
        val reactantLoader = classLoaderFor(reactantClasses)
        val scriptLoader = ScriptClassLoader(
            listOf(scriptClasses.toUri().toURL()),
            ClassLoadingPolicy(
                reactantClassLoader = reactantLoader,
                engineClassLoader = emptyLoader(),
                serverClassLoader = null,
                reactantPackagePrefixes = emptySet(),
                enginePackagePrefixes = emptySet(),
                serverPackagePrefixes = emptySet(),
                reactantClassIndex = listOf(reactantClasses.toFile()).toClassPathIndex()
            )
        )

        assertSame(reactantLoader.loadClass(fqcn), scriptLoader.loadClass(fqcn))
    }

    @Test
    fun `shaded Reactant dependencies are rejected as protected duplicates`() {
        val fqcn = "io.reactivex.rxjava3.disposables.CompositeDisposable"
        val dependencyClasses = compileClass(fqcn)
        val policy = ClassLoadingPolicy(
            reactantClassLoader = emptyLoader(),
            engineClassLoader = emptyLoader(),
            serverClassLoader = null,
            reactantPackagePrefixes = emptySet(),
            enginePackagePrefixes = emptySet(),
            serverPackagePrefixes = emptySet(),
            reactantClassIndex = listOf(dependencyClasses.toFile()).toClassPathIndex()
        )
        val plan = ScriptClasspathPlan(Files.createTempFile("script", ".lc.kts").toFile())

        assertFailsWith<IllegalArgumentException> {
            plan.recordResolvedDependencies(listOf(dependencyClasses.toFile()), policy)
        }
    }

    @Test
    fun `Kotlin scripting classes are delegated to the engine before Reactant Kotlin runtime`() {
        val fqcn = "kotlin.script.experimental.jvmhost.HostType"
        val engineClasses = compileClass(fqcn)
        val reactantClasses = compileClass(fqcn)
        val scriptClasses = compileClass(fqcn)
        val engineLoader = classLoaderFor(engineClasses)
        val reactantLoader = classLoaderFor(reactantClasses)
        val scriptLoader = ScriptClassLoader(
            listOf(scriptClasses.toUri().toURL()),
            ClassLoadingPolicy(
                reactantClassLoader = reactantLoader,
                engineClassLoader = engineLoader,
                serverClassLoader = null,
                reactantPackagePrefixes = setOf("kotlin."),
                enginePackagePrefixes = setOf("kotlin.script."),
                serverPackagePrefixes = emptySet()
            )
        )

        assertSame(engineLoader.loadClass(fqcn), scriptLoader.loadClass(fqcn))
    }

    @Test
    fun `required plugin classes are delegated to the plugin classloader`() {
        val fqcn = "io.github.clayclaw.lightcargo.api.LightCargoApi"
        val pluginClasses = compileClass(fqcn)
        val scriptClasses = compileClass(fqcn)
        val pluginLoader = classLoaderFor(pluginClasses)
        val scriptLoader = ScriptClassLoader(
            listOf(scriptClasses.toUri().toURL()),
            ClassLoadingPolicy(
                reactantClassLoader = emptyLoader(),
                engineClassLoader = emptyLoader(),
                serverClassLoader = null,
                pluginRoutes = listOf(
                    PluginClassLoaderRoute(
                        "LightCargo",
                        pluginLoader,
                        listOf(pluginClasses.toFile()).toClassPathIndex()
                    )
                ),
                reactantPackagePrefixes = emptySet(),
                enginePackagePrefixes = emptySet(),
                serverPackagePrefixes = emptySet()
            )
        )

        assertSame(pluginLoader.loadClass(fqcn), scriptLoader.loadClass(fqcn))
    }

    @Test
    fun `script private classes are loaded by the script classloader`() {
        val fqcn = "scriptprivate.DependencyType"
        val scriptClasses = compileClass(fqcn)
        val scriptLoader = ScriptClassLoader(
            listOf(scriptClasses.toUri().toURL()),
            ClassLoadingPolicy(
                reactantClassLoader = emptyLoader(),
                engineClassLoader = emptyLoader(),
                serverClassLoader = null,
                reactantPackagePrefixes = emptySet(),
                enginePackagePrefixes = emptySet(),
                serverPackagePrefixes = emptySet()
            )
        )

        assertSame(scriptLoader, scriptLoader.loadClass(fqcn).classLoader)
    }

    @Test
    fun `protected duplicate dependencies are rejected`() {
        val fqcn = "kotlinx.coroutines.SharedDependency"
        val dependencyClasses = compileClass(fqcn)
        val policy = ClassLoadingPolicy(
            reactantClassLoader = emptyLoader(),
            engineClassLoader = emptyLoader(),
            serverClassLoader = null,
            reactantPackagePrefixes = setOf("kotlinx."),
            enginePackagePrefixes = emptySet(),
            serverPackagePrefixes = emptySet()
        )
        val plan = ScriptClasspathPlan(Files.createTempFile("script", ".lc.kts").toFile())

        assertFailsWith<IllegalArgumentException> {
            plan.recordResolvedDependencies(listOf(dependencyClasses.toFile()), policy)
        }
    }

    private fun compileClass(fqcn: String): Path {
        val root = Files.createTempDirectory("lightcargo-classloader-test")
        val sourceRoot = root.resolve("src")
        val outputRoot = root.resolve("classes")
        val packageName = fqcn.substringBeforeLast('.')
        val simpleName = fqcn.substringAfterLast('.')
        val sourceFile = sourceRoot
            .resolve(packageName.replace('.', '/'))
            .resolve("$simpleName.java")

        Files.createDirectories(sourceFile.parent)
        Files.createDirectories(outputRoot)
        sourceFile.writeText("package $packageName; public class $simpleName {}")

        val compiler = ToolProvider.getSystemJavaCompiler()
            ?: error("JDK compiler is required to run classloader tests")
        val result = compiler.run(null, null, null, "-d", outputRoot.toString(), sourceFile.toString())
        check(result == 0) { "Could not compile $fqcn" }

        return outputRoot
    }

    private fun classLoaderFor(classes: Path): URLClassLoader {
        return URLClassLoader(arrayOf(classes.toUri().toURL()), null)
    }

    private fun emptyLoader(): ClassLoader = URLClassLoader(emptyArray(), null)
}
