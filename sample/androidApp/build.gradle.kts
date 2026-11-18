import com.android.build.api.variant.HostTestBuilder

plugins {
    id(applibs.plugins.android.application.id)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = Config.Sample.namespace
    compileSdk = Config.compileSdk

    defaultConfig {
        applicationId = Config.artifactId
        minSdk = Config.appMinSdk
        targetSdk = Config.targetSdk
        versionCode = Config.versionCode
        versionName = Config.versionName
        testInstrumentationRunner = Config.testRunner
        proguardFiles(getDefaultProguardFile(Config.defaultProguardFile))
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = Config.javaVersion
        targetCompatibility = Config.javaVersion
    }

    lint {
        warning += "AutoboxingStateCreation"
    }

    packaging {
        resources.excludes += listOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "DebugProbesKt.bin",
            "META-INF/versions/9/previous-compilation-data.bin"
        )
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.maxHeapSize = "1G"
                it.forkEvery = 100
                it.jvmArgs("-Xmx1g", "-Xms512m")
            }
        }
    }

    val passwd by lazy {
        val props by localProperties()
        props["keystore.password"]?.toString()?.trim()?.takeIf { it.isNotBlank() }
    }
    val keystore by lazy { rootDir.resolve("certificates/keystore.jks") }
    val signed by lazy { passwd != null && keystore.exists() }
    signingConfigs {
        if (signed) create("release") {
            keyAlias = "key"
            keyPassword = passwd
            storeFile = keystore
            storePassword = passwd
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
            versionNameSuffix = "-debug"
            isMinifyEnabled = Config.isMinifyEnabledDebug
            isShrinkResources = Config.isMinifyEnabledDebug
        }
        release {
            ndk.debugSymbolLevel = "FULL"
            isShrinkResources = true
            isMinifyEnabled = true
            signingConfig = if (signed) signingConfigs.getByName("release") else null
        }
    }

    androidResources {
        generateLocaleConfig = false
    }
}

androidComponents {
    beforeVariants(selector().withBuildType("release")) { variant ->
        // AGP's Compose Preview tasks require a host unit-test component to package resources.
        variant.hostTests.getValue(HostTestBuilder.UNIT_TEST_TYPE).enable = true
    }
    onVariants { variant ->
        variant.outputs.forEach { it.outputFileName.set("${Config.Sample.namespace}.apk") }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = Config.jvmTarget
        freeCompilerArgs.addAll(Config.jvmCompilerArgs)
        optIn.addAll(Config.optIns)
        progressiveMode = true
    }
}

dependencies {
    implementation(projects.sample)
    implementation(projects.android)
    implementation(projects.core)
    implementation(projects.metrics)

    implementation(libs.compose.activity)
    implementation(libs.compose.runtime)
    implementation(applibs.androidx.splashscreen)
    implementation(applibs.decompose)
    implementation(applibs.kmputils.common)
    implementation(applibs.koin.android)
    implementation(applibs.view.constraintlayout)
    implementation(applibs.view.material)

    debugImplementation(projects.debugger.debuggerPlugin)
}
