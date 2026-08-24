package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.goveye.app.data.local.entity.HistoricalMemberEntity

@Dao
interface HistoricalMemberDao {
    @Query("SELECT * FROM historical_members WHERE twfyPersonId = :twfyPersonId")
    suspend fun getByTwfyPersonId(twfyPersonId: Int): HistoricalMemberEntity?

    @Query("SELECT * FROM historical_members WHERE twfyPersonId IN (:twfyPersonIds)")
    suspend fun getByTwfyPersonIds(twfyPersonIds: List<Int>): List<HistoricalMemberEntity>

    @Query("SELECT * FROM historical_members WHERE parliamentMemberId = :parliamentMemberId")
    suspend fun getByParliamentMemberId(parliamentMemberId: Int): HistoricalMemberEntity?

    @Query("SELECT * FROM historical_members WHERE isCurrent = 1 ORDER BY displayName")
    suspend fun getCurrentMembers(): List<HistoricalMemberEntity>

    @Query("SELECT * FROM historical_members ORDER BY isCurrent DESC, displayName")
    suspend fun getAll(): List<HistoricalMemberEntity>

    @Query(
        """
        SELECT hm.* FROM historical_members_fts4
        JOIN historical_members hm ON hm.rowid = historical_members_fts4.rowid
        WHERE historical_members_fts4 MATCH :query
        ORDER BY hm.isCurrent DESC, hm.displayName
        LIMIT 100
        """
    )
    suspend fun search(query: String): List<HistoricalMemberEntity>

    @Query("SELECT COUNT(*) FROM historical_members")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM historical_members WHERE isCurrent = 1")
    suspend fun currentCount(): Int

    @Query(
        """
        SELECT * FROM historical_members
        WHERE displayName LIKE '%' || :name || '%'
        ORDER BY isCurrent DESC, displayName
        LIMIT 10
        """
    )
    suspend fun searchByDisplayName(name: String): List<HistoricalMemberEntity>
}
