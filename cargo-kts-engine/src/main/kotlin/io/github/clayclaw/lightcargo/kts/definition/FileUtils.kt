package io.github.clayclaw.lightcargo.kts.definition

import java.io.File

fun File.isKotlinScript() = this.isFile && this.extension == "kts"

fun File.discoverAllScriptRecursively(suffix: String = "kts"): Sequence<File> {
    require(this.isDirectory) { "File $absolutePath must be a directory" }
    return walkTopDown().filter { it.name.endsWith(suffix) }
}