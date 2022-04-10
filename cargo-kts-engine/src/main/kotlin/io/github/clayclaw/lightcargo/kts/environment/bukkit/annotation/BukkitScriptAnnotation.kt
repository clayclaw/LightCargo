package io.github.clayclaw.lightcargo.kts.environment.bukkit.annotation

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class RequiredPlugins(vararg val plugins: String)