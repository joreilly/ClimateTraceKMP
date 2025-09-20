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
        maven(url = "https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public") {
            mavenContent {
                includeGroupAndSubgroups("ai.koog")
            }
        }
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public") {
            mavenContent {
                includeGroupAndSubgroups("ai.koog")
            }
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "ClimateTraceKMP"
include(":composeApp")
include(":mcp-server")
include(":agents")
