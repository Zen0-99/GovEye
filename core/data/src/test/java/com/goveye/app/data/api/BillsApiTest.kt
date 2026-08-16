package com.goveye.app.data.api

import com.goveye.app.data.dto.bills.BillDto
import com.goveye.app.data.dto.bills.BillListResponse
import com.goveye.app.data.dto.bills.BillStagesResponse
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

class BillsApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: BillsApi
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
                .create(BillsApi::class.java)
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
    fun `get bills parses items envelope correctly`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loadFixture("bills_list.json")))

        val response: BillListResponse = api.getBills()

        assertEquals(2, response.items.size)
        assertEquals(3973, response.items[0].billId)
        assertTrue(response.items[0].shortTitle.contains("A34"))
    }

    @Test
    fun `get bill parses single response with sponsors`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loadFixture("bill_detail.json")))

        val response: BillDto = api.getBill(3973)

        assertEquals(3973, response.billId)
        assertEquals(1, response.sponsors.size)
        assertEquals("Olivia Bailey", response.sponsors[0].member?.name)
    }

    @Test
    fun `get bill stages parses stages with sittings`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loadFixture("bill_stages.json")))

        val response: BillStagesResponse = api.getBillStages(3973)

        assertEquals(2, response.items.size)
        assertEquals("1st reading", response.items[0].description)
        assertEquals(2, response.totalResults)
    }

    @Test(expected = retrofit2.HttpException::class)
    fun `handles 404 error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
        api.getBill(999)
    }

    @Test(expected = retrofit2.HttpException::class)
    fun `handles 500 error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))
        api.getBill(999)
    }
}
