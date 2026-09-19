import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Local (untracked) configuration such as Supabase URL / anon key.
// Read from local.properties only (gitignored), so secrets are never committed.
val localProps = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use { load(it) }
}

fun secret(name: String): String = localProps.getProperty(name) ?: ""

// Release (upload) signing credentials. Read from the gitignored
// keystore.properties so secrets are never committed. When the file is
// absent (e.g. a fresh clone without signing access), release stays
// unsigned and a warning is emitted instead of failing the build.
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.techvisiondz.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.techvisiondz.app"
        minSdk = 26
        targetSdk = 37
        // versionCode is the authoritative update ordering key. versionName is
        // display-only and normalized to match the v1.0.0 GitHub release.
        versionCode = 4
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Public client configuration. These are NOT secret credentials and are
        // overridden per environment below.
        buildConfigField("String", "SUPABASE_URL", "\"${secret("TECHVISION_SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secret("TECHVISION_SUPABASE_ANON_KEY")}\"")
        // Public article website base used to build canonical share URLs. Public,
        // not a secret — kept here to follow the same config architecture.
        buildConfigField("String", "ARTICLE_BASE_URL", "\"${secret("TECHVISION_ARTICLE_BASE_URL")}\"")
        // Temporary pre-Google-Play in-app updater configuration. The manifest
        // URL is a single pinned value (a stable GitHub Releases "latest" asset)
        // so remote versions can never point the app at an arbitrary host.
        // Flip UPDATER_ENABLED to false once the app is distributed via Google
        // Play and the manual updater is retired.
        buildConfigField(
            "String",
            "UPDATE_MANIFEST_URL",
            "\"https://github.com/nnou64396/tech-vision-dz-android/releases/latest/download/update-manifest.json\"",
        )
        buildConfigField("boolean", "UPDATER_ENABLED", "true")
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        debug {
            // Debug configuration can differ from release if desired.
        }
        release {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn("keystore.properties not found: release builds will not use the upload signing config.")
            }
            // Enables code + resource optimization (R8) for AGP 9.3+.
            // Keep rules live in src/main/keepRules/*.keep.
            optimization {
                enable = true
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
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
