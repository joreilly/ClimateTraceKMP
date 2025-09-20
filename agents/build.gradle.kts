plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(libs.mcp.kotlin)
    implementation(libs.koin.core)
    //implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation(libs.google.adk)
    implementation(libs.google.adk.dev)

    implementation (libs.kotlinx.coroutines.rx3)

    // following needed for AdkWebServer (dev UI)
    implementation("org.apache.httpcomponents.client5:httpclient5:5.5")

    implementation(projects.composeApp)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// Task to execute the startDevUI() Kotlin function via a small entrypoint
tasks.register<JavaExec>("startDevUI") {
    group = "application"
    description = "Starts the ADK Dev UI by invoking startDevUI()"
    mainClass.set("adk.DevUiMainKt")
    classpath = sourceSets["main"].runtimeClasspath
}

