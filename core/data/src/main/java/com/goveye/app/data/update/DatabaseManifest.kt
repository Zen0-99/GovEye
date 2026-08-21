package com.goveye.app.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Manifest.json format from the goveye-data repo's build scripts (D-05, D-10).
 *
 * Each per-API release (mps-latest, commons-votes-latest, lords-votes-latest,
 * bills-latest, committees-latest, recess-latest) has its own manifest.json with its own
 * version number. The Android app fetches 6 manifests and compares each
 * against its corresponding per-API version key in DatabasePreferences
 * (D-10a). The seed-latest release may optionally have a manifest for
 * SHA-256 verification.
 */
@Serializable
data class DatabaseManifest(
    val version: Int,
    @SerialName("previousVersion") val previousVersion: Int? = null,
    @SerialName("schemaVersion") val schemaVersion: Int,
    @SerialName("generatedAt") val generatedAt: String,
    @SerialName("dbHash") val dbHash: String,
    @SerialName("dbSize") val dbSize: Long,
    @SerialName("patchHash") val patchHash: String,
    @SerialName("patchSize") val patchSize: Long
)
