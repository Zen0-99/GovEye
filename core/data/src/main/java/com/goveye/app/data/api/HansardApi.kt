package com.goveye.app.data.api

import com.goveye.app.data.dto.hansard.HansardSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface HansardApi {
    @GET("search.json")
    suspend fun search(
        @Query("searchTerm") searchTerm: String,
        @Query("itemsPerPage") itemsPerPage: Int = 20
    ): HansardSearchResponse
}
