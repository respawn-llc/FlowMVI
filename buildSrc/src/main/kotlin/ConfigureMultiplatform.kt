@file:Suppress("MissingPackageDeclaration", "unused", "UndocumentedPublicFunction", "LongMethod", "UnusedImports")

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
fun Project.configureMultiplatform(
    ext: KotlinMultiplatformExtension,
    jvm: Boolean = true,
    linux: Boolean = true,
    iOs: Boolean = true,
    js: Boolean = true,
    tvOs: Boolean = true,
    macOs: Boolean = true,
    watchOs: Boolean = true,
    windows: Boolean = true,
    wasmJs: Boolean = true,
    wasmWasi: Boolean = true,
    explicitApi: Boolean = true,
    configure: KotlinHierarchyBuilder.Root.() -> Unit = {},
) = ext.apply {
    val libs by versionCatalog
    if (explicitApi) explicitApi()
    applyDefaultHierarchyTemplate(configure)
    withSourcesJar(true)
    compilerOptions {
        extraWarnings.set(true)
        freeCompilerArgs.addAll(Config.compilerArgs)
        optIn.addAll(Config.optIns)
        progressiveMode.set(true)
    }

    if (linux) {
        linuxX64()
        linuxArm64()
    }

    if (windows) mingwX64()

    if (js) js(IR) {
        browser()
        nodejs()
        binaries.library()
        binaries.configureEach {
            if (mode == KotlinJsBinaryMode.PRODUCTION) {
                // This flag only affects the JS linker, despite being accepted by the Wasm CLI.
                linkTask.configure {
                    compilerOptions.freeCompilerArgs.add("-Xenable-advanced-optimizations")
                }
            }
        }
    }

    if (wasmJs) wasmJs {
        outputModuleName.set(this@configureMultiplatform.name)
        nodejs()
        browser()
        binaries.library()
        compilerOptions {
            freeCompilerArgs.addAll(Config.wasmCompilerArgs)
        }
    }

    if (wasmWasi) wasmWasi {
        nodejs()
    }

    targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach {
        compileSdk(Config.compileSdk)
        minSdk = Config.minSdk
        lint.warning.add("AutoboxingStateCreation")
        withHostTest {
            isReturnDefaultValues = true
        }
        packaging.resources.excludes.addAll(
            listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "DebugProbesKt.bin",
                "META-INF/versions/9/previous-compilation-data.bin",
            )
        )
        compilerOptions {
            jvmTarget.set(Config.jvmTarget)
            freeCompilerArgs.addAll(Config.jvmCompilerArgs)
        }
    }

    if (jvm) jvm {
        compilerOptions {
            jvmTarget.set(Config.jvmTarget)
            freeCompilerArgs.addAll(Config.jvmCompilerArgs)
        }
    }

    sequence {
        if (iOs) {
            yield(iosArm64())
            yield(iosSimulatorArm64())
        }
        if (macOs) {
            yield(macosArm64())
        }
        if (tvOs) {
            yield(tvosArm64())
            yield(tvosSimulatorArm64())
        }
        if (watchOs) {
            yield(watchosArm64())
            yield(watchosDeviceArm64())
            yield(watchosSimulatorArm64())
        }
    }.toList() // for now, do nothing, but iterate the lazy sequence

    sourceSets.apply {
        if (jvm) {
            val jvmTest by getting {
                dependencies {
                    implementation(libs.requireLib("kotest-junit"))
                }
            }
        }
        all {
            languageSettings {
                progressiveMode = true
                Config.optIns.forEach { optIn(it) }
            }
        }
    }
}
