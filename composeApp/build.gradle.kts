import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

kotlin {
    jvmToolchain(17)

//    wasmJs {
//        moduleName = "composeApp"
//        browser {
//            commonWebpackConfig {
//                outputFileName = "composeApp.js"
//            }
//        }
//        binaries.executable()
//    }


    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)

    }


    jvm("desktop")
    
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

    compilerOptions {
        languageVersion.set(KOTLIN_2_0)
    }
    
    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }


        commonMain.dependencies {

//            implementation(libs.androidx.navigation3)
//            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.androidx.lifecycle.viewmodel)

            implementation("androidx.savedstate:savedstate:1.3.0-alpha07")
            implementation("androidx.collection:collection:1.4.5")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation("org.jetbrains.compose.ui:ui-backhandler:1.8.0-alpha03")
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            api(libs.compose.adaptive)
            api(libs.compose.adaptive.layout)

            implementation(libs.molecule)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            implementation(libs.kstore)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.bundles.ktor.common)
            implementation(libs.kmpObservableViewModel)

            implementation(libs.koalaplot)
            implementation(libs.treemap.chart)
            implementation(libs.treemap.chart.compose)
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
            // workaround for https://youtrack.jetbrains.com/issue/CMP-5959/Invalid-redirect-in-window-core#focus=Comments-27-10365630.0-0
            implementation("androidx.window:window-core:1.3.0")
        }

        val desktopMain by getting

        desktopMain.dependencies {
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


        val desktopTest by getting

        // Adds the desktop test dependency
        desktopTest.dependencies {
            implementation(compose.desktop.currentOs)
        }

//        val wasmJsMain by getting
//
//        wasmJsMain.dependencies {
//            implementation(libs.kstore.storage)
//        }
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
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
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

compose.experimental {
    web.application {}
}


kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
}

configurations.configureEach {
    exclude("androidx.window.core", "window-core")
}