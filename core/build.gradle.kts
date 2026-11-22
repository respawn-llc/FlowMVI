@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("pro.respawn.shared-library")
    alias(libs.plugins.atomicfu)
    alias(libs.plugins.maven.publish)

    // TODO: https://github.com/kotest/kotest/issues/3598
    // alias(libs.plugins.kotest)
    dokkaDocumentation
}

atomicfu {
    dependenciesVersion = libs.versions.kotlinx.atomicfu.get()
    transformJvm = true
    jvmVariant = "VH"
}

kotlin.compilerOptions.freeCompilerArgs.add("-Xdont-warn-on-error-suppression")

kotlin.android {
    namespace = Config.namespace
}

dependencies {
    commonMainApi(libs.kotlin.coroutines.core)
    commonMainImplementation(libs.kotlin.atomicfu)

    commonTestImplementation(libs.bundles.unittest)
    commonTestImplementation(projects.test)
}
