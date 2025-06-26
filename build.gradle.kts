plugins {
    kotlin("jvm") version "1.9.23" apply false
    id("io.ktor.plugin") version "2.3.10" apply false
}

allprojects {
    group = "com.sporttracker"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    }
} 