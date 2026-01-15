@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import java.util.Properties
import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kmpNativeCoroutines)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    jvmToolchain(17)

    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }


    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }


    jvm()
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.molecule)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            implementation(libs.kstore)

            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.datetime)

            implementation(libs.bundles.ktor.common)

            implementation(libs.voyager)

            implementation(libs.kmpObservableViewModel)

            implementation(libs.koalaplot)
            implementation(libs.treemap.chart)
            implementation(libs.treemap.chart.compose)
            implementation("dev.carlsen.flagkit:flagkit:1.1.0")
            api(libs.compose.adaptive)
            api(libs.compose.adaptive.layout)

            implementation(libs.markdown.renderer)

            implementation(libs.koog.agents)
            implementation(libs.koog.agents.features.opentelemetry)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))

            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }

        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.kstore.file)
            implementation(libs.ktor.client.android)
        }

        jvmMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:${libs.versions.kotlinx.coroutines}")
            implementation(compose.desktop.currentOs)
            implementation(libs.harawata.appdirs)
            implementation(libs.kstore.file)
            implementation(libs.ktor.client.java)
        }

        appleMain.dependencies {
            implementation(libs.kstore.file)
            implementation(libs.ktor.client.darwin)
        }


        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }

        val wasmJsMain by getting

        wasmJsMain.dependencies {
            implementation(libs.kstore.storage)
        }
    }
}

android {
    namespace = "dev.johnoreilly.climatetrace"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "dev.johnoreilly.climatetrace"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            all {
                it.exclude("**/screen/**")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "dev.johnoreilly.climatetrace"
            packageVersion = "1.0.0"
        }
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
}

configurations.all {
    // FIXME exclude netty from Koog dependencies?
    exclude(group = "io.netty", module = "*")
}

buildkonfig {
    packageName = "dev.johnoreilly.climatetrace"

    val localPropsFile = rootProject.file("local.properties")
    val localProperties = Properties()
    if (localPropsFile.exists()) {
        runCatching {
            localProperties.load(localPropsFile.inputStream())
        }.getOrElse {
            it.printStackTrace()
        }
    }
    defaultConfigs {
        buildConfigField(
            FieldSpec.Type.STRING,
            "GEMINI_API_KEY",
            localProperties["gemini_api_key"]?.toString() ?: ""
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "OPENAI_API_KEY",
            localProperties["openai_api_key"]?.toString() ?: ""
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "OPENROUTER_API_KEY",
            localProperties["openrouter_api_key"]?.toString() ?: ""
        )
    }

}

