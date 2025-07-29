@file:Suppress("UnstableApiUsage")

import com.google.cloud.tools.jib.gradle.JibTask


plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.jib)
    application
    alias(libs.plugins.graalvm)
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
                nativeImageCapable.set(true)
            })
            buildArgs("--enable-url-protocols=http,https")
        }
        named("main") {
            imageName.set("climate-trace-mcp")
            mainClass.set("McpServerKt")
        }
    }
}

jib {
    from.image = "docker.io/library/alpine:3.22"

    to {
        image = "gcr.io/climatetrace-mcp/climatetrace-mcp-server"
    }
    container {
        ports = listOf("8080")
        entrypoint = listOf("/climate-trace-mcp")
    }
    extraDirectories {
        paths {
            path {
                setFrom("build/native/nativeCompile")
                into = "/"
            }
        }
    }
}

tasks.withType<JibTask> {
    dependsOn(tasks.named("nativeCompile"))
}
