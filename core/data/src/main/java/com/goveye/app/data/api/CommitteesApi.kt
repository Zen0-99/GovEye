package com.goveye.app.data.api

import com.goveye.app.data.dto.committees.CommitteeSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CommitteesApi {
    @GET("Committees")
    suspend fun getCommitteesForMember(
        @Query("MemberId") memberId: Int,
    ): CommitteeSearchResponse
}
