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

    /**
     * Batch fetch tags for multiple publications in a single query.
     * Returns rows ordered by hitCount so grouping preserves priority.
     * Use this instead of calling [getTagsForPublication] in a loop.
     */
    @Query(
        "SELECT publicationId, tag, hitCount FROM publication_tags WHERE publicationId IN (:ids) ORDER BY hitCount DESC"
    )
    suspend fun getTagRowsForPublications(ids: List<Int>): List<com.goveye.app.data.local.entity.PublicationTagEntity>

    // --- Statement tags ---

    @Query("SELECT tag FROM statement_tags WHERE statementId = :statementId ORDER BY hitCount DESC")
    fun observeTagsForStatement(statementId: Int): Flow<List<String>>

    @Query("SELECT tag FROM statement_tags WHERE statementId = :statementId ORDER BY hitCount DESC")
    suspend fun getTagsForStatement(statementId: Int): List<String>

    @Query("SELECT statementId FROM statement_tags WHERE tag = :tag")
    suspend fun getStatementIdsForTag(tag: String): List<Int>

    @Query("SELECT DISTINCT tag FROM statement_tags ORDER BY tag")
    fun observeAllStatementTags(): Flow<List<String>>

    /**
     * Batch fetch tags for multiple statements in a single query.
     * Use this instead of calling [getTagsForStatement] in a loop.
     */
    @Query("SELECT statementId, tag, hitCount FROM statement_tags WHERE statementId IN (:ids) ORDER BY hitCount DESC")
    suspend fun getTagRowsForStatements(ids: List<Int>): List<com.goveye.app.data.local.entity.StatementTagEntity>

    // --- Legislation tags ---

    @Query("SELECT tag FROM legislation_tags WHERE legislationId = :legislationId ORDER BY hitCount DESC")
    fun observeTagsForLegislation(legislationId: Int): Flow<List<String>>

    @Query("SELECT tag FROM legislation_tags WHERE legislationId = :legislationId ORDER BY hitCount DESC")
    suspend fun getTagsForLegislation(legislationId: Int): List<String>

    @Query("SELECT legislationId FROM legislation_tags WHERE tag = :tag")
    suspend fun getLegislationIdsForTag(tag: String): List<Int>

    @Query("SELECT DISTINCT tag FROM legislation_tags ORDER BY tag")
    fun observeAllLegislationTags(): Flow<List<String>>

    /**
     * Batch fetch tags for multiple legislation items in a single query.
     * Use this instead of calling [getTagsForLegislation] in a loop.
     */
    @Query(
        "SELECT legislationId, tag, hitCount FROM legislation_tags WHERE legislationId IN (:ids) ORDER BY hitCount DESC"
    )
    suspend fun getTagRowsForLegislation(ids: List<Int>): List<com.goveye.app.data.local.entity.LegislationTagEntity>
}
