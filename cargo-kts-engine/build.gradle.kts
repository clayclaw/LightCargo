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