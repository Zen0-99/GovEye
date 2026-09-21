package com.goveye.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.goveye.app.data.local.entity.ConstituencyElectionCandidateEntity
import com.goveye.app.data.local.entity.ConstituencyElectionEntity

@Dao
interface ConstituencyElectionDao {

    // All recorded elections for a seat, newest first.
    @Query("SELECT * FROM constituency_elections WHERE constituencyId = :constituencyId ORDER BY electionDate DESC")
    suspend fun getElectionsForConstituency(constituencyId: Int): List<ConstituencyElectionEntity>

    // Candidate rows for one election, winner first.
    @Query(
        "SELECT * FROM constituency_election_candidates WHERE constituencyId = :constituencyId AND electionId = :electionId ORDER BY rankOrder"
    )
    suspend fun getCandidatesForElection(
        constituencyId: Int,
        electionId: Int
    ): List<ConstituencyElectionCandidateEntity>

    // Every election where this member stood (win or lose), newest first —
    // joins on candidate.memberId (D-05 "seats they personally contested").
    @Query(
        "SELECT DISTINCT e.* FROM constituency_elections e " +
            "INNER JOIN constituency_election_candidates c " +
            "ON c.constituencyId = e.constituencyId AND c.electionId = e.electionId " +
            "WHERE c.memberId = :memberId ORDER BY e.electionDate DESC"
    )
    suspend fun getElectionsContestedByMember(memberId: Int): List<ConstituencyElectionEntity>

    @Upsert
    suspend fun upsertElections(elections: List<ConstituencyElectionEntity>)

    @Upsert
    suspend fun upsertCandidates(candidates: List<ConstituencyElectionCandidateEntity>)
}
