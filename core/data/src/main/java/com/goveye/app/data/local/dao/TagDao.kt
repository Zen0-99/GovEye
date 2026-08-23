package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.BillTagEntity
import com.goveye.app.data.local.entity.DivisionTagEntity
import com.goveye.app.data.local.entity.TagMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    // --- Division tags ---

    @Query("SELECT tag FROM division_tags WHERE divisionId = :divisionId ORDER BY hitCount DESC")
    fun observeTagsForDivision(divisionId: Int): Flow<List<String>>

    @Query("SELECT tag FROM division_tags WHERE divisionId = :divisionId ORDER BY hitCount DESC")
    suspend fun getTagsForDivision(divisionId: Int): List<String>

    @Query("SELECT * FROM division_tags ORDER BY hitCount DESC")
    fun observeAllDivisionTagRows(): Flow<List<DivisionTagEntity>>

    @Query("SELECT DISTINCT tag FROM division_tags ORDER BY tag")
    fun observeAllDivisionTags(): Flow<List<String>>

    @Query("SELECT DISTINCT tag FROM division_tags ORDER BY tag")
    suspend fun getAllDivisionTags(): List<String>

    @Query("SELECT divisionId FROM division_tags WHERE tag = :tag")
    suspend fun getDivisionIdsForTag(tag: String): List<Int>

    // --- Bill tags ---

    @Query("SELECT tag FROM bill_tags WHERE billId = :billId ORDER BY hitCount DESC")
    fun observeTagsForBill(billId: Int): Flow<List<String>>

    @Query("SELECT tag FROM bill_tags WHERE billId = :billId ORDER BY hitCount DESC")
    suspend fun getTagsForBill(billId: Int): List<String>

    @Query("SELECT DISTINCT tag FROM bill_tags ORDER BY tag")
    fun observeAllBillTags(): Flow<List<String>>

    @Query("SELECT DISTINCT tag FROM bill_tags ORDER BY tag")
    suspend fun getAllBillTags(): List<String>

    @Query("SELECT billId FROM bill_tags WHERE tag = :tag")
    suspend fun getBillIdsForTag(tag: String): List<Int>

    // --- Tag metadata ---

    @Query("SELECT * FROM tag_metadata WHERE tag = :tag")
    suspend fun getTagMetadata(tag: String): TagMetadataEntity?

    @Query(
        "SELECT * FROM tag_metadata WHERE divisionCount > 0 OR billCount > 0 ORDER BY divisionCount DESC, billCount DESC"
    )
    fun observeAllTagMetadata(): Flow<List<TagMetadataEntity>>

    @Query(
        "SELECT * FROM tag_metadata WHERE divisionCount > 0 OR billCount > 0 ORDER BY divisionCount DESC, billCount DESC"
    )
    suspend fun getAllTagMetadata(): List<TagMetadataEntity>

    // --- Combined ---

    @Query(
        """
        SELECT DISTINCT tag FROM (
            SELECT tag FROM division_tags
            UNION
            SELECT tag FROM bill_tags
            UNION
            SELECT tag FROM publication_tags
            UNION
            SELECT tag FROM statement_tags
            UNION
            SELECT tag FROM legislation_tags
            UNION
            SELECT tag FROM mp_tags
        )
        ORDER BY tag
    """
    )
    suspend fun getAllTags(): List<String>

    @Query(
        """
        SELECT DISTINCT tag FROM (
            SELECT tag FROM publication_tags
            UNION
            SELECT tag FROM statement_tags
            UNION
            SELECT tag FROM legislation_tags
        )
        ORDER BY tag
    """
    )
    fun observeAllAnnouncementTags(): Flow<List<String>>

    @Query("SELECT DISTINCT tag FROM mp_tags ORDER BY tag")
    fun observeAllMpTags(): Flow<List<String>>
}
