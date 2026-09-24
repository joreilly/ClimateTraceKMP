import java.util.Properties

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(libs.google.adk.kotlin.core)
    implementation(libs.google.adk.kotlin.webserver)
    implementation(libs.kotlinx.coroutines)

    // AG-UI protocol types, shared with the client SDK (com.ag-ui.community:kotlin-client).
    implementation(libs.agui.kotlin.core)
    implementation(libs.ktor2.server.core)
    implementation(libs.ktor2.server.netty)
}

java {
    toolchain {
        // Was 17, but the AG-UI JVM artifacts are compiled to class file version 65 (Java 21)
        // despite the SDK documenting a Java 11 minimum. 24 matches composeApp and CI.
        languageVersion = JavaLanguageVersion.of(24)
    }
}

// Reuse the same local.properties-sourced key composeApp exposes via BuildKonfig (see its
// buildkonfig {} block). This module isn't part of the KMP graph so it reads local.properties
// directly and hands it to the GenAI SDK via GEMINI_API_KEY, which Gemini() falls back to when no
// apiKey is passed explicitly.
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        runCatching { load(localPropsFile.inputStream()) }.getOrElse { it.printStackTrace() }
    }
}
val geminiApiKey = localProperties["gemini_api_key"]?.toString().orEmpty()

tasks.withType<JavaExec>().configureEach {
    environment("GEMINI_API_KEY", geminiApiKey)
}

// Runs the Kotlin ADK console agent, e.g.:
//   ./gradlew :agents:runAdkAgent
tasks.register<JavaExec>("runAdkAgent") {
    group = "application"
    description = "Runs the Kotlin ADK console agent"
    mainClass.set("adk.AdkAgentKt")
    classpath = sourceSets["main"].runtimeClasspath
}

// Starts the ADK Dev UI, e.g.:
//   ./gradlew :agents:startDevUI
tasks.register<JavaExec>("startDevUI") {
    group = "application"
    description = "Starts the ADK Dev UI"
    mainClass.set("adk.DevUiMainKt")
    classpath = sourceSets["main"].runtimeClasspath
}

// Serves the same agent over the AG-UI protocol, e.g.:
//   ./gradlew :agents:startAgUiServer
tasks.register<JavaExec>("startAgUiServer") {
    group = "application"
    description = "Starts the AG-UI server exposing the ClimateTrace agent"
    mainClass.set("agui.AgUiServerKt")
    classpath = sourceSets["main"].runtimeClasspath
}
