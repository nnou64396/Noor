import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * Release signing credentials.
 *
 * Read from environment variables, falling back to the untracked
 * `local.properties`, so nothing sensitive ever enters version control. No
 * keystore and no password is committed to this repository; see RELEASE.md for
 * the external setup a release owner has to perform once.
 *
 * When no credentials are configured the release build still succeeds and simply
 * produces an unsigned artifact, which keeps `assembleRelease` usable for
 * anyone who clones the repository. Supplying only part of the set, or pointing
 * at a keystore that is not there, is treated as a misconfiguration and fails
 * the build rather than silently emitting an unsigned release.
 */
val untrackedLocalProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun releaseSecret(name: String): String? =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
        ?: untrackedLocalProperties.getProperty(name)?.takeIf { it.isNotBlank() }

val releaseStoreFile = releaseSecret("NOOR_RELEASE_STORE_FILE")
val releaseStorePassword = releaseSecret("NOOR_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSecret("NOOR_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSecret("NOOR_RELEASE_KEY_PASSWORD")

val releaseSecrets = listOf(
    "NOOR_RELEASE_STORE_FILE" to releaseStoreFile,
    "NOOR_RELEASE_STORE_PASSWORD" to releaseStorePassword,
    "NOOR_RELEASE_KEY_ALIAS" to releaseKeyAlias,
    "NOOR_RELEASE_KEY_PASSWORD" to releaseKeyPassword,
)

val hasReleaseSigning = releaseSecrets.all { (_, value) -> value != null }

if (releaseSecrets.any { (_, value) -> value != null } && !hasReleaseSigning) {
    // Deliberately names only the absent keys, never any configured value.
    val missing = releaseSecrets.filter { (_, value) -> value == null }.map { (name, _) -> name }
    throw GradleException(
        "Release signing is partially configured. Missing: ${missing.joinToString()}. " +
            "Configure all four, or none of them to build unsigned. See RELEASE.md.",
    )
}

android {
    namespace = "com.noor.islamic"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.noor.islamic"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                val storePath = rootProject.file(requireNotNull(releaseStoreFile))
                if (!storePath.isFile) {
                    throw GradleException(
                        "NOOR_RELEASE_STORE_FILE does not point at a readable file. " +
                            "Check the path without committing the keystore. See RELEASE.md.",
                    )
                }
                storeFile = storePath
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // Null when no credentials are configured, which keeps the release
            // build working and yields an unsigned artifact.
            signingConfig = signingConfigs.findByName("release")

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        // Lint errors must fail the build; a release cannot ship with them
        // silenced. Warnings stay non-fatal so pre-existing advisory findings
        // (dependency updates, plurals) do not block a release.
        abortOnError = true
        warningsAsErrors = false
        // lintVitalRelease already runs as part of assembleRelease; being
        // explicit keeps that guarantee visible.
        checkReleaseBuilds = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.com.google.android.material)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Data
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Quran offline reader (pure-Kotlin shared normalizer keeps search rules identical at build + query time)
    implementation(project(":tools:quran:common"))

    // Quran audio (Media3/ExoPlayer). Recitation streams from the network and is kept
    // strictly separate from the offline Quran text database and importer pipeline.
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Location
    implementation(libs.play.services.location)

    // Imaging
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    // Instrumented tests
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.compose.ui.test.junit4)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}