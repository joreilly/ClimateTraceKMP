pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Kotlin dev builds (2.5.0-Beta2-*) for Swift Export; drop once Beta2 is on Maven Central
        maven("https://packages.jetbrains.team/maven/p/kt/dev")
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Kotlin dev builds (2.5.0-Beta2-*) for Swift Export; drop once Beta2 is on Maven Central
        maven("https://packages.jetbrains.team/maven/p/kt/dev")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "ClimateTraceKMP"
include(":composeApp")
include(":ios-export")
include(":androidApp")
include(":mcp-server")
include(":agents")
