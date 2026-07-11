import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

/**
 * Release signing values are read from (in priority order):
 *   1. Environment variables (used by CI):
 *      ANDROID_KEYSTORE_FILE, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD
 *   2. A local, git-ignored keystore.properties file at the project root:
 *      storeFile, storePassword, keyAlias, keyPassword
 *
 * The build never silently falls back to the debug key for release artifacts.
 */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

fun signingValue(envName: String, propName: String): String? {
    val env = System.getenv(envName)
    if (!env.isNullOrBlank()) return env
    val prop = keystoreProperties.getProperty(propName)
    if (!prop.isNullOrBlank()) return prop
    return null
}

android {
    namespace = "com.pawmino.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pawmino.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingValue("ANDROID_KEYSTORE_FILE", "storeFile")
            val storePass = signingValue("ANDROID_KEYSTORE_PASSWORD", "storePassword")
            val alias = signingValue("ANDROID_KEY_ALIAS", "keyAlias")
            val keyPass = signingValue("ANDROID_KEY_PASSWORD", "keyPassword")

            if (storeFilePath != null && storePass != null && alias != null && keyPass != null) {
                storeFile = file(storeFilePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
            // If values are missing, this config stays incomplete. The release build
            // type below fails clearly rather than silently using the debug key.
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        getByName("release") {
            // Stage 1: keep these false, verify a working release build, then flip to true.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            } else {
                // Do not fall back to debug signing. Fail clearly at configuration time
                // when a real release build is requested without credentials.
                gradle.taskGraph.whenReady {
                    val buildingRelease = allTasks.any {
                        it.name.contains("Release", ignoreCase = true) &&
                            (it.name.startsWith("assemble") || it.name.startsWith("bundle"))
                    }
                    if (buildingRelease) {
                        throw GradleException(
                            "Release signing credentials are missing. Provide either the " +
                                "ANDROID_KEYSTORE_FILE / ANDROID_KEYSTORE_PASSWORD / ANDROID_KEY_ALIAS / " +
                                "ANDROID_KEY_PASSWORD environment variables (CI) or a keystore.properties " +
                                "file with storeFile/storePassword/keyAlias/keyPassword (local)."
                        )
                    }
                }
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.ui.tooling)
}
