package io.github.clayclaw.lightcargo.kts.definition

val javaImports = listOf(
    "java.util.*",
    "java.util.concurrent.*",
    "java.io.*",
)

val kotlinCoroutinesImports = listOf(
    "kotlinx.coroutines.*",
    "kotlinx.coroutines.flow.*",
    "kotlinx.coroutines.channels.*",
    "kotlinx.coroutines.selects.*",
)

val annotationsImports = listOf(
    "kotlin.script.experimental.dependencies.DependsOn",
    "kotlin.script.experimental.dependencies.Repository",
    "io.github.clayclaw.lightcargo.kts.definition.annotation.*"
)