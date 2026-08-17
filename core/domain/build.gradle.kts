plugins {
    alias(libs.plugins.goveye.android.library)
}

android {
    namespace = "com.goveye.app.domain"
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
