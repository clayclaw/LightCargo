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
        val source = primaryFrame?.lineNumber
            ?.takeIf { it > 0 }
            ?.let { scriptFile.resolveSourceContext(it) }

        return buildString {
            append("Error while ").append(phase).append(" script: ").append(scriptFile.name)
            when {
                source == null -> Unit
                source.reportedLine != source.displayLine -> {
                    append(":").append(source.reportedLine)
                    append(" (mapped to line ").append(source.displayLine)
                    append(" of ").append(source.totalLines).append(")")
                }
                else -> append(":").append(source.reportedLine)
            }
            append('\n')
            append("  ").append(root.javaClass.name)
            root.message?.takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
                ?: append(" (no message)")
            if (source != null) {
                append('\n')
                append(source.render())
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

    private fun File.resolveSourceContext(reportedLine: Int): SourceContext? {
        val lines = runCatching { readLines() }.getOrNull() ?: return null
        if (lines.isEmpty()) return null

        // Kotlin script <init> frames often point one past the last statement.
        val displayLine = reportedLine.coerceIn(1, lines.size)
        val contextStart = (displayLine - 2).coerceAtLeast(1)
        val contextEnd = (displayLine + 1).coerceAtMost(lines.size)

        return SourceContext(
            reportedLine = reportedLine,
            displayLine = displayLine,
            totalLines = lines.size,
            snippet = (contextStart..contextEnd).map { lineNo ->
                SourceLine(lineNo, lines[lineNo - 1], highlight = lineNo == displayLine)
            }
        )
    }

    private data class SourceLine(
        val number: Int,
        val text: String,
        val highlight: Boolean
    )

    private data class SourceContext(
        val reportedLine: Int,
        val displayLine: Int,
        val totalLines: Int,
        val snippet: List<SourceLine>
    ) {
        fun render(): String = buildString {
            if (reportedLine > totalLines) {
                append("  note: bytecode line ")
                append(reportedLine)
                append(" is past end of file (")
                append(totalLines)
                append(" lines); showing nearby source")
                append('\n')
            }
            snippet.forEach { line ->
                append(if (line.highlight) "  > " else "    ")
                append(line.number)
                append("| ")
                append(line.text.trimEnd())
                append('\n')
            }
        }.trimEnd()
    }
}
