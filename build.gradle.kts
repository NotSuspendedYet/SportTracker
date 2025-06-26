plugins {
    kotlin("jvm") version "1.9.23" apply false
    id("io.ktor.plugin") version "2.3.10" apply false
}

allprojects {
    group = "com.sporttracker"
    version = "0.1.0"

    repositories {
        mavenCentral()
        maven { url "https://jitpack.io" }
    }
} 