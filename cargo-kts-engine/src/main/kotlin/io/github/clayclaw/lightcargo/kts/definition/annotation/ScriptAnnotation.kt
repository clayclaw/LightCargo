package io.github.clayclaw.lightcargo.kts.definition.annotation

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Import(vararg val scriptPaths: String)