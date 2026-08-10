dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(kotlin("reflect"))

    listOf(
        "org.jetbrains.kotlin:kotlin-script-runtime",
        "org.jetbrains.kotlin:kotlin-scripting-common",
        "org.jetbrains.kotlin:kotlin-scripting-jvm",
        "org.jetbrains.kotlin:kotlin-scripting-jvm-host",
        "org.jetbrains.kotlin:kotlin-scripting-dependencies",
        "org.jetbrains.kotlin:kotlin-scripting-dependencies-maven",
        "org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable"
    ).forEach {
        implementation(it) {
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
            exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        }
    }

    testImplementation(kotlin("test-junit5"))
}
repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}