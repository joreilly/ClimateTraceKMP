plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.shadowPlugin)
    alias(libs.plugins.jib)
    application
}

dependencies {
    implementation(libs.ktor.client.java)
    implementation(libs.mcp.kotlin)
    implementation(libs.koin.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.sse)
//    implementation("ch.qos.logback:logback-classic:1.5.8")
    implementation(projects.composeApp)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = "McpServerKt"
}

tasks.shadowJar {
    archiveFileName.set("serverAll.jar")
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "McpServerKt"
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
