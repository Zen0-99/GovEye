package com.goveye.app.data.api

import com.goveye.app.data.dto.interests.InterestsResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class InterestsApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: InterestsApi
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(InterestsApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun loadFixture(name: String): String =
        this::class.java.classLoader!!.getResourceAsStream("fixtures/$name")!!
            .readBytes()
            .decodeToString()

    @Test
    fun `get interests parses OData envelope with dynamic fields`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loadFixture("interests_list.json")))

        val response: InterestsResponse = api.getInterests(172)

        assertEquals(2, response.items.size)
        assertEquals(13091, response.items[0].id)
        assertEquals("Employment and earnings - Ad hoc payments", response.items[0].category.name)
    }

    @Test
    fun `parses dynamic field types correctly`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loadFixture("interests_list.json")))

        val response = api.getInterests(172)

        assertEquals(6, response.items[0].fields.size)
        assertEquals("PaymentReceived", response.items[0].fields[0].name)
        assertEquals("Boolean", response.items[0].fields[0].type)
        assertEquals("Value", response.items[0].fields[4].name)
        assertEquals("Decimal", response.items[0].fields[4].type)
    }

    @Test(expected = retrofit2.HttpException::class)
    fun `handles 404 error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
        api.getInterests(999)
    }

    @Test
    fun `handles empty interests`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"items":[],"totalResults":0}"""),
        )

        val response = api.getInterests(999)

        assertTrue(response.items.isEmpty())
    }
}
