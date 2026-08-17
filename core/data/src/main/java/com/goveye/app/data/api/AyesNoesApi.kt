package com.goveye.app.data.api

import com.goveye.app.data.dto.ayesnoes.AyesMpResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface AyesNoesApi {
    @GET("mps/{id}")
    suspend fun getMp(@Path("id") memberId: Int): AyesMpResponse
}
