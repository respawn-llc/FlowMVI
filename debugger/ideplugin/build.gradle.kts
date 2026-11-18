@file:Suppress("UnstableApiUsage")

import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import kotlin.text.replace

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.intellij.ide)
}

val pluginPrivateKey by lazy {
    val props by localProperties()
    props["plugin.publishing.privatekey"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
}

val pluginSigningPassword by lazy {
    val props by localProperties()
    props["signing.password"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
}

val pluginPublishingToken by lazy {
    val props by localProperties()
    props["plugin.publishing.token"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
}

repositories {
    google {
        mavenContent {
            includeGroupAndSubgroups("androidx")
            includeGroupAndSubgroups("com.android")
            includeGroupAndSubgroups("com.google")
        }
    }
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    projectName = Config.name
    // needed when plugin provides custom settings exposed to the UI
    buildSearchableOptions = false
    if (pluginPrivateKey != null && pluginSigningPassword != null) {
        signing {
            certificateChainFile = rootProject.rootDir.resolve(Config.Plugin.certPath)
            privateKey = pluginPrivateKey
            password = pluginSigningPassword
        }
    }
    publishing {
        token = pluginPublishingToken
        hidden = true
    }
    pluginVerification {
        ides {
            // TODO: https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1965
            // props["plugin.local.ide.path"]?.toString()?.let(::local)
            create(
                IntelliJPlatformType.IntellijIdea,
                libs.versions.intellij.idea.get()
            )
            create(
                IntelliJPlatformType.IntellijIdea,
                libs.versions.intellij.verification.ide.get()
            )
        }
    }
    pluginConfiguration {
        ideaVersion {
            sinceBuild = Config.Plugin.minIdeaVersion
            untilBuild = provider { null }
        }
        vendor {
            name = Config.vendorName
            email = Config.supportEmail
            url = Config.url
        }
        id = Config.Plugin.id
        description = Config.Plugin.description
        name = Config.Plugin.name
        version = Config.versionName
        changeNotes = System.getenv("CHANGELOG")?.replace("\n", "<br>")
    }
}

java {
    // Compile with the build JDK, but keep the plugin runnable on Java 21 IDEs.
    toolchain.languageVersion.set(JavaLanguageVersion.of(JavaVersion.current().majorVersion.toInt()))
}

tasks {
    withType<JavaCompile> {
        options.release.set(Config.Plugin.jvmTarget.target.toInt())
    }
    withType<KotlinJvmCompile> {
        compilerOptions {
            jvmTarget.set(Config.Plugin.jvmTarget)
            freeCompilerArgs.add("-Xjdk-release=${Config.Plugin.jvmTarget.target}")
        }
    }
    buildPlugin {
        archiveFileName = "${Config.artifact}-$version.zip"
    }
}

// https://youtrack.jetbrains.com/issue/IJPL-1901
configurations {
    implementation.configure {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    }
    api.configure {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    }
}

dependencies {
    compileOnly(compose.desktop.currentOs)
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlin.coroutines.core)

    // platform already includes bindings
    // implementation(libs.kotlin.coroutines.swing)

    implementation(libs.compose.desktop)
    implementation(libs.compose.desktop.jvm.linux.arm64)
    implementation(libs.compose.desktop.jvm.linux.x64)
    implementation(libs.compose.desktop.jvm.macos.arm64)
    implementation(libs.compose.desktop.jvm.macos.x64)
    implementation(libs.compose.desktop.jvm.windows.x64)
    implementation(libs.compose.material3)

    implementation(projects.core)
    implementation(projects.debugger.server)

    implementation(applibs.decompose)
    implementation(applibs.decompose.compose)
    implementation(applibs.bundles.koin)

    intellijPlatform {
        @Suppress("DEPRECATION") // crashes without this usage :D
        intellijIdea(libs.versions.intellij.idea)
        pluginVerifier()
        zipSigner()
        bundledPlugin(libs.kotlin.stdlib.map(Dependency::getGroup))
    }
}
