plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
    dokkaDocumentation
}

kotlin {
    configureMultiplatform(
        this,
        watchOs = false,
        tvOs = false,
        linux = false,
        js = true,
        wasmJs = true,
        windows = false,
        wasmWasi = false,
    )

    sourceSets {
        androidMain.dependencies {
            implementation(libs.ktor.client.engine)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.engine)
        }
        nativeMain.dependencies {
            implementation(libs.ktor.client.engine)
        }
        webMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

android {
    namespace = "${Config.namespace}.debugger.plugin"
    configureAndroidLibrary(this)
}

dependencies {
    commonMainApi(projects.debugger.debuggerClient)

    commonMainImplementation(projects.debugger.debuggerCommon)
    commonMainImplementation(libs.bundles.ktor.client)
    commonMainImplementation(libs.bundles.serialization)
}
