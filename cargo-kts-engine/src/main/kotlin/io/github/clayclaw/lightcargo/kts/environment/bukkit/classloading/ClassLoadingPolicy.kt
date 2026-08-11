package io.github.clayclaw.lightcargo.kts.environment.bukkit.classloading

data class PluginClassLoaderRoute(
    val pluginName: String,
    val classLoader: ClassLoader,
    val index: ClassPathIndex
) {
    fun ownsClass(className: String): Boolean = index.ownsClass(className)
}

data class ClassLoaderRoute(
    val owner: String,
    val classLoader: ClassLoader
)

data class ClassLoadingPolicy(
    val reactantClassLoader: ClassLoader,
    val engineClassLoader: ClassLoader,
    val serverClassLoader: ClassLoader?,
    val pluginRoutes: List<PluginClassLoaderRoute> = emptyList(),
    val reactantPackagePrefixes: Set<String> = defaultReactantPackagePrefixes(),
    val enginePackagePrefixes: Set<String> = defaultEnginePackagePrefixes(),
    val serverPackagePrefixes: Set<String> = defaultServerPackagePrefixes(),
    val reactantClassIndex: ClassPathIndex = ClassPathIndex.empty,
    val engineClassIndex: ClassPathIndex = ClassPathIndex.empty
) {
    fun routeFor(className: String): ClassLoaderRoute? {
        return when {
            className.isJavaPlatformClass() -> null
            enginePackagePrefixes.matchesClassName(className) || engineClassIndex.ownsClass(className) -> {
                ClassLoaderRoute("LightCargo-KtsEngine", engineClassLoader)
            }
            reactantPackagePrefixes.matchesClassName(className) || reactantClassIndex.ownsClass(className) -> {
                ClassLoaderRoute("Reactant", reactantClassLoader)
            }
            else -> {
                pluginRoutes.firstOrNull { it.ownsClass(className) }
                    ?.let { ClassLoaderRoute(it.pluginName, it.classLoader) }
                    ?: serverPackagePrefixes
                        .takeIf { it.matchesClassName(className) }
                        ?.let { ClassLoaderRoute("server", serverClassLoader ?: engineClassLoader) }
            }
        }
    }

    fun protectedOwnerFor(className: String): String? {
        return routeFor(className)?.owner
    }

    fun protectedPackageOwnerFor(packageName: String): String? {
        val className = "$packageName.__LightCargoPackageProbe"
        return protectedOwnerFor(className)
    }
}

fun defaultReactantPackagePrefixes(): Set<String> {
    val configuredPrefixes = System.getProperty("lightcargo.kts.reactantSharedPackages")
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    return linkedSetOf(
        "kotlin.",
        "kotlinx.",
        "org.jetbrains.kotlin.",
        "org.jetbrains.annotations.",
        "org.intellij.lang.annotations.",
        "dev.reactant.reactant."
    ).apply {
        addAll(configuredPrefixes.map { it.ensurePackagePrefix() })
    }
}

fun defaultEnginePackagePrefixes(): Set<String> = linkedSetOf(
    "kotlin.script.",
    "io.github.clayclaw.lightcargo.kts."
)

fun defaultServerPackagePrefixes(): Set<String> = linkedSetOf(
    "org.bukkit.",
    "net.md_5.bungee.",
    "com.destroystokyo.paper.",
    "io.papermc.paper."
)

internal fun Set<String>.matchesClassName(className: String): Boolean {
    return any { prefix -> className.startsWith(prefix.ensurePackagePrefix()) }
}

private fun String.ensurePackagePrefix(): String {
    return if (endsWith(".")) this else "$this."
}

private fun String.isJavaPlatformClass(): Boolean {
    return startsWith("java.")
        || startsWith("javax.")
        || startsWith("jdk.")
        || startsWith("sun.")
        || startsWith("com.sun.")
}
