plugins {
    id(libs.plugins.kotlin.multiplatform.id)
    id(libs.plugins.androidMultiplatformLibrary.id)
    alias(libs.plugins.maven.publish)
    dokkaDocumentation
}

kotlin {
    configureMultiplatform(
        ext = this,
        jvm = true,
        iOs = true,
        macOs = true,
        watchOs = false,
        tvOs = false,
        windows = false,
        linux = true,
        js = true,
        wasmJs = true,
        wasmWasi = false,
    )

    sourceSets.androidMain.dependencies {
        api(libs.kotlin.coroutines.android)
        api(libs.androidx.fragment)
        api(libs.androidx.activity)
    }
}

kotlin.android {
    namespace = "${Config.namespace}.android"
    optimization.consumerKeepRules.apply {
        publish = true
        file(Config.consumerProguardFile)
    }
}

dependencies {
    commonMainApi(projects.core)
    commonMainApi(libs.lifecycle.runtime)
    commonMainApi(libs.lifecycle.viewmodel)
}
