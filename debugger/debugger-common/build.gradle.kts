plugins {
    kotlin("multiplatform")
    id(libs.plugins.androidMultiplatformLibrary.id)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
    dokkaDocumentation
}

kotlin {
    configureMultiplatform(this,)
}

kotlin.android {
    namespace = "${Config.namespace}.debugger"
}

dependencies {
    commonMainApi(projects.core)
    commonMainApi(projects.metrics)
    commonMainImplementation(libs.kotlin.atomicfu)
    commonMainImplementation(libs.bundles.serialization)
}
