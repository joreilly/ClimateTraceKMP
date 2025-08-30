plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(libs.mcp.kotlin)
    implementation(libs.koin.core)
    implementation(libs.koog.agents)
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

tasks.register<JavaExec>("devUi") {
    group = "application"
    description = "Start the ADK Dev UI server"
    mainClass.set("com.google.adk.web.AdkWebServer")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("--adk.agents.source-dir=src/main/java")
}

