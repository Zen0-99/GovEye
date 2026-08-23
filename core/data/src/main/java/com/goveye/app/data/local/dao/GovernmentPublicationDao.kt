package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.GovernmentPublicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GovernmentPublicationDao {
    @Query("SELECT * FROM government_publications ORDER BY firstPublishedAt DESC LIMIT :limit")
    fun observePublications(limit: Int = 50): Flow<List<GovernmentPublicationEntity>>

    @Query(
        "SELECT * FROM government_publications WHERE organisationSlug = :orgSlug ORDER BY firstPublishedAt DESC LIMIT :limit"
    )
    fun observePublicationsByOrg(orgSlug: String, limit: Int = 50): Flow<List<GovernmentPublicationEntity>>

    @Query(
        "SELECT * FROM government_publications WHERE title LIKE '%' || :query || '%' ORDER BY firstPublishedAt DESC LIMIT :limit"
    )
    fun searchPublications(query: String, limit: Int = 50): Flow<List<GovernmentPublicationEntity>>

    @Query("SELECT * FROM government_publications WHERE id = :id")
    suspend fun getPublication(id: Int): GovernmentPublicationEntity?
}
