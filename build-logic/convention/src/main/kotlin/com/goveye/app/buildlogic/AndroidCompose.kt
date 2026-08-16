package com.goveye.app.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    commonExtension.apply {
        buildFeatures.apply {
            compose = true
        }
    }

    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    dependencies {
        add("implementation", platform(libs.findLibrary("androidx.compose.bom").get()))
        add("androidTestImplementation", platform(libs.findLibrary("androidx.compose.bom").get()))
    }

    extensions.getByType<ComposeCompilerGradlePluginExtension>().apply {
        // Compose compiler configuration — can add stability config, metrics, reports later
    }
}
