import java.util.Properties

plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(libs.google.adk.kotlin.core)
    implementation(libs.google.adk.kotlin.webserver)
    implementation(libs.kotlinx.coroutines)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
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
