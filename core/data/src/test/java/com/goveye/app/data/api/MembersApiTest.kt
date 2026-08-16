package com.goveye.app.data.api

import com.goveye.app.data.dto.members.MemberSearchResponse
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

class MembersApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: MembersApi
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
                .create(MembersApi::class.java)
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
    fun `search members parses OData envelope correctly`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loadFixture("members_search.json")))

        val response: MemberSearchResponse = api.searchMembers()

        assertTrue(response.items.size >= 2)
        assertEquals(172, response.items[0].value.id)
        assertTrue(response.items[0].value.nameListAs.contains("Abbott"))
    }

    @Test
    fun `get member parses single response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loadFixture("member_detail.json")))

        val response = api.getMember(172)

        assertEquals(172, response.value.id)
    }

    @Test
    fun `get member contact parses array`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loadFixture("member_contact.json")))

        val response = api.getMemberContact(172)

        assertEquals(2, response.value.size)
        assertEquals("Parliamentary office", response.value[0].type)
    }

    @Test(expected = retrofit2.HttpException::class)
    fun `handles 404 error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
        api.getMember(999)
    }

    @Test(expected = Exception::class)
    fun `handles malformed json`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{invalid json"))
        api.getMember(172)
    }
}
