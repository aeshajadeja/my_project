// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    // This redirecting the build directory to C:/android-builds prevents OneDrive 
    // from locking files during the build process, which fixes "Unable to delete directory" errors.
    buildDir = file("C:/android-builds/${rootProject.name}/${project.name}")
}