import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
}
dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-common")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-jvm")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-jvm-host")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-dependencies")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable")

    // runtimeOnly("org.jetbrains.kotlin:kotlin-script-runtime")
}
repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "17"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "17"
}