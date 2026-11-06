// :app is the Android TV application. It depends on :core for logic and adds
// Android-specific code (screens, player, manifest) on top.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.calias.tv.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.calias.tv"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            // App optimization (AGP 9.3+ DSL, per official docs): R8 removes unused code AND unused
            // resources, and rewrites code for speed. Debug builds skip this on purpose.
            optimization {
                enable = true
            }
            // Signed with the debug key so it installs on the TV without a store keystore.
            // Personal use only. Not for publishing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)

    implementation(libs.activity.compose)
    implementation(libs.tv.material)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
}
