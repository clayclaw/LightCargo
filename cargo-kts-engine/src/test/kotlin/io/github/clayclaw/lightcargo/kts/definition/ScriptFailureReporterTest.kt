package io.github.clayclaw.lightcargo.kts.definition

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class ScriptFailureReporterTest {

    @Test
    fun `format includes script line and source snippet`() {
        val script = Files.createTempFile("demo", ".lc.kts").toFile()
        script.writeText(
            """
            val disposables = CompositeDisposable()
            val eventService = injectContainer<EventService>()!!
            """.trimIndent()
        )

        val error = NullPointerException().apply {
            stackTrace = arrayOf(
                StackTraceElement("Demo_lc", "<init>", script.name, 2),
                StackTraceElement("other.Type", "run", "Other.kt", 10)
            )
        }

        val message = ScriptFailureReporter.format("evaluating", script, error)

        assertContains(message, "Error while evaluating script: ${script.name}:2")
        assertContains(message, "NullPointerException (no message)")
        assertContains(message, "source: val eventService = injectContainer<EventService>()!!")
        assertContains(message, "Demo_lc.<init>(${script.name}:2)")
        assertTrue("Other.kt" !in message.split("at ").drop(1).first())
    }
}
