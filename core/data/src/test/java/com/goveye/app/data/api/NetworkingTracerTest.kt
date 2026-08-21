package com.goveye.app.data.api

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Serializable
data class TracerResponse(val id: Int, val name: String)

interface TracerApi {
    @retrofit2.http.GET("test")
    suspend fun getTest(): TracerResponse
}

class NetworkingTracerTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TracerApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }

        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TracerApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `networking stack parses JSON response from MockWebServer`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":172,"name":"Abbott, Ms Diane"}""")
        )

        val response = api.getTest()

        assertEquals(172, response.id)
        assertEquals("Abbott, Ms Diane", response.name)
        assertNotNull(server.takeRequest())
    }

    @Test
    fun `networking stack handles unknown keys gracefully`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":172,"name":"Abbott, Ms Diane","unknownField":"ignored"}""")
        )

        val response = api.getTest()

        assertEquals(172, response.id)
        assertEquals("Abbott, Ms Diane", response.name)
    }
}
