import com.vanniktech.maven.publish.Checksum
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.MavenPublishBasePlugin
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import nl.littlerobots.vcu.plugin.versionCatalogUpdate
import nl.littlerobots.vcu.plugin.versionSelector
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradleSubplugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.version.catalog.update)
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.atomicfu) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.benchmark) apply false
    alias(libs.plugins.intellij.ide) apply false
    // plugins already on a classpath (conventions)
    // alias(libs.plugins.androidApplication) apply false
    // alias(libs.plugins.kotlin.multiplatform) apply false
    dokkaDocumentation
}

allprojects {
    group = Config.artifactId
    version = Config.versionName
}

subprojects {
    plugins.withType<ComposeCompilerGradleSubplugin>().configureEach {
        the<ComposeCompilerGradlePluginExtension>().apply {
            stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_definitions.txt"))
            if (properties["enableComposeCompilerReports"] == "true") {
                val metricsDir = layout.buildDirectory.dir("compose_metrics")
                metricsDestination = metricsDir
                reportsDestination = metricsDir
            }
        }
    }
    plugins.withType<MavenPublishBasePlugin> {
        the<MavenPublishBaseExtension>().apply {
            val isReleaseBuild = properties["release"]?.toString().toBoolean()
            publishToMavenCentral(false)
            checksums(Checksum.MD5, Checksum.SHA1, Checksum.SHA256, Checksum.SHA512)
            excludeSignatureChecksums(false)
            configure(
                KotlinMultiplatform(
                    javadocJar = JavadocJar.Empty(),
                    sourcesJar = true,
                )
            )
            if (isReleaseBuild) signAllPublications()
            coordinates(Config.artifactId, name, Config.version(isReleaseBuild))
            pom {
                name = Config.name
                description = Config.description
                url = Config.url
                licenses {
                    license {
                        name = Config.licenseName
                        url = Config.licenseUrl
                        distribution = Config.licenseUrl
                    }
                }
                developers {
                    developer {
                        id = Config.vendorId
                        name = Config.vendorName
                        url = Config.developerUrl
                        email = Config.supportEmail
                        organizationUrl = Config.developerUrl
                    }
                }
                scm {
                    url = Config.scmUrl
                }
            }
        }
    }
    tasks {
        withType<Test>().configureEach {
            useJUnitPlatform()
            filter { isFailOnNoMatchingTests = true }
            failOnNoDiscoveredTests = false
            testLogging {
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                showStandardStreams = true
                events("failed", "standardOut", "standardError")
                showCauses = true
                showStackTraces = true
            }
        }
    }
}

dependencies {
    detektPlugins(rootProject.libs.detekt.formatting)
    detektPlugins(rootProject.libs.detekt.compose)
    projects.run {
        listOf(
            core,
            essenty,
            savedstate,
            android,
            compose,
            essenty.essentyCompose,
            test,
            debugger.debuggerClient,
            debugger.debuggerPlugin,
            metrics,
        ).forEach { dokka(it) }
    }
}

versionCatalogUpdate {
    sortByKey = true

    versionSelector { stabilityLevel(it.candidate.version) >= Config.minStabilityLevel }

    keep {
        keepUnusedVersions = true
    }

    versionCatalogs {
        create("sample") {
            catalogFile = file("sample/libs.versions.toml")
        }
    }
}

val detektSources = fileTree(layout.projectDirectory) {
    include(Config.Detekt.includedFiles)
    exclude(Config.Detekt.excludedFiles)
}

val detektLibraryConfig = layout.projectDirectory.file("detekt-libraries.yml")

detekt {
    buildUponDefaultConfig.set(true)
    parallel.set(true)
    config.setFrom(layout.projectDirectory.file(Config.Detekt.configFile))
    source.setFrom(detektSources)
}

val detektAll = tasks.register<Detekt>("detektAll") {
    description = "Run detekt on whole project"
}

subprojects {
    plugins.withType<MavenPublishBasePlugin> {
        // Whole-tree lint is syntax-only; published libraries need their compilation classpaths.
        pluginManager.apply("dev.detekt")
        dependencies.add("detektPlugins", rootProject.libs.detekt.libraries)
        extensions.configure<DetektExtension> {
            config.setFrom(detektLibraryConfig)
            buildUponDefaultConfig.set(true)
            disableDefaultRuleSets.set(true)
        }
        val libraryAnalysis = tasks.withType<Detekt>().matching {
            it.name == "detektMainJvm" || it.name == "detektMainAndroid"
        }
        detektAll.configure { dependsOn(libraryAnalysis) }
    }
}

tasks {
    withType<Detekt>().configureEach {
        setSource(detektSources)
        jvmTarget.set(Config.jvmTarget.target)
        multiPlatformEnabled.set(true)
        reports {
            checkstyle.required.set(false)
            html.required.set(true)
            sarif.required.set(true)
            markdown.required.set(false)
        }
    }

    register<Detekt>("detektFormat") {
        description = "Formats whole project."
        autoCorrect.set(true)
    }
}

rootProject.plugins.withType<YarnPlugin>().configureEach {
    rootProject.the<YarnRootExtension>().apply {
        yarnLockMismatchReport = YarnLockMismatchReport.WARNING // NONE | FAIL | FAIL_AFTER_BUILD
        reportNewYarnLock = true
        yarnLockAutoReplace = true
    }
}

gradle.taskGraph.whenReady {
    val lastTask = allTasks.lastOrNull()
    lastTask?.doLast {
        if (this.state.failure != null) return@doLast
        println("✅ TASK SUCCESSFUL. Some messages suppressed by logging.level=quiet")
    }
}
