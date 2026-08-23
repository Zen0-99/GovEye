package com.goveye.app.data.repo

import android.util.Log
import com.goveye.app.data.api.PostcodeResult
import com.goveye.app.data.api.PostcodesApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for postcode lookups via postcodes.io.
 *
 * Resolves UK postcodes to parliamentary constituency names,
 * which are then used to find MPs in the local GovEye database.
 */
@Singleton
class PostcodeRepository @Inject constructor(private val postcodesApi: PostcodesApi) {
    companion object {
        private const val TAG = "GovEye/Postcode"
    }

    /**
     * Lookup a single postcode and return the constituency + council info.
     * Returns null if the postcode is not found or the API fails.
     */
    suspend fun lookupPostcode(postcode: String): PostcodeLookupResult? {
        val cleaned = postcode.replace(" ", "").uppercase()
        return try {
            Log.i(TAG, "Looking up postcode: $cleaned")
            val response = postcodesApi.lookupPostcode(cleaned)
            val result = response.result
            if (response.status == 200 && result != null) {
                Log.i(
                    TAG,
                    "Postcode $cleaned -> constituency=${result.parliamentary_constituency}, district=${result.admin_district}"
                )
                PostcodeLookupResult(
                    postcode = result.postcode,
                    constituencyName = result.parliamentary_constituency,
                    adminDistrict = result.admin_district,
                    adminCounty = result.admin_county,
                    region = result.region,
                    country = result.country
                )
            } else {
                Log.w(TAG, "Postcode $cleaned not found (status=${response.status})")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Postcode lookup failed for $cleaned: ${e.message}")
            null
        }
    }
}

data class PostcodeLookupResult(
    val postcode: String,
    val constituencyName: String?,
    val adminDistrict: String?,
    val adminCounty: String?,
    val region: String?,
    val country: String?
)
