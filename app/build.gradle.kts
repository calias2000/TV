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

    lint {
        // The TV banner is one 320x180 image in drawable-xhdpi on purpose: TV screens are xhdpi.
        disable += "IconMissingDensityFolder"
    }

    testOptions {
        unitTests {
            // Robolectric needs the app resources (fonts, drawables) to draw screens in a JVM test.
            isIncludeAndroidResources = true
            all {
                // Robolectric reaches into JDK internals that recent JDKs (24+) close by default.
                it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--enable-native-access=ALL-UNNAMED")
            }
        }
    }

    buildTypes {
        release {
            // R8: removes unused code and resources and rewrites code for speed. Debug builds skip it on purpose.
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
    // Format modules: ExoPlayer plays plain streams alone; HLS (.m3u8) and DASH (.mpd) need their own module.
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui.compose)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    implementation(libs.datastore.preferences)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Screen and state tests run on the JVM (Robolectric), with fake data: no device, no network.
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
