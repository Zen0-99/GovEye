package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for announcement tag tables (publication_tags, statement_tags,
 * legislation_tags). Follows the [TagDao] pattern for division/bill tags.
 */
@Dao
interface AnnouncementTagDao {
    // --- Publication tags ---

    @Query("SELECT tag FROM publication_tags WHERE publicationId = :publicationId ORDER BY hitCount DESC")
    fun observeTagsForPublication(publicationId: Int): Flow<List<String>>

    @Query("SELECT tag FROM publication_tags WHERE publicationId = :publicationId ORDER BY hitCount DESC")
    suspend fun getTagsForPublication(publicationId: Int): List<String>

    @Query("SELECT publicationId FROM publication_tags WHERE tag = :tag")
    suspend fun getPublicationIdsForTag(tag: String): List<Int>

    @Query("SELECT DISTINCT tag FROM publication_tags ORDER BY tag")
    fun observeAllPublicationTags(): Flow<List<String>>

    // --- Statement tags ---

    @Query("SELECT tag FROM statement_tags WHERE statementId = :statementId ORDER BY hitCount DESC")
    fun observeTagsForStatement(statementId: Int): Flow<List<String>>

    @Query("SELECT tag FROM statement_tags WHERE statementId = :statementId ORDER BY hitCount DESC")
    suspend fun getTagsForStatement(statementId: Int): List<String>

    @Query("SELECT statementId FROM statement_tags WHERE tag = :tag")
    suspend fun getStatementIdsForTag(tag: String): List<Int>

    @Query("SELECT DISTINCT tag FROM statement_tags ORDER BY tag")
    fun observeAllStatementTags(): Flow<List<String>>

    // --- Legislation tags ---

    @Query("SELECT tag FROM legislation_tags WHERE legislationId = :legislationId ORDER BY hitCount DESC")
    fun observeTagsForLegislation(legislationId: Int): Flow<List<String>>

    @Query("SELECT tag FROM legislation_tags WHERE legislationId = :legislationId ORDER BY hitCount DESC")
    suspend fun getTagsForLegislation(legislationId: Int): List<String>

    @Query("SELECT legislationId FROM legislation_tags WHERE tag = :tag")
    suspend fun getLegislationIdsForTag(tag: String): List<Int>

    @Query("SELECT DISTINCT tag FROM legislation_tags ORDER BY tag")
    fun observeAllLegislationTags(): Flow<List<String>>
}
