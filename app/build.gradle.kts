import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Semantic version (MAJOR.MINOR.PATCH). Override per-build with -PappVersionName=X.Y.Z,
// which is how the release GitHub Action derives it from the pushed `vX.Y.Z` tag.
val appVersionName: String = (project.findProperty("appVersionName") as String?) ?: "1.4.0"

fun versionCodeFromSemver(version: String): Int {
    val (major, minor, patch) = version.split(".")
        .map { it.toIntOrNull() ?: 0 }
        .let { Triple(it.getOrElse(0) { 0 }, it.getOrElse(1) { 0 }, it.getOrElse(2) { 0 }) }
    return major * 10_000 + minor * 100 + patch
}

fun gitCommitHash(): String {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "--short=10", "HEAD")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        val hash = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        hash.ifBlank { "unknown" }
    } catch (e: Exception) {
        "unknown"
    }
}

val appBuildTime: String = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
    .withZone(ZoneOffset.UTC)
    .format(Instant.now())

android {
    namespace = "pl.cukrzycowy.ollee.glycemia"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "pl.cukrzycowy.ollee.glycemia"
        minSdk = 24
        targetSdk = 36
        versionCode = versionCodeFromSemver(appVersionName)
        versionName = appVersionName

        buildConfigField("String", "GIT_COMMIT_HASH", "\"${gitCommitHash()}\"")
        buildConfigField("String", "BUILD_TIME", "\"$appBuildTime\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("KEYSTORE_FILE")
            val storePass    = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias     = System.getenv("KEY_ALIAS")
            val keyPass      = System.getenv("KEY_PASSWORD")

            if (storeFilePath != null && storePass != null && keyAlias != null && keyPass != null) {
                storeFile     = file(storeFilePath)
                storePassword = storePass
                this.keyAlias     = keyAlias
                keyPassword   = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigningConfig = signingConfigs.getByName("release")
            if (releaseSigningConfig.storeFile != null) {
                signingConfig = releaseSigningConfig
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.json:json:20231013")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}