@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.shadowPlugin)
    alias(libs.plugins.jib)
    application
    id("org.graalvm.buildtools.native") version "0.11.0"
}

dependencies {
    implementation(libs.mcp.kotlin)
    implementation(libs.koin.core)
    implementation(projects.composeApp)
    implementation(libs.logback.classic)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
        vendor.set(JvmVendorSpec.GRAAL_VM)
        nativeImageCapable.set(true)
    }
}

application {
    mainClass = "McpServerKt"
}

graalvmNative {
    agent {
        enabled.set(true)
    }

    binaries {
        all {
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(24))
                vendor.set(JvmVendorSpec.GRAAL_VM)
                nativeImageCapable.set(true)
            })
        }
        named("main") {
            imageName.set("climate-trace-mcp")
            mainClass.set("MainKt")
        }
    }
}

jib {
    from.image = "docker.io/library/eclipse-temurin:21"

    to {
        image = "gcr.io/climatetrace-mcp/climatetrace-mcp-server"
    }
    container {
        ports = listOf("8080")
        mainClass = "McpServerKt"
    }
}
