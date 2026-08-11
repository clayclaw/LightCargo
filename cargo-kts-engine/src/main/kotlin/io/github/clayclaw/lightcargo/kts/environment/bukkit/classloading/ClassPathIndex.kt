package io.github.clayclaw.lightcargo.kts.environment.bukkit.classloading

import java.io.File
import java.net.URI
import java.net.URL
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
        ?.mapNotNull { it.toExistingFile() }
        ?: emptyList()
}

fun ClassLoader.classpathFilesIncludingParents(): List<File> {
    val files = linkedSetOf<File>()
    var current: ClassLoader? = this
    while (current != null) {
        files.addAll(current.classpathFiles())
        current = current.parent
    }
    return files.toList()
}

fun serverApiClasspathFiles(
    classNames: Collection<String> = defaultServerApiClassNames()
): List<File> {
    val files = linkedSetOf<File>()
    var resolvedAnyApiClass = false
    classNames.forEach { name ->
        runCatching {
            val clazz = Class.forName(name)
            resolvedAnyApiClass = true
            clazz.protectionDomain?.codeSource?.location?.toExistingFile()?.let(files::add)
            clazz.classLoader?.classpathFilesIncludingParents()?.let(files::addAll)
        }
    }
    if (resolvedAnyApiClass && files.isEmpty()) {
        files.addAll(javaClassPathFiles())
    }
    return files.toList()
}

fun javaClassPathFiles(): List<File> {
    return System.getProperty("java.class.path")
        ?.split(File.pathSeparator)
        ?.map(::File)
        ?.filter { it.exists() }
        ?: emptyList()
}

fun defaultServerApiClassNames(): List<String> = listOf(
    "org.bukkit.Bukkit",
    "org.bukkit.entity.Player",
    "org.bukkit.event.player.PlayerMoveEvent",
    "net.md_5.bungee.api.chat.BaseComponent",
    "io.papermc.paper.event.player.AsyncChatEvent"
)

internal fun URL.toExistingFile(): File? {
    return runCatching {
        when (protocol) {
            "file" -> File(toURI())
            "jar" -> {
                val nested = file.substringBefore('!')
                when {
                    nested.startsWith("file:") -> File(URI(nested))
                    else -> File(nested)
                }
            }
            else -> null
        }
    }.getOrNull()?.takeIf { it.exists() }
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
