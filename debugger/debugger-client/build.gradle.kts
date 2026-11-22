plugins {
    kotlin("multiplatform")
    id(libs.plugins.androidMultiplatformLibrary.id)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
    dokkaDocumentation
}
kotlin {
    configureMultiplatform(
        this,
        // not supported by all needed ktor artifacts?
        watchOs = false,
        wasmJs = true,
        wasmWasi = false,
    )
}
kotlin.android {
    namespace = "${Config.namespace}.debugger.client"
}

dependencies {
    commonMainApi(projects.core)

    commonMainImplementation(projects.debugger.debuggerCommon)
    commonMainImplementation(libs.kotlin.atomicfu)
    commonMainImplementation(libs.bundles.ktor.client)
    commonMainImplementation(libs.bundles.serialization)
}
