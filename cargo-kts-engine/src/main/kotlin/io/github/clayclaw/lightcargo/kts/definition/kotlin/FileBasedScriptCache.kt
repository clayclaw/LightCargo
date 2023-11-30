package io.github.clayclaw.lightcargo.kts.definition.kotlin

/**
 * Code from kotlin scripting cache test
 * https://github.com/JetBrains/kotlin/blob/master/libraries/scripting/jvm-host-test/test/kotlin/script/experimental/jvmhost/test/CachingTest.kt
 */

import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.MessageDigest
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.jvm.CompiledJvmScriptsCache
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript

class FileBasedScriptCache(
    private val baseDir: File
): CompiledJvmScriptsCache {
    override fun get(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): CompiledScript? {
        val file = File(baseDir, uniqueScriptHash(script, scriptCompilationConfiguration))
        return if (!file.exists()) null else file.readCompiledScript()
    }

    override fun store(
        compiledScript: CompiledScript,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ) {
        val file = File(baseDir, uniqueScriptHash(script, scriptCompilationConfiguration))
        file.outputStream().use { fs ->
            ObjectOutputStream(fs).use { os ->
                os.writeObject(compiledScript)
            }
        }
    }

}

internal fun uniqueScriptHash(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): String {
    val digestWrapper = MessageDigest.getInstance("MD5")
    digestWrapper.update(script.text.toByteArray())
    scriptCompilationConfiguration.notTransientData.entries
        .sortedBy { it.key.name }
        .forEach {
            digestWrapper.update(it.key.name.toByteArray())
            digestWrapper.update(it.value.toString().toByteArray())
        }
    return digestWrapper.digest().toHexString()
}

private fun File.readCompiledScript(): CompiledScript {
    return inputStream().use { fs ->
        ObjectInputStream(fs).use {
            it.readObject() as KJvmCompiledScript
        }
    }
}

private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })