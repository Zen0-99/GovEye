package com.goveye.app.data.mapper

import com.goveye.app.data.dto.interests.InterestCategoryDto
import com.goveye.app.data.dto.interests.InterestDto
import com.goveye.app.data.dto.interests.InterestFieldDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InterestMapperTest {
    @Test
    fun `maps interest DTO to domain with fields JSON`() {
        val dto =
            InterestDto(
                id = 13091,
                summary = "Test",
                category = InterestCategoryDto(id = 1, number = "1.1", name = "Employment", type = "Commons"),
                fields =
                    listOf(
                        InterestFieldDto(name = "PaymentType", type = "String", value = kotlinx.serialization.json.JsonPrimitive("Monetary")),
                    ),
            )

        val domain = InterestMapper.toDomain(dto, memberId = 172)

        assertEquals(13091, domain.id)
        assertEquals("Employment", domain.categoryName)
        assertEquals("1.1", domain.categoryNumber)
        assertTrue(domain.fieldsJson.contains("PaymentType"))
    }

    @Test
    fun `maps interest with null dates`() {
        val dto =
            InterestDto(
                id = 1,
                summary = "Test",
                registrationDate = null,
                publishedDate = null,
                category = InterestCategoryDto(id = 1, number = "1", name = "Cat", type = "Commons"),
                fields = emptyList(),
            )

        val domain = InterestMapper.toDomain(dto, memberId = 172)

        assertNull(domain.registrationDate)
        assertNull(domain.publishedDate)
    }

    @Test
    fun `maps interest list`() {
        val dtos =
            listOf(
                InterestDto(id = 1, summary = "A", category = InterestCategoryDto(id = 1, number = "1", name = "Cat", type = "Commons")),
                InterestDto(id = 2, summary = "B", category = InterestCategoryDto(id = 2, number = "2", name = "Dog", type = "Commons")),
            )

        val result = InterestMapper.toDomainList(dtos, memberId = 172)

        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
        assertEquals(2, result[1].id)
    }

    @Test
    fun `fields JSON is valid JSON string`() {
        val dto =
            InterestDto(
                id = 1,
                summary = "Test",
                category = InterestCategoryDto(id = 1, number = "1", name = "Cat", type = "Commons"),
                fields =
                    listOf(
                        InterestFieldDto(name = "Field1", type = "String", value = kotlinx.serialization.json.JsonPrimitive("Value1")),
                    ),
            )

        val domain = InterestMapper.toDomain(dto, memberId = 172)

        val parsed = Json.parseToJsonElement(domain.fieldsJson)
        assertTrue(parsed is JsonArray)
        assertEquals(1, (parsed as JsonArray).size)
    }
}
