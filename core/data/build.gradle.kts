plugins {
    alias(libs.plugins.goveye.android.library)
    alias(libs.plugins.goveye.android.hilt)
    alias(libs.plugins.goveye.android.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.goveye.app.data"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core:domain"))

    // Networking
    implementation(libs.bundles.networking)

    // Persistence
    implementation(libs.bundles.room)
    implementation(libs.androidx.datastore.preferences)

    // Paging 3
    implementation(libs.androidx.paging.runtime)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.robolectric)
}
