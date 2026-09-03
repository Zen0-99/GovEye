package com.goveye.app.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Serializer for sittingDates that handles both JSON arrays (from proper patch JSON)
 * and JSON-encoded strings (from the DB column which stores json.dumps output).
 *
 * The build scripts store sittingDates as json.dumps(list) — a JSON array encoded
 * as a string. When the patch system extracts this value, it becomes a JSON string
 * literal in the patch JSON. This serializer detects that and parses the inner JSON.
 */
object SittingDatesSerializer : JsonTransformingSerializer<List<String>>(
    ListSerializer(String.serializer())
) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        // If the element is a JSON string (JsonPrimitive), parse its content as a JSON array
        if (element is JsonPrimitive && element.isString) {
            return kotlinx.serialization.json.Json.parseToJsonElement(element.content)
        }
        return element
    }
}

@Serializable
@Entity(tableName = "bill_stages", primaryKeys = ["billId", "stageId"])
data class BillStageEntity(
    val billId: Int,
    val stageId: Int,
    val description: String,
    val abbreviation: String,
    val house: String,
    val sortOrder: Int,
    val sessionId: Int? = null,
    @Serializable(with = SittingDatesSerializer::class)
    val sittingDates: List<String>,
    val lastUpdated: Long
)
