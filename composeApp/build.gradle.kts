plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "composeApp"
            isStatic = true
            // Export shared module so Swift can access LLMBridge and other iOS-specific types
            export(projects.shared)
        }
    }

    // Configure intermediate source sets
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            // Use api() for shared module so it can be exported in iOS framework
            api(projects.shared)

            // Compose
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.components.resources)

            // Lifecycle & ViewModel
            implementation(libs.bundles.lifecycle)

            // Navigation
            implementation(libs.navigation.compose)

            // DI
            implementation(libs.bundles.koin.common)

            // Images
            implementation(libs.bundles.coil)

            // Coroutines
            implementation(libs.coroutines.core)

            // DateTime
            implementation(libs.datetime)

            // Logging
            implementation(libs.kermit)

            // Icons (Tabler Icons - MIT License, 4985+ icons)
            implementation("br.com.devsrsouza.compose.icons:tabler-icons:1.1.1")

            // Charts (KoalaPlot - Compose Multiplatform charting library)
            implementation("io.github.koalaplot:koalaplot-core:0.10.4")

            // File Picker
            implementation(libs.filekit.core)
            implementation(libs.filekit.compose)
        }

        commonTest.dependencies {
            implementation(libs.bundles.testing)
            implementation(libs.datetime)
        }

        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.coroutines.android)
            implementation(libs.koin.android)
        }

        iosMain.dependencies {
            // iOS-specific compose dependencies if needed
        }
    }
}

android {
    namespace = "com.finuts.composeapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

detekt {
    config.setFrom("${rootDir}/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    source.setFrom(
        "src/commonMain/kotlin",
        "src/androidMain/kotlin",
        "src/iosMain/kotlin"
    )
}
