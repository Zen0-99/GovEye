package com.goveye.app.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Manifest.json format from 10-03's manifest.py output.
 *
 * Published as a release asset on the `database-latest` tag in the
 * goveye-data repo (D-06). The app fetches this (~200B) on startup to
 * determine whether a patch or full DB download is needed (DATA-03).
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
    @SerialName("patchSize") val patchSize: Long,
)
