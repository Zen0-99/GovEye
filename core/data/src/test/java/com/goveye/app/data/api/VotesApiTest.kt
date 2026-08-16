package com.goveye.app.data.api

import com.goveye.app.data.dto.votes.DivisionDto
import com.goveye.app.data.dto.votes.MemberVoteDto
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

class VotesApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: VotesApi
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
                .create(VotesApi::class.java)
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
    fun `search divisions parses bare array correctly`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loadFixture("divisions_search.json")))

        val result: List<DivisionDto> = api.searchDivisions()

        assertEquals(2, result.size)
        assertEquals(2411, result[0].divisionId)
        assertEquals(330, result[0].ayeCount)
    }

    @Test
    fun `get division parses single response with voters`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loadFixture("division_detail.json")))

        val response: DivisionDto = api.getDivision(2409)

        assertEquals(2409, response.divisionId)
        assertEquals(2, response.ayes.size)
        assertEquals(39, response.ayes[0].memberId)
    }

    @Test
    fun `get member voting parses bare array with embedded division`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loadFixture("member_voting.json")))

        val result: List<MemberVoteDto> = api.getMemberVoting(172)

        assertEquals(2, result.size)
        assertTrue(result[0].memberVotedNo)
        assertEquals(2406, result[0].publishedDivision?.divisionId)
    }

    @Test(expected = retrofit2.HttpException::class)
    fun `handles 404 error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
        api.getDivision(999)
    }

    @Test(expected = retrofit2.HttpException::class)
    fun `handles 500 error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))
        api.getDivision(999)
    }
}
