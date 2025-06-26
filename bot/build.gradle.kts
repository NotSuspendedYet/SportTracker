plugins {
    id("io.ktor.plugin")
    kotlin("jvm")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation("io.ktor:ktor-server-core-jvm:2.3.10")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.10")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.10")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.10")
    implementation("com.github.kotlin-telegram-bot:kotlin-telegram-bot:6.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.14")
} 