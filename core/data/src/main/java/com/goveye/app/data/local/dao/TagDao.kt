package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.BillTagEntity
import com.goveye.app.data.local.entity.DivisionTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    // --- Division tags ---

    @Query("SELECT tag FROM division_tags WHERE divisionId = :divisionId ORDER BY hitCount DESC")
    fun observeTagsForDivision(divisionId: Int): Flow<List<String>>

    @Query("SELECT tag FROM division_tags WHERE divisionId = :divisionId ORDER BY hitCount DESC")
    suspend fun getTagsForDivision(divisionId: Int): List<String>

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

    // --- Combined ---

    @Query("""
        SELECT DISTINCT tag FROM (
            SELECT tag FROM division_tags
            UNION
            SELECT tag FROM bill_tags
        )
        ORDER BY tag
    """)
    suspend fun getAllTags(): List<String>
}
