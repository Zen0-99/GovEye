package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.CommitteeEntity
import com.goveye.app.data.local.entity.MpCommitteeCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface CommitteeDao {
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

    @Upsert
    suspend fun upsertCommittees(committees: List<CommitteeEntity>)

    @Upsert
    suspend fun upsertCrossRefs(refs: List<MpCommitteeCrossRef>)

    @Query("DELETE FROM mp_committee_cross_ref WHERE memberId = :memberId")
    suspend fun deleteCrossRefsForMember(memberId: Int)

    @Query("SELECT MIN(lastUpdated) FROM mp_committee_cross_ref WHERE memberId = :memberId")
    suspend fun getOldestTimestampForMember(memberId: Int): Long?
}
