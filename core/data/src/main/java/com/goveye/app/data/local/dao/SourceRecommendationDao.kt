package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.SourceRecommendationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceRecommendationDao {
    @Query("SELECT * FROM source_recommendations WHERE tag = :tag ORDER BY hitCount DESC")
    fun observeRecommendationsForTag(tag: String): Flow<List<SourceRecommendationEntity>>

    @Query("SELECT * FROM source_recommendations ORDER BY tag, hitCount DESC")
    fun observeAllRecommendations(): Flow<List<SourceRecommendationEntity>>

    @Query("SELECT * FROM source_recommendations WHERE tag = :tag AND isRecommended = 1 ORDER BY hitCount DESC")
    fun observeRecommendedForTag(tag: String): Flow<List<SourceRecommendationEntity>>
}
