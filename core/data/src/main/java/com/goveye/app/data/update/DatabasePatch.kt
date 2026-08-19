package com.goveye.app.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Patch.json format from 10-03's diff_db.py output (D-05).
 *
 * Contains per-table upsert/delete arrays. Row data is [JsonObject] because
 * different tables have different column sets; the DAO layer decodes each
 * JsonObject into the appropriate entity type using the Json instance.
 *
 * Applied atomically inside a single Room transaction — if any table fails,
 * the entire patch rolls back.
 */
@Serializable
data class DatabasePatch(
    @SerialName("patchVersion") val patchVersion: Int,
    @SerialName("previousVersion") val previousVersion: Int,
    @SerialName("generatedAt") val generatedAt: String,
    @SerialName("schemaVersion") val schemaVersion: Int,
    val changes: Map<String, TableChanges> = emptyMap(),
)

@Serializable
data class TableChanges(
    val upsert: List<JsonObject> = emptyList(),
    val delete: List<JsonObject> = emptyList(),
)
