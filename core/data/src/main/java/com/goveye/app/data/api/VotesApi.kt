package com.goveye.app.data.api

import com.goveye.app.data.dto.votes.DivisionDto
import com.goveye.app.data.dto.votes.MemberVoteDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface VotesApi {
    @GET("divisions.json/search")
    suspend fun searchDivisions(
        @Query("itemsPerPage") itemsPerPage: Int = 20,
        @Query("skip") skip: Int = 0,
    ): List<DivisionDto>

    @GET("division/{id}.json")
    suspend fun getDivision(@Path("id") id: Int): DivisionDto

    @GET("divisions.json/membervoting")
    suspend fun getMemberVoting(
        @Query("memberId") memberId: Int,
        @Query("itemsPerPage") itemsPerPage: Int = 20,
        @Query("skip") skip: Int = 0,
    ): List<MemberVoteDto>
}
