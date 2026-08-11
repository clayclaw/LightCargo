package io.github.clayclaw.lightcargo.kts.environment.bukkit.classloading

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassPathIndexTest {

    @Test
    fun `classpathFilesIncludingParents collects jars from the loader chain`() {
        val parentJar = Files.createTempFile("parent", ".jar").toFile().also { it.writeBytes(byteArrayOf()) }
        val childJar = Files.createTempFile("child", ".jar").toFile().also { it.writeBytes(byteArrayOf()) }
        val parent = URLClassLoader(arrayOf(parentJar.toURI().toURL()), null)
        val child = URLClassLoader(arrayOf(childJar.toURI().toURL()), parent)

        val files = child.classpathFilesIncludingParents()

        assertEquals(
            listOf(childJar.canonicalFile, parentJar.canonicalFile),
            files.map { it.canonicalFile }
        )
    }

    @Test
    fun `toExistingFile resolves file and nested jar urls`() {
        val jar = Files.createTempFile("api", ".jar").toFile().also { it.writeBytes(byteArrayOf()) }

        assertEquals(jar.canonicalFile, jar.toURI().toURL().toExistingFile()?.canonicalFile)
        assertEquals(
            jar.canonicalFile,
            java.net.URI("jar:${jar.toURI()}!/org/bukkit/Bukkit.class").toURL().toExistingFile()?.canonicalFile
        )
    }

    @Test
    fun `serverApiClasspathFiles resolves known jdk classes from code source when available`() {
        val files = serverApiClasspathFiles(listOf("java.lang.String", "missing.Type"))

        // Bootstrap classes may not expose a file code source; ensure the call is safe.
        assertTrue(files.all { it.exists() })
    }
}
