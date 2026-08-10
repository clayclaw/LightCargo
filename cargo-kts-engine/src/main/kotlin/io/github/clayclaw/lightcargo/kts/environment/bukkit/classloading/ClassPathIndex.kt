package io.github.clayclaw.lightcargo.kts.environment.bukkit.classloading

import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

data class ClassPathIndex(
    val classNames: Set<String>,
    val packageNames: Set<String>
) {
    fun ownsClass(className: String): Boolean {
        return className in classNames || packageNames.any { className.startsWith("$it.") }
    }

    fun containsPackagePrefix(prefix: String): Boolean {
        val normalizedPrefix = prefix.trimEnd('.')
        return packageNames.any { it == normalizedPrefix || it.startsWith("$normalizedPrefix.") }
    }

    companion object {
        val empty = ClassPathIndex(emptySet(), emptySet())
    }
}

fun ClassLoader.classpathFiles(): List<File> {
    return (this as? URLClassLoader)
        ?.urLs
        ?.mapNotNull { url -> runCatching { File(url.toURI()) }.getOrNull() }
        ?: emptyList()
}

fun Iterable<File>.toClassPathIndex(): ClassPathIndex {
    val classNames = linkedSetOf<String>()
    val packageNames = linkedSetOf<String>()

    forEach { file ->
        when {
            file.isFile && file.extension.equals("jar", ignoreCase = true) -> {
                runCatching {
                    JarFile(file).use { jar ->
                        jar.entries().asSequence()
                            .filter { !it.isDirectory && it.name.endsWith(".class") }
                            .forEach { entry -> addClassName(entry.name, classNames, packageNames) }
                    }
                }
            }
            file.isDirectory -> {
                file.walkTopDown()
                    .filter { it.isFile && it.extension.equals("class", ignoreCase = true) }
                    .forEach {
                        addClassName(
                            it.relativeTo(file).invariantSeparatorsPath,
                            classNames,
                            packageNames
                        )
                    }
            }
        }
    }

    return ClassPathIndex(classNames, packageNames)
}

private fun addClassName(
    entryName: String,
    classNames: MutableSet<String>,
    packageNames: MutableSet<String>
) {
    val className = entryName
        .removeSuffix(".class")
        .replace('/', '.')
        .replace('\\', '.')

    classNames.add(className)
    className.substringBeforeLast('.', missingDelimiterValue = "")
        .takeIf { it.isNotBlank() }
        ?.let(packageNames::add)
}
