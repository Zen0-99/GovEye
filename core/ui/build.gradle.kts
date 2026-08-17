plugins {
    alias(libs.plugins.goveye.android.library)
    alias(libs.plugins.goveye.android.compose)
}

android {
    namespace = "com.goveye.app.ui"
}

dependencies {
    implementation(project(":core:domain"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    // Material 3
    implementation(libs.androidx.material3)

    // Coil 3 (image loading)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Vico charts
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
}
