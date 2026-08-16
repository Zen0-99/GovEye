package com.goveye.app.data.api

import com.goveye.app.data.dto.members.BiographyResponse
import com.goveye.app.data.dto.members.ContactResponse
import com.goveye.app.data.dto.members.ExperienceResponse
import com.goveye.app.data.dto.members.MemberResponse
import com.goveye.app.data.dto.members.MemberSearchResponse
import com.goveye.app.data.dto.members.SynopsisResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MembersApi {
    @GET("Members/Search")
    suspend fun searchMembers(
        @Query("House") house: Int = 1,
        @Query("IsCurrentMember") isCurrentMember: Boolean = true,
        @Query("itemsPerPage") itemsPerPage: Int = 20,
        @Query("skip") skip: Int = 0,
    ): MemberSearchResponse

    @GET("Members/{id}")
    suspend fun getMember(@Path("id") id: Int): MemberResponse

    @GET("Members/{id}/Synopsis")
    suspend fun getMemberSynopsis(@Path("id") id: Int): SynopsisResponse

    @GET("Members/{id}/Contact")
    suspend fun getMemberContact(@Path("id") id: Int): ContactResponse

    @GET("Members/{id}/Experience")
    suspend fun getMemberExperience(@Path("id") id: Int): ExperienceResponse

    @GET("Members/{id}/Biography")
    suspend fun getMemberBiography(@Path("id") id: Int): BiographyResponse
}
