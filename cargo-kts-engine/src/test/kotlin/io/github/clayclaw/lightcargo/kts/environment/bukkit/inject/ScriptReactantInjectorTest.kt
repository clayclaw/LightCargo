package io.github.clayclaw.lightcargo.kts.environment.bukkit.inject

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScriptReactantInjectorTest {

    @Test
    fun `empty injection name matches wildcard provide patterns`() {
        assertTrue(ScriptReactantInjector.nameMatches("", ".*"))
        assertTrue(ScriptReactantInjector.nameMatches("any", ".*"))
        assertFalse(ScriptReactantInjector.nameMatches("other", "exact"))
        assertTrue(ScriptReactantInjector.nameMatches("exact", "exact"))
    }
}
