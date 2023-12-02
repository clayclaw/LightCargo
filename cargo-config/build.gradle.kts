import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
}
dependencies {
    compileOnly("com.google.code.gson:gson:2.9.0")
    compileOnly("org.yaml:snakeyaml:1.30")
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}