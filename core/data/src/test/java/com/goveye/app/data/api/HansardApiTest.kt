package com.goveye.app.data.api

import com.goveye.app.data.dto.hansard.HansardSearchResponse
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

class HansardApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: HansardApi
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
                .create(HansardApi::class.java)
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
    fun `search parses custom envelope with contributions`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loadFixture("hansard_search.json")))

        val response: HansardSearchResponse = api.search("climate")

        assertEquals(62623, response.totalContributions)
        assertEquals(2, response.contributions.size)
        assertEquals("The Lord Bishop of Oxford", response.contributions[0].memberName)
        assertEquals("Lords", response.contributions[0].house)
    }

    @Test
    fun `search parses search terms`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loadFixture("hansard_search.json")))

        val response = api.search("climate")

        assertEquals(listOf("climate"), response.searchTerms)
    }

    @Test(expected = retrofit2.HttpException::class)
    fun `handles 404 error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
        api.search("test")
    }

    @Test(expected = retrofit2.HttpException::class)
    fun `handles 500 error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))
        api.search("test")
    }

    @Test
    fun `handles empty contributions`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"TotalContributions":0,"Contributions":[],"SearchTerms":[]}""")
        )

        val response = api.search("nothing")

        assertTrue(response.contributions.isEmpty())
    }
}
