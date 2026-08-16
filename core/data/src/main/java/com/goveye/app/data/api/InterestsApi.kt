package com.goveye.app.data.api

import com.goveye.app.data.dto.interests.InterestsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface InterestsApi {
    @GET("Interests")
    suspend fun getInterests(
        @Query("MemberId") memberId: Int,
        @Query("Take") take: Int = 20,
        @Query("Skip") skip: Int = 0,
        @Query("SortOrder") sortOrder: String = "PublishingDateDescending",
    ): InterestsResponse
}
