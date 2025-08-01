@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.shadowPlugin)
    alias(libs.plugins.jib)
}

kotlin {
    jvm() {
        withJava()
        binaries {
            executable {
                mainClass.set("ServerKt")
            }
        }
    }

    sourceSets {
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.serialization)

            implementation("io.ktor:ktor-server-core:3.2.1")
            implementation("io.ktor:ktor-server-netty:3.2.1")
            implementation("io.ktor:ktor-server-cors:3.2.1")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.1")
            implementation("io.ktor:ktor-server-content-negotiation:3.2.1")

            implementation("ch.qos.logback:logback-classic:1.5.8")

            //implementation(projects.composeApp)
            implementation(libs.mcp.kotlin)
            implementation(projects.mcpServer)
        }
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    manifest {
        attributes["Main-Class"] = "ServerKt"
    }
}

tasks.withType<Tar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
tasks.withType<Zip> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }


jib {
    from.image = "docker.io/library/eclipse-temurin:21"

    to {
        image = "gcr.io/climatetrace-mcp/climatetrace-mcp-server"
    }
    container {
        ports = listOf("8080")
        mainClass = "ServerKt"
    }
}