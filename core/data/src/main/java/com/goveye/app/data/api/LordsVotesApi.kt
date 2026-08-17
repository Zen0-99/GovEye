package com.goveye.app.data.api

import com.goveye.app.data.dto.votes.LordsDivisionDto
import com.goveye.app.data.dto.votes.LordsMemberVoteDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Lords Votes API — separate base URL from Commons.
 * Uses PascalCase paths (Divisions vs divisions) and camelCase field names.
 * Content / Not Content = Aye / No equivalent.
 */
interface LordsVotesApi {
    @GET("Divisions/search")
    suspend fun searchDivisions(
        @Query("itemsPerPage") itemsPerPage: Int = 20,
        @Query("skip") skip: Int = 0,
    ): List<LordsDivisionDto>

    @GET("Divisions/{id}")
    suspend fun getDivision(@Path("id") id: Int): LordsDivisionDto

    @GET("Divisions/membervoting")
    suspend fun getMemberVoting(
        @Query("memberId") memberId: Int,
        @Query("itemsPerPage") itemsPerPage: Int = 20,
        @Query("skip") skip: Int = 0,
    ): List<LordsMemberVoteDto>
}
