package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.CommitteeEntity
import com.goveye.app.data.local.entity.MpCommitteeCrossRef
import com.goveye.app.data.local.entity.MpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommitteeDao {
    @Query(
        """
        SELECT c.id AS id, c.name AS name, c.house AS house,
               c.categoryName AS categoryName, c.isActive AS isActive,
               COUNT(ref.memberId) AS memberCount
        FROM committees c
        INNER JOIN mp_committee_cross_ref ref ON c.id = ref.committeeId
        INNER JOIN mps m ON ref.memberId = m.id AND m.isActive = 1
        GROUP BY c.id
        HAVING memberCount > 0
        ORDER BY c.isActive DESC, memberCount DESC, c.name
        """
    )
    fun observeAllCommittees(): Flow<List<CommitteeSummary>>

    @Query(
        """
        SELECT c.id AS id, c.name AS name, c.house AS house,
               c.categoryName AS categoryName, c.isActive AS isActive,
               COUNT(ref.memberId) AS memberCount
        FROM committees c
        INNER JOIN mp_committee_cross_ref ref ON c.id = ref.committeeId
        INNER JOIN mps m ON ref.memberId = m.id AND m.isActive = 1
        GROUP BY c.id
        HAVING memberCount > 0
        ORDER BY c.isActive DESC, memberCount DESC, c.name
        """
    )
    suspend fun getAllCommittees(): List<CommitteeSummary>

    @Query(
        """
        SELECT c.* FROM committees c
        INNER JOIN mp_committee_cross_ref ref ON c.id = ref.committeeId
        WHERE ref.memberId = :memberId
        ORDER BY c.isActive DESC, c.name
        """
    )
    fun observeCommitteesForMember(memberId: Int): Flow<List<CommitteeEntity>>

    @Query(
        """
        SELECT c.* FROM committees c
        INNER JOIN mp_committee_cross_ref ref ON c.id = ref.committeeId
        WHERE ref.memberId = :memberId
        ORDER BY c.isActive DESC, c.name
        """
    )
    suspend fun getCommitteesForMember(memberId: Int): List<CommitteeEntity>

    @Query("SELECT * FROM committees WHERE id = :committeeId")
    suspend fun getCommittee(committeeId: Int): CommitteeEntity?

    @Query("SELECT * FROM committees WHERE id = :committeeId")
    fun observeCommittee(committeeId: Int): Flow<CommitteeEntity?>

    @Query(
        """
        SELECT m.* FROM mps m
        INNER JOIN mp_committee_cross_ref ref ON m.id = ref.memberId
        WHERE ref.committeeId = :committeeId
        ORDER BY m.nameListAs
        """
    )
    fun observeCommitteeMembers(committeeId: Int): Flow<List<MpEntity>>

    @Query(
        """
        SELECT m.* FROM mps m
        INNER JOIN mp_committee_cross_ref ref ON m.id = ref.memberId
        WHERE ref.committeeId = :committeeId
        ORDER BY m.nameListAs
        """
    )
    suspend fun getCommitteeMembers(committeeId: Int): List<MpEntity>

    @Query("SELECT COUNT(*) FROM mp_committee_cross_ref WHERE committeeId = :committeeId")
    suspend fun getMemberCount(committeeId: Int): Int

    @Upsert
    suspend fun upsertCommittees(committees: List<CommitteeEntity>)

    @Upsert
    suspend fun upsertCrossRefs(refs: List<MpCommitteeCrossRef>)

    @Query("DELETE FROM mp_committee_cross_ref WHERE memberId = :memberId")
    suspend fun deleteCrossRefsForMember(memberId: Int)

    @Query("SELECT MIN(lastUpdated) FROM mp_committee_cross_ref WHERE memberId = :memberId")
    suspend fun getOldestTimestampForMember(memberId: Int): Long?
}
