// Root build file. Declares which Gradle plugins exist and their versions (from libs.versions.toml).
// "apply false" means: known here, but only switched on inside the module that needs it.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
}
