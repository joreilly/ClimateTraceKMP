import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jib) apply false
}

// ktor-client-core's wasm-js klib pins ws 8.20.1 (GHSA-96hv-2xvq-fx4p, fixed in 8.21.0).
// Drop this once a Ktor release ships a patched ws.
plugins.withType<WasmYarnPlugin> {
    the<WasmYarnRootExtension>().resolution("ws", "8.22.0")
}
