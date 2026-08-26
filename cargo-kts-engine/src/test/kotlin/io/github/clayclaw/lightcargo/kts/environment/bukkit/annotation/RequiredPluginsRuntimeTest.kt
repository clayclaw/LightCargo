package io.github.clayclaw.lightcargo.kts.environment.bukkit.annotation

import kotlin.test.Test
import kotlin.test.assertEquals

class RequiredPluginsRuntimeTest {

    @Test
    fun `parseRequiredPluginNames reads single and repeated file annotations`() {
        val script = """
            @file:RequiredPlugins("AuroraCore")
            @file:RequiredPlugins("Vault", "PlaceholderAPI")

            import net.mythofaurora.mob.definition.MobDefinitionRegistry

            fun onLoad() {}
        """.trimIndent()

        assertEquals(
            listOf("AuroraCore", "Vault", "PlaceholderAPI"),
            parseRequiredPluginNames(script)
        )
    }

    @Test
    fun `parseRequiredPluginNames ignores unrelated annotations and deduplicates`() {
        val script = """
            @file:DependsOn("org.example:lib:1.0")
            @file:RequiredPlugins("AuroraCore")
            @file:RequiredPlugins("AuroraCore")

            fun onLoad() {}
        """.trimIndent()

        assertEquals(listOf("AuroraCore"), parseRequiredPluginNames(script))
    }

    @Test
    fun `parseRequiredPluginNames returns empty when annotation is absent`() {
        assertEquals(emptyList(), parseRequiredPluginNames("fun onLoad() {}"))
    }
}
