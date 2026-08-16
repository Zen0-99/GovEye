package com.goveye.app.data.api

import com.goveye.app.data.dto.bills.BillDto
import com.goveye.app.data.dto.bills.BillListResponse
import com.goveye.app.data.dto.bills.BillStagesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BillsApi {
    @GET("Bills")
    suspend fun getBills(
        @Query("itemsPerPage") itemsPerPage: Int = 20,
        @Query("skip") skip: Int = 0,
    ): BillListResponse

    @GET("Bills/{id}")
    suspend fun getBill(@Path("id") id: Int): BillDto

    @GET("Bills/{id}/Stages")
    suspend fun getBillStages(@Path("id") id: Int): BillStagesResponse
}
