package io.github.clayclaw.lightcargo.kts.definition

import java.io.File
import java.util.logging.Logger

object ScriptFailureReporter {

    fun log(logger: Logger, phase: String, scriptFile: File, throwable: Throwable) {
        logger.severe(format(phase, scriptFile, throwable))
        throwable.stackTraceToString()
            .lineSequence()
            .drop(1) // summary already includes the exception header
            .forEach { logger.severe(it) }
    }

    fun format(phase: String, scriptFile: File, throwable: Throwable): String {
        val root = rootCause(throwable)
        val frames = scriptFrames(throwable, scriptFile)
        val primaryFrame = frames.firstOrNull()
        val sourceLine = primaryFrame?.lineNumber
            ?.takeIf { it > 0 }
            ?.let { scriptFile.readLineOrNull(it) }
            ?.trim()

        return buildString {
            append("Error while ").append(phase).append(" script: ").append(scriptFile.name)
            primaryFrame?.lineNumber?.takeIf { it > 0 }?.let { append(":$it") }
            append('\n')
            append("  ").append(root.javaClass.name)
            root.message?.takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
                ?: append(" (no message)")
            if (sourceLine != null) {
                append('\n')
                append("  source: ").append(sourceLine)
            }
            if (frames.isNotEmpty()) {
                append('\n')
                append("  at ")
                append(frames.joinToString("\n  at ") { it.toString() })
            }
            if (throwable !== root) {
                append('\n')
                append("  caused by chain: ")
                append(causeChain(throwable).joinToString(" -> ") { it.javaClass.simpleName })
            }
        }
    }

    private fun rootCause(throwable: Throwable): Throwable {
        var current = throwable
        while (current.cause != null && current.cause !== current) {
            current = current.cause!!
        }
        return current
    }

    private fun causeChain(throwable: Throwable): List<Throwable> {
        val chain = mutableListOf<Throwable>()
        var current: Throwable? = throwable
        while (current != null && current !in chain) {
            chain += current
            current = current.cause
        }
        return chain
    }

    private fun scriptFrames(throwable: Throwable, scriptFile: File): List<StackTraceElement> {
        val fileNames = setOf(scriptFile.name, scriptFile.nameWithoutExtension)
        return causeChain(throwable)
            .flatMap { it.stackTrace.asList() }
            .filter { frame ->
                frame.fileName != null && (
                    frame.fileName in fileNames ||
                        frame.fileName!!.endsWith(scriptFile.name) ||
                        scriptFile.name.startsWith(frame.fileName!!.substringBefore('.'))
                    )
            }
            .distinct()
    }

    private fun File.readLineOrNull(lineNumber: Int): String? {
        return runCatching {
            useLines { lines -> lines.drop(lineNumber - 1).firstOrNull() }
        }.getOrNull()
    }
}
