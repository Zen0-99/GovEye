package com.goveye.app.data.repo

import com.goveye.app.data.api.LordsVotesApi
import com.goveye.app.data.api.VotesApi
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.mapper.DivisionMapper
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.model.DivisionVote
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.NewVote
import com.goveye.app.domain.model.PartyBreakdown
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import com.goveye.app.domain.model.VoteType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class VotesRepository @Inject constructor(
    private val divisionDao: DivisionDao,
    private val votesApi: VotesApi,
    private val lordsVotesApi: LordsVotesApi,
    private val mapper: DivisionMapper,
) {
    fun observeDivisions(limit: Int = 200): Flow<RepositoryResult<List<Division>>> =
        divisionDao.observeDivisions(limit).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                val oldest = entities.minOf { it.lastUpdated }
                val isStale = System.currentTimeMillis() - oldest > CacheTtl.DIVISIONS_MS
                RepositoryResult(entities.map { it.toDomain() }, if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
            }
        }

    fun observeDivisionsByHouse(house: Int, limit: Int = 200): Flow<RepositoryResult<List<Division>>> =
        if (house == 0) {
            observeDivisions(limit)
        } else {
            divisionDao.observeDivisionsByHouse(house, limit).map { entities ->
                if (entities.isEmpty()) {
                    RepositoryResult(emptyList(), SyncStatus.EMPTY)
                } else {
                    val oldest = entities.minOf { it.lastUpdated }
                    val isStale = System.currentTimeMillis() - oldest > CacheTtl.DIVISIONS_MS
                    RepositoryResult(entities.map { it.toDomain() }, if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
                }
            }
        }

    fun searchDivisions(query: String, house: Int = 0, limit: Int = 50): Flow<RepositoryResult<List<Division>>> =
        if (house == 0) {
            divisionDao.searchDivisions(query, limit)
        } else {
            divisionDao.searchDivisionsByHouse(query, house, limit)
        }.map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                RepositoryResult(entities.map { it.toDomain() }, SyncStatus.FRESH)
            }
        }

    fun observeDivision(id: Int): Flow<RepositoryResult<Division?>> =
        divisionDao.observeDivision(id).map { entity ->
            if (entity == null) {
                RepositoryResult(null, SyncStatus.EMPTY)
            } else {
                val isStale = System.currentTimeMillis() - entity.lastUpdated > CacheTtl.DIVISIONS_MS
                RepositoryResult(entity.toDomain(), if (isStale) SyncStatus.STALE else SyncStatus.FRESH)
            }
        }

    fun observeVotesForDivision(divisionId: Int): Flow<RepositoryResult<List<DivisionVote>>> =
        divisionDao.observeVotesForDivision(divisionId).map { entities ->
            RepositoryResult(entities.map { it.toDomain() }, SyncStatus.FRESH)
        }

    fun observeMemberVoting(memberId: Int): Flow<RepositoryResult<List<DivisionVote>>> =
        divisionDao.observeVotesForMember(memberId).map { entities ->
            if (entities.isEmpty()) {
                RepositoryResult(emptyList(), SyncStatus.EMPTY)
            } else {
                RepositoryResult(entities.map { it.toDomain() }, SyncStatus.FRESH)
            }
        }

    suspend fun refresh() {
        try {
            val pageSize = 25
            var skip = 0
            val allEntities = mutableListOf<DivisionEntity>()

            while (true) {
                val divisions = votesApi.searchDivisions(itemsPerPage = pageSize, skip = skip)
                if (divisions.isEmpty()) break
                allEntities.addAll(divisions.map { dto ->
                    DivisionEntity(
                        id = dto.divisionId,
                        title = dto.title,
                        date = dto.date,
                        publicationUpdated = dto.publicationUpdated,
                        number = dto.number,
                        isDeferred = dto.isDeferred,
                        ayeCount = dto.ayeCount,
                        noCount = dto.noCount,
                        house = 1,
                        lastUpdated = System.currentTimeMillis(),
                    )
                })
                if (divisions.size < pageSize) break
                skip += pageSize
            }

            if (allEntities.isNotEmpty()) divisionDao.upsertAll(allEntities)
        } catch (e: Exception) {
            // Cache is still served
        }
    }

    suspend fun refreshLords() {
        try {
            val pageSize = 25
            var skip = 0
            val allEntities = mutableListOf<DivisionEntity>()

            while (true) {
                val divisions = lordsVotesApi.searchDivisions(itemsPerPage = pageSize, skip = skip)
                if (divisions.isEmpty()) break
                allEntities.addAll(divisions.map { dto ->
                    DivisionEntity(
                        id = dto.divisionId,
                        title = dto.title,
                        date = dto.date,
                        publicationUpdated = null,
                        number = dto.number,
                        isDeferred = false,
                        ayeCount = dto.memberContentCount,
                        noCount = dto.memberNotContentCount,
                        house = 2,
                        lastUpdated = System.currentTimeMillis(),
                    )
                })
                if (divisions.size < pageSize) break
                skip += pageSize
            }

            if (allEntities.isNotEmpty()) divisionDao.upsertAll(allEntities)
        } catch (e: Exception) {
            // Cache is still served
        }
    }

    /**
     * Fetch full division detail with voter lists and persist votes.
     * Commons: Ayes → AYE, Noes → NO
     */
    suspend fun refreshDivisionDetail(divisionId: Int, house: Int) {
        try {
            if (house == 2) {
                val dto = lordsVotesApi.getDivision(divisionId)
                val votes = mutableListOf<DivisionVoteEntity>()
                dto.contents.forEach { voter ->
                    votes.add(mapper.toDomain(voter, divisionId, VoteType.AYE).toEntity())
                }
                dto.notContents.forEach { voter ->
                    votes.add(mapper.toDomain(voter, divisionId, VoteType.NO).toEntity())
                }
                if (votes.isNotEmpty()) divisionDao.upsertVotes(votes)
            } else {
                val dto = votesApi.getDivision(divisionId)
                val votes = mutableListOf<DivisionVoteEntity>()
                dto.ayes.forEach { voter ->
                    votes.add(mapper.toDomain(voter, divisionId, VoteType.AYE).toEntity())
                }
                dto.noes.forEach { voter ->
                    votes.add(mapper.toDomain(voter, divisionId, VoteType.NO).toEntity())
                }
                if (votes.isNotEmpty()) divisionDao.upsertVotes(votes)
            }
        } catch (e: Exception) {
            // Cache is still served
        }
    }

    /**
     * Fetch a member's full voting record from the API and cache it.
     * Paginates through all available votes (API returns newest first, max 25/page).
     * Upserts after each page so UI can show data progressively.
     * Also stores division metadata so charts can join votes to dates.
     */
    suspend fun refreshMemberVoting(memberId: Int, house: Int) {
        try {
            val pageSize = 25
            var skip = 0

            if (house == 2) {
                // Lords API
                while (true) {
                    val dtos = lordsVotesApi.getMemberVoting(memberId, itemsPerPage = pageSize, skip = skip)
                    if (dtos.isEmpty()) break
                    val pageVotes = mutableListOf<DivisionVoteEntity>()
                    val pageDivisions = mutableListOf<DivisionEntity>()
                    dtos.forEach { dto ->
                        val vote = mapper.toDomain(dto) ?: return@forEach
                        pageVotes.add(vote.toEntity())
                        val pub = dto.publishedDivision ?: return@forEach
                        pageDivisions.add(
                            DivisionEntity(
                                id = pub.divisionId,
                                title = pub.title,
                                date = pub.date,
                                publicationUpdated = null,
                                number = null,
                                isDeferred = false,
                                ayeCount = pub.memberContentCount,
                                noCount = pub.memberNotContentCount,
                                house = 2,
                                lastUpdated = System.currentTimeMillis(),
                            ),
                        )
                    }
                    if (pageDivisions.isNotEmpty()) divisionDao.upsertAll(pageDivisions)
                    if (pageVotes.isNotEmpty()) divisionDao.upsertVotes(pageVotes)
                    if (dtos.size < pageSize) break
                    skip += pageSize
                }
            } else {
                // Commons API
                while (true) {
                    val dtos = votesApi.getMemberVoting(memberId, itemsPerPage = pageSize, skip = skip)
                    if (dtos.isEmpty()) break
                    val pageVotes = mutableListOf<DivisionVoteEntity>()
                    val pageDivisions = mutableListOf<DivisionEntity>()
                    dtos.forEach { dto ->
                        val vote = mapper.toDomain(dto) ?: return@forEach
                        pageVotes.add(vote.toEntity())
                        val pub = dto.publishedDivision ?: return@forEach
                        pageDivisions.add(
                            DivisionEntity(
                                id = pub.divisionId,
                                title = pub.title,
                                date = pub.date,
                                publicationUpdated = null,
                                number = null,
                                isDeferred = false,
                                ayeCount = pub.ayeCount,
                                noCount = pub.noCount,
                                house = 1,
                                lastUpdated = System.currentTimeMillis(),
                            ),
                        )
                    }
                    if (pageDivisions.isNotEmpty()) divisionDao.upsertAll(pageDivisions)
                    if (pageVotes.isNotEmpty()) divisionDao.upsertVotes(pageVotes)
                    if (dtos.size < pageSize) break
                    skip += pageSize
                }
            }
        } catch (e: Exception) {
            // Cache is still served
        }
    }

    /**
     * Compute party-level breakdown of Ayes and Noes for a division.
     */
    suspend fun getPartyBreakdown(divisionId: Int): List<PartyBreakdown> {
        val voteList = getVotesForDivisionSync(divisionId)
        val byParty = voteList.groupBy { it.partyName }
        return byParty.map { (party, partyVotes) ->
            PartyBreakdown(
                partyName = party,
                partyColour = partyVotes.firstOrNull()?.partyColour ?: "",
                ayeCount = partyVotes.count { it.vote == VoteType.AYE.name },
                noCount = partyVotes.count { it.vote == VoteType.NO.name },
                totalMembers = partyVotes.size,
            )
        }.sortedByDescending { it.totalMembers }
    }

    /**
     * Get a member's voting record with division context.
     * Returns a list of [MemberVoteWithDivision] sorted by date descending.
     */
    suspend fun getMemberVotingWithDivisions(memberId: Int): List<MemberVoteWithDivision> {
        val votes = divisionDao.getVotesForMember(memberId)
        val divisionIds = votes.map { it.divisionId }.distinct()
        val divisions = divisionIds.associateWith { divisionDao.getDivision(it) }
        return votes.mapNotNull { voteEntity ->
            val division = divisions[voteEntity.divisionId] ?: return@mapNotNull null
            MemberVoteWithDivision(
                divisionId = division.id,
                divisionTitle = division.title,
                divisionDate = division.date,
                house = division.house,
                ayeCount = division.ayeCount,
                noCount = division.noCount,
                vote = VoteType.valueOf(voteEntity.vote),
                isTeller = voteEntity.isTeller,
            )
        }.sortedByDescending { it.divisionDate }
    }

    /**
     * Get all votes for a set of divisions (used for rebellion computation).
     * Returns a map of divisionId → list of all votes in that division.
     */
    suspend fun getAllVotesForDivisions(divisionIds: List<Int>): Map<Int, List<DivisionVote>> {
        return divisionIds.associateWith { id ->
            divisionDao.getVotesForDivision(id).map { it.toDomain() }
        }
    }

    /**
     * Batch-fetch full division details (all votes) for a list of division IDs.
     * Skips divisions that already have votes cached. Used to populate the
     * full voter lists needed for rebellion computation.
     *
     * @param divisionIds Divisions to fetch details for
     * @param house House (1=Commons, 2=Lords) — determines which API to call
     * @param limit Maximum number of divisions to fetch (most recent first)
     */
    suspend fun batchFetchDivisionDetails(divisionIds: List<Int>, house: Int, limit: Int = 100) {
        val toFetch = divisionIds
            .distinct()
            .take(limit)
            .filter { id -> divisionDao.countVotesForDivision(id) == 0 }
        for (id in toFetch) {
            try {
                refreshDivisionDetail(id, house)
            } catch (e: Exception) {
                // Continue fetching other divisions even if one fails
            }
        }
    }

    /**
     * Get all divisions for a given house (used for attendance calculation).
     * Returns all division dates so we can compute how many divisions
     * occurred in each period vs how many the MP voted in.
     */
    suspend fun getAllDivisionDates(house: Int): List<String> =
        divisionDao.getAllDivisionsByHouse(house).map { it.date }

    /**
     * Detect new votes for a member since the last poll.
     *
     * Compares the set of division IDs the member has voted in (before vs
     * after refreshing from the API). Returns a [NewVote] for each new
     * division, with enough context to build a notification.
     *
     * Used by the VotePollingWorker (Phase 6, D-02).
     *
     * @param memberId The followed MP's ID
     * @param house The MP's house (1=Commons, 2=Lords)
     * @param memberName The MP's display name (for notification title)
     * @param thumbnailUrl The MP's thumbnail URL (for notification large icon)
     * @param partyName The MP's party name (for rebellion detection)
     * @return List of new votes, or empty list if none
     */
    suspend fun detectNewVotesForMember(
        memberId: Int,
        house: Int,
        memberName: String,
        thumbnailUrl: String?,
        partyName: String,
    ): List<NewVote> {
        // 1. Get existing division IDs before refresh
        val existingIds = divisionDao.getDivisionIdsForMember(memberId).toSet()

        // 2. Refresh from API (upserts new votes into DB)
        refreshMemberVoting(memberId, house)

        // 3. Get current division IDs after refresh
        val currentIds = divisionDao.getDivisionIdsForMember(memberId).toSet()

        // 4. New IDs = current - existing
        val newIds = currentIds - existingIds
        if (newIds.isEmpty()) return emptyList()

        // 5. For each new vote, build a NewVote with division context
        return newIds.mapNotNull { divisionId ->
            val division = divisionDao.getDivision(divisionId) ?: return@mapNotNull null
            val voteEntity = divisionDao.getVotesForMember(memberId)
                .firstOrNull { it.divisionId == divisionId } ?: return@mapNotNull null
            val voteType = runCatching { VoteType.valueOf(voteEntity.vote) }.getOrNull()
                ?: return@mapNotNull null

            // Check if rebel: compare MP's vote against party majority
            val isRebel = checkIfRebel(divisionId, partyName, voteType)

            NewVote(
                memberId = memberId,
                memberName = memberName,
                thumbnailUrl = thumbnailUrl,
                partyName = partyName,
                divisionId = divisionId,
                house = division.house,
                divisionTitle = division.title,
                voteType = voteType,
                isRebel = isRebel,
            )
        }
    }

    /**
     * Check if the MP's vote is a rebellion (against party majority).
     */
    private suspend fun checkIfRebel(divisionId: Int, partyName: String, mpVote: VoteType): Boolean {
        if (mpVote == VoteType.NO_VOTE_RECORDED) return false
        val votes = divisionDao.getVotesForDivision(divisionId)
        val partyVotes = votes.filter { it.partyName.equals(partyName, ignoreCase = true) }
        if (partyVotes.isEmpty()) return false
        val ayes = partyVotes.count { it.vote == "AYE" }
        val noes = partyVotes.count { it.vote == "NO" }
        if (ayes == noes) return false // tie — no rebellion
        val partyMajority = if (ayes > noes) VoteType.AYE else VoteType.NO
        return mpVote != partyMajority
    }

    /**
     * Get all votes for a single division (suspend, one-shot).
     */
    suspend fun getVotesForDivision(divisionId: Int): List<DivisionVote> =
        divisionDao.getVotesForDivision(divisionId).map { it.toDomain() }

    private suspend fun getVotesForDivisionSync(divisionId: Int): List<DivisionVoteEntity> =
        divisionDao.observeVotesForDivision(divisionId).first()

    private fun DivisionEntity.toDomain(): Division =
        Division(
            id = id,
            title = title,
            date = date,
            number = number,
            ayeCount = ayeCount,
            noCount = noCount,
            isDeferred = isDeferred,
            house = house,
        )

    private fun DivisionVoteEntity.toDomain(): DivisionVote =
        DivisionVote(
            divisionId = divisionId,
            memberId = memberId,
            vote = VoteType.valueOf(vote),
            memberName = memberName,
            partyName = partyName,
            partyColour = partyColour,
            constituencyName = constituencyName,
            isTeller = isTeller,
        )

    private fun DivisionVote.toEntity(): DivisionVoteEntity =
        DivisionVoteEntity(
            divisionId = divisionId,
            memberId = memberId,
            vote = vote.name,
            memberName = memberName,
            partyName = partyName ?: "",
            partyColour = partyColour ?: "",
            constituencyName = constituencyName ?: "",
            isTeller = isTeller,
        )
}
