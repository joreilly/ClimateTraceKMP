plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

dependencies {
    implementation(libs.mcp.kotlin)
    implementation(libs.koin.core)
    implementation("ai.koog:koog-agents:0.2.1")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation(projects.composeApp)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = "MainKt"
}


