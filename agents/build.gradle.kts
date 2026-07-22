plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.mcp.kotlin)
    implementation(libs.koin.core)
    //implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation(libs.google.adk.kotlin.core)
    implementation(libs.google.adk.kotlin.webserver)
    implementation(libs.opentelemetry.sdk)
    ksp(libs.google.adk.kotlin.processor)

    implementation(projects.composeApp)
}

java {
    toolchain {
        // Must match composeApp's jvmToolchain(24): agents depends on composeApp's compiled
        // classes at runtime, and a lower toolchain here can't load its class files.
        languageVersion = JavaLanguageVersion.of(24)
    }
}

// Task to execute the startDevUI() Kotlin function via a small entrypoint
tasks.register<JavaExec>("startDevUI") {
    group = "application"
    description = "Starts the ADK Dev UI by invoking startDevUI()"
    mainClass.set("adk.DevUiMainKt")
    classpath = sourceSets["main"].runtimeClasspath
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}

