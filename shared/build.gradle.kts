plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
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
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Coroutines
            implementation(libs.coroutines.core)

            // Serialization
            implementation(libs.serialization.json)

            // DateTime
            implementation(libs.datetime)

            // DI
            implementation(libs.koin.core)

            // Network
            implementation(libs.bundles.ktor.common)

            // Database
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)

            // DataStore
            implementation(libs.datastore.preferences)
            implementation(libs.datastore.core.okio)

            // Logging
            implementation(libs.kermit)

            // AI SDKs
            implementation(libs.openai.client)
            implementation(libs.anthropic.client)
        }

        commonTest.dependencies {
            implementation(libs.bundles.testing)
            implementation(libs.datetime)
        }

        androidMain.dependencies {
            implementation(libs.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.sqlcipher.android)
            // OCR: Tesseract4Android (OpenMP variant for performance)
            implementation(libs.tesseract.android)
        }

        // iOS source set hierarchy
        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    }
}

android {
    namespace = "com.finuts.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*_Impl",
                    "*_Factory",
                    "*.BuildConfig",
                    "*.Fake*"
                )
            }
        }
        verify {
            rule {
                minBound(65)
            }
        }
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
