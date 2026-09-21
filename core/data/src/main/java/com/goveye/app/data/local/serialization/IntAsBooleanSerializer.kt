package com.goveye.app.data.local.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Serializer for Boolean fields that tolerates SQLite-style integer
 * representations (0/1) in addition to JSON booleans (true/false).
 *
 * SQLite stores Booleans as INTEGER (0/1). When diff_db.py exports rows
 * to JSON for patch files, boolean columns may appear as 0/1 instead of
 * true/false. This serializer handles both forms transparently.
 *
 * Usage: annotate the field with @Serializable(with = IntAsBooleanSerializer::class)
 */
object IntAsBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IntAsBoolean", PrimitiveKind.BOOLEAN)

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }

    override fun deserialize(decoder: Decoder): Boolean = if (decoder is JsonDecoder) {
        val element = decoder.decodeJsonElement()
        when (element) {
            is JsonPrimitive -> parseBoolean(element)

            else -> throw kotlinx.serialization.SerializationException(
                "Expected JsonPrimitive, got ${element::class.simpleName}"
            )
        }
    } else {
        decoder.decodeBoolean()
    }

    private fun parseBoolean(element: JsonPrimitive): Boolean {
        // Try as string first (handles "true"/"false"/"0"/"1")
        element.contentOrNull?.let { content ->
            return when (content.lowercase()) {
                "true", "1" -> true

                "false", "0" -> false

                else -> element.intOrNull?.let { it != 0 }
                    ?: throw kotlinx.serialization.SerializationException(
                        "Cannot parse '$content' as Boolean"
                    )
            }
        }
        // Fall back to int
        return element.intOrNull?.let { it != 0 }
            ?: throw kotlinx.serialization.SerializationException(
                "Cannot parse ${element.content} as Boolean"
            )
    }
}
