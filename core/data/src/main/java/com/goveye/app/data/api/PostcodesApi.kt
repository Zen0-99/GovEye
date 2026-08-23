package com.goveye.app.data.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface for postcodes.io — the free UK postcode lookup API.
 *
 * Used for postcode-to-constituency search: the user types a postcode,
 * we call postcodes.io to resolve it to a parliamentary constituency,
 * then look up the MP in the local DB by constituency name.
 *
 * Rate limit: 600 requests per 5 minutes (2 req/sec). Postcode search
 * is user-initiated so this is well within limits.
 *
 * API docs: https://postcodes.io
 */
interface PostcodesApi {
    /**
     * Lookup a single postcode.
     * GET /postcodes/{postcode}
     * Returns the constituency, admin district, and geographic data.
     */
    @GET("postcodes/{postcode}")
    suspend fun lookupPostcode(@Path("postcode") postcode: String): PostcodesResponse<PostcodeResult>

    /**
     * Bulk lookup up to 100 postcodes.
     * POST /postcodes with JSON body { "postcodes": ["SW1A1AA", ...] }
     */
    @POST("postcodes")
    @Headers("Content-Type: application/json")
    suspend fun bulkLookupPostcodes(@Body request: BulkPostcodeRequest): PostcodesResponse<BulkPostcodeResult>
}

@Serializable
data class BulkPostcodeRequest(val postcodes: List<String>)

@Serializable
data class PostcodesResponse<T>(val status: Int = 0, val result: T? = null)

@Serializable
data class PostcodeResult(
    val postcode: String = "",
    val parliamentary_constituency: String? = null,
    val admin_district: String? = null,
    val admin_county: String? = null,
    val region: String? = null,
    val country: String? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val codes: PostcodeCodes? = null
)

@Serializable
data class PostcodeCodes(
    val parliamentary_constituency: String? = null,
    val admin_district: String? = null,
    val admin_county: String? = null
)

@Serializable
data class BulkPostcodeResult(val query: String = "", val result: PostcodeResult? = null)
