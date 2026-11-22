import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.serialization)
    id(libs.plugins.kotlin.multiplatform.id)
    id(libs.plugins.androidMultiplatformLibrary.id)
    alias(libs.plugins.maven.publish)
    dokkaDocumentation
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    configureMultiplatform(this, wasmWasi = false) {
        common {
            group("nonBrowser") {
                withJvm()
                withNative()
                withCompilations { it.target is KotlinMultiplatformAndroidLibraryTarget }
            }
            group("browser") {
                withWasmJs()
                withJs()
            }
        }
    }

    sourceSets {
        nativeMain.dependencies {
            implementation(libs.kotlin.io)
        }
        androidMain.dependencies {
            api(libs.lifecycle.savedstate)
        }
        commonMain.dependencies {
            api(projects.core)
            api(libs.kotlin.serialization.json)
            implementation(libs.kotlin.atomicfu)
            implementation(libs.kotlin.io)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlin.browser)
        }
        jvmTest.dependencies {
            implementation(libs.bundles.unittest)
            implementation(projects.test)
        }
    }
}

kotlin.android {
    namespace = "${Config.namespace}.savedstate"
}
