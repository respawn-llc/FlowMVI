import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    id(libs.plugins.androidMultiplatformLibrary.id)
    alias(libs.plugins.maven.publish)
    dokkaDocumentation
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    configureMultiplatform(
        ext = this,
        tvOs = false,
        watchOs = false,
        linux = false,
        windows = false,
        wasmWasi = false,
    )
}

kotlin.android {
    namespace = "${Config.namespace}.essenty"
}

dependencies {
    commonMainApi(projects.core)
    commonMainApi(projects.savedstate)

    commonMainApi(libs.lifecycle.runtime)
    commonMainApi(libs.essenty.lifecycle)
    commonMainApi(libs.essenty.instancekeeper)
    commonMainApi(libs.essenty.statekeeper)
    commonMainApi(libs.essenty.lifecycle.coroutines)
}
