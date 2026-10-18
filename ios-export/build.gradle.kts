@file:OptIn(ExperimentalSwiftExportDsl::class)

import org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportVisibility
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl

// Umbrella for Swift Export's `export { swift { } }` DSL: :composeApp (an `api` dep here) is
// exported, along with whatever :composeApp exposes via `api`. Libraries Swift doesn't use directly
// are HIDDEN, which cuts them down to stubs for the few types :composeApp's API mentions. A function
// returning a hidden type is generated as a fatalError() stub with no warning, so check the
// generated Swift.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(projects.composeApp)
        }
    }

    export {
        swift {
            moduleName = "Shared"
            xcodeIntegration {
                configure(projects.composeApp) {
                    moduleName = "ComposeApp"
                    rootPackage = "dev.johnoreilly.climatetrace"
                }
                listOf(
                    "io.insert-koin:koin-core",
                    "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel",
                ).forEach { configure(it, SwiftExportVisibility.HIDDEN) }
            }
        }
    }
}
