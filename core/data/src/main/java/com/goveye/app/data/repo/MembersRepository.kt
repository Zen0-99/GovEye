package com.goveye.app.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.goveye.app.data.api.MembersApi
import com.goveye.app.data.local.dao.ConstituencyElectionDao
import com.goveye.app.data.local.dao.HistoricalMemberDao
import com.goveye.app.data.local.dao.MpCareerEventDao
import com.goveye.app.data.local.dao.MpContactDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.MpExperienceDao
import com.goveye.app.data.local.dao.MpSynopsisDao
import com.goveye.app.data.local.dao.SearchDao
import com.goveye.app.data.local.entity.ConstituencyElectionCandidateEntity
import com.goveye.app.data.local.entity.ConstituencyElectionEntity
import com.goveye.app.data.local.entity.MpCareerEventEntity
import com.goveye.app.data.local.entity.MpContactEntity
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.MpExperienceEntity
import com.goveye.app.data.local.entity.MpSynopsisEntity
import com.goveye.app.data.mapper.MemberMapper
import com.goveye.app.domain.model.BiographyExperience
import com.goveye.app.domain.model.BiographyItem
import com.goveye.app.domain.model.CareerCategory
import com.goveye.app.domain.model.CareerEvent
import com.goveye.app.domain.model.ConstituencyElection
import com.goveye.app.domain.model.Contact
import com.goveye.app.domain.model.ElectionCandidate
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.MpElectionResults
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import com.goveye.app.domain.search.FtsQuerySanitizer
import com.goveye.app.domain.search.FuzzyMatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class MembersRepository @Inject constructor(
    private val mpDao: MpDao,
    private val searchDao: SearchDao,
    private val membersApi: MembersApi,
    private val mapper: MemberMapper,
    private val historicalMemberDao: HistoricalMemberDao,
    private val mpSynopsisDao: MpSynopsisDao,
    private val mpContactDao: MpContactDao,
    private val mpCareerEventDao: MpCareerEventDao,
    private val constituencyElectionDao: ConstituencyElectionDao,
    private val mpExperienceDao: MpExperienceDao
) {

    suspend fun getMpsByIds(ids: List<Int>): List<MpEntity> = mpDao.getMpsByIds(ids)

    fun observeAllMps(): Flow<RepositoryResult<List<Mp>>> = mpDao.observeAllMps().map { entities ->
        if (entities.isEmpty()) {
            RepositoryResult(emptyList(), SyncStatus.EMPTY)
        } else {
            RepositoryResult(entities.map { it.toDomain() }, SyncStatus.FRESH)
        }
    }

    fun observeMp(id: Int): Flow<RepositoryResult<Mp?>> = mpDao.observeMp(id).map { entity ->
        if (entity == null) {
            RepositoryResult(null, SyncStatus.EMPTY)
        } else {
            RepositoryResult(entity.toDomain(), SyncStatus.FRESH)
        }
    }

    /**
     * Fetches a member from the live Parliament API by ID.
     * Used as a fallback for members not in the bundled DB (e.g. Lords).
     * Returns null on network failure.
     */
    suspend fun fetchMemberFromApi(memberId: Int): Mp? = try {
        val response = membersApi.getMember(memberId)
        mapper.toDomain(response.value)
    } catch (e: Exception) {
        null
    }

    fun observePagedMps(): Flow<PagingData<Mp>> = Pager(
        config = PagingConfig(
            pageSize = 30,
            prefetchDistance = 15,
            initialLoadSize = 60,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { mpDao.pagingSource() }
    ).flow.map { pagingData ->
        pagingData.map { it.toDomain() }
    }

    /**
     * Local-first FTS search using Room's mps_fts table.
     * Searches across nameListAs, nameDisplayAs, constituencyName, and partyName.
     * Returns a reactive Flow that updates when the Room cache changes.
     *
     * Query sanitization: strips FTS special chars (*, ", `), then appends *
     * to each token for prefix matching. Empty/blank queries return an empty Flow.
     */
    fun searchMpsFts(query: String): Flow<List<Mp>> {
        val sanitized = FtsQuerySanitizer.sanitize(query) ?: return flowOf(emptyList())
        return searchDao.searchMpsFts(sanitized).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Combined search with FTS → LIKE → fuzzy fallback chain.
     *
     * Phase 1: FTS4 MATCH (fast, prefix matching via mps_fts + historical_members_fts4)
     * Phase 2: LIKE search (substring matching — catches cases FTS tokenization misses)
     * Phase 3: Fuzzy Levenshtein matching (typo tolerance — "Hamiltton" → "Hamilton")
     *
     * Returns current MPs first, then historical members.
     * Historical members are mapped to Mp domain objects with isActive=false.
     */
    fun searchAllMembersFts(query: String): Flow<List<Mp>> {
        val sanitized = FtsQuerySanitizer.sanitize(query) ?: return flowOf(emptyList())
        val rawQuery = query.trim()
        return flow {
            // Phase 1: FTS search (current MPs)
            var currentMps = searchDao.searchMpsFts(sanitized).first().map { it.toDomain() }

            // Phase 2: LIKE fallback if FTS returned nothing
            if (currentMps.isEmpty() && rawQuery.length >= 2) {
                currentMps = searchDao.searchMps(rawQuery).first().map { it.toDomain() }
            }

            // Phase 3: Fuzzy fallback if LIKE also returned nothing
            if (currentMps.isEmpty() && rawQuery.length >= 3) {
                currentMps = fuzzySearchMps(rawQuery)
            }

            // Search historical members (excluding current MPs)
            val currentIds = currentMps.map { it.id }.toSet()
            var historical = try {
                historicalMemberDao.search(sanitized)
                    .filter { it.parliamentMemberId == null || it.parliamentMemberId !in currentIds }
                    .take(50 - currentMps.size)
            } catch (e: Exception) {
                emptyList()
            }

            // Fuzzy fallback for historical members too
            if (historical.isEmpty() && currentMps.isEmpty() && rawQuery.length >= 3) {
                historical = try {
                    historicalMemberDao.getAll()
                        .filter { hm ->
                            FuzzyMatcher.matches(
                                rawQuery,
                                "${hm.displayName} ${hm.alternateNames ?: ""} ${hm.constituency ?: ""}"
                            )
                        }
                        .filter { it.parliamentMemberId == null || it.parliamentMemberId !in currentIds }
                        .take(50)
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val historicalMps = historical.map { it.toDomainMp() }
            emit(currentMps + historicalMps)
        }
    }

    /**
     * Fuzzy search: load all MPs and filter by Levenshtein distance.
     * Used as a final fallback when FTS and LIKE both return no results.
     */
    private suspend fun fuzzySearchMps(query: String): List<Mp> {
        val allMps = mpDao.getAllMps().map { it.toDomain() }
        return allMps
            .map { mp ->
                FuzzyMatcher.score(
                    query,
                    "${mp.nameListAs} ${mp.nameDisplayAs} ${mp.constituency?.name ?: ""} ${mp.party?.name ?: ""}"
                ) to
                    mp
            }
            .filter { it.first < Int.MAX_VALUE }
            .sortedBy { it.first }
            .take(50)
            .map { it.second }
    }

    /**
     * Find current MPs by constituency name (exact or partial match).
     * Used by postcode search: postcodes.io returns a constituency name,
     * and we look up the MP(s) for that constituency in the local DB.
     *
     * Returns a list because some constituency names from postcodes.io
     * might partially match multiple constituencies (unlikely but possible).
     */
    suspend fun searchMpsByConstituency(constituencyName: String): List<Mp> = try {
        mpDao.getMpsByConstituency(constituencyName).map { it.toDomain() }
    } catch (e: Exception) {
        // Fallback: use FTS search on the constituency name
        searchMpsFts(constituencyName).first()
    }

    /**
     * Maps a HistoricalMemberEntity to an Mp domain object.
     * Uses parliamentMemberId as the id (for navigation to profile).
     * isActive is false for historical members (they're former MPs).
     */
    private fun com.goveye.app.data.local.entity.HistoricalMemberEntity.toDomainMp(): Mp = Mp(
        id = parliamentMemberId ?: -twfyPersonId,
        nameListAs = displayName,
        nameDisplayAs = displayName,
        nameFullTitle = null,
        gender = null,
        party = party?.let {
            com.goveye.app.domain.model.Party(
                0,
                it,
                partyAbbreviation ?: it,
                partyColourHex ?: "#808080",
                "#FFFFFF"
            )
        },
        constituency = constituency?.let { com.goveye.app.domain.model.Constituency(0, it) },
        house = house,
        membershipStartDate = startDate,
        isActive = isCurrent,
        thumbnailUrl = if (parliamentMemberId != null) {
            "https://members-api.parliament.uk/api/Members/$parliamentMemberId/Portrait"
        } else {
            null
        }
    )

    /**
     * Distinct party names from active MPs — for the filter bottom sheet's Party section.
     */
    fun observeDistinctParties(): Flow<List<String>> = mpDao.observeDistinctParties()

    /**
     * Active parties with seat counts — for the Parties tab.
     */
    suspend fun getActiveParties(): List<com.goveye.app.data.local.dao.PartySummary> = mpDao.getActiveParties()

    // --- In-memory cached profile data (one-shot fetches) ---

    private val synopsisCache = mutableMapOf<Int, Pair<String, Long>>()
    private val contactCache = mutableMapOf<Int, Pair<List<Contact>, Long>>()
    private val experienceCache = mutableMapOf<Int, Pair<List<BiographyExperience>, Long>>()
    private val biographyCache = mutableMapOf<Int, Pair<List<BiographyItem>, Long>>()

    suspend fun getSynopsis(memberId: Int): String? {
        val cached = synopsisCache[memberId]
        if (cached != null && System.currentTimeMillis() - cached.second < CacheTtl.MPS_MS) {
            return cached.first
        }
        // Try bundled DB first — instant, no network
        val dbEntity = mpSynopsisDao.getByMpId(memberId)
        if (dbEntity != null && !dbEntity.synopsisText.isNullOrBlank()) {
            val synopsis = stripHtml(dbEntity.synopsisText)
            // Filter out Wikipedia disambiguation pages that were stored as
            // bios (e.g. "Jack Abbott may refer to..."). These are not real
            // biographies — return null so the BioSection is hidden.
            if (!isDisambiguationPage(synopsis)) {
                synopsisCache[memberId] = synopsis to System.currentTimeMillis()
                return synopsis
            }
        }
        // Fall back to live API if not in bundled DB (or if bundled text was
        // a disambiguation page — the live Parliament API synopsis is always
        // a real one-liner, never a disambiguation page)
        return try {
            val response = membersApi.getMemberSynopsis(memberId)
            val synopsis = stripHtml(response.value ?: "")
            if (synopsis.isNotBlank() && !isDisambiguationPage(synopsis)) {
                synopsisCache[memberId] = synopsis to System.currentTimeMillis()
                synopsis
            } else {
                cached?.first
            }
        } catch (e: Exception) {
            cached?.first
        }
    }

    suspend fun getContact(memberId: Int): List<Contact> {
        val cached = contactCache[memberId]
        if (cached != null && System.currentTimeMillis() - cached.second < CacheTtl.MPS_MS) {
            return cached.first
        }
        // Try bundled DB first — instant, no network
        val dbContacts = mpContactDao.getByMpId(memberId)
        if (dbContacts.isNotEmpty()) {
            val contacts = dbContacts.map { it.toDomain() }
            contactCache[memberId] = contacts to System.currentTimeMillis()
            return contacts
        }
        // Fall back to live API if not in bundled DB
        return try {
            val response = membersApi.getMemberContact(memberId)
            val contacts = response.value.map { mapper.toContactDomain(it) }
            contactCache[memberId] = contacts to System.currentTimeMillis()
            contacts
        } catch (e: Exception) {
            cached?.first ?: emptyList()
        }
    }

    suspend fun getExperience(memberId: Int): List<BiographyExperience> {
        val cached = experienceCache[memberId]
        if (cached != null && System.currentTimeMillis() - cached.second < CacheTtl.MPS_MS) {
            return cached.first
        }
        // Try bundled DB first — instant, no network
        val dbExperiences = mpExperienceDao.getByMpId(memberId)
        if (dbExperiences.isNotEmpty()) {
            val experiences = dbExperiences.map { it.toDomain() }
                .filter { it.title != null || it.organisation != null }
            experienceCache[memberId] = experiences to System.currentTimeMillis()
            return experiences
        }
        // Fall back to live API if not in bundled DB
        return try {
            val response = membersApi.getMemberExperience(memberId)
            val experiences = response.value.map { mapper.toExperienceDomain(it) }
                .filter { it.title != null || it.organisation != null }
            experienceCache[memberId] = experiences to System.currentTimeMillis()
            experiences
        } catch (e: Exception) {
            cached?.first ?: emptyList()
        }
    }

    private val careerEventCache = mutableMapOf<Int, Pair<List<CareerEvent>, Long>>()

    /**
     * Unified career timeline from the Parliament Biography API and
     * Wikipedia/Wikidata. Merges government posts, opposition posts,
     * other posts, committee memberships, representations, party
     * affiliations, house memberships, education, and occupations into
     * a single list sorted by start date (most recent first).
     *
     * If the DB table is empty for this MP (e.g. the seed DB hasn't been
     * rebuilt yet with Biography data), falls back to fetching from the
     * Parliament Biography API at runtime and caches the result to the DB
     * so subsequent loads are instant.
     */
    suspend fun getCareerEvents(memberId: Int): List<CareerEvent> {
        val cached = careerEventCache[memberId]
        if (cached != null && System.currentTimeMillis() - cached.second < CacheTtl.MPS_MS) {
            return cached.first
        }
        val entities = mpCareerEventDao.getByMpId(memberId)
        if (entities.isNotEmpty()) {
            val events = entities.map { it.toDomain() }
            careerEventCache[memberId] = events to System.currentTimeMillis()
            return events
        }
        // DB is empty — fetch from the Biography API and cache to DB
        val events = fetchCareerEventsFromApi(memberId)
        careerEventCache[memberId] = events to System.currentTimeMillis()
        return events
    }

    /**
     * Fetch career events from the Parliament Biography API, convert to
     * CareerEvent entities, upsert to the DB, and return as domain models.
     */
    private suspend fun fetchCareerEventsFromApi(memberId: Int): List<CareerEvent> = try {
        val response = membersApi.getMemberBiography(memberId)
        val v = response.value
        val now = System.currentTimeMillis()
        val entities = mutableListOf<MpCareerEventEntity>()

        fun addPosts(posts: List<com.goveye.app.data.dto.members.BiographyPostDto>, category: CareerCategory) {
            posts.forEach { post ->
                entities.add(
                    MpCareerEventEntity(
                        mpId = memberId,
                        category = category.apiName,
                        name = post.name ?: "Unknown",
                        house = post.house?.id,
                        startDate = post.startDate,
                        endDate = post.endDate,
                        additionalInfo = post.additionalInfo,
                        additionalInfoLink = post.additionalInfoLink,
                        constituencyName = if (category == CareerCategory.REPRESENTATION) post.name else null,
                        constituencyId = if (category == CareerCategory.REPRESENTATION) post.id else null,
                        source = "parliament",
                        lastUpdated = now
                    )
                )
            }
        }

        addPosts(v.governmentPosts, CareerCategory.GOVERNMENT_POST)
        addPosts(v.oppositionPosts, CareerCategory.OPPOSITION_POST)
        addPosts(v.otherPosts, CareerCategory.OTHER_POST)
        addPosts(v.committeeMemberships, CareerCategory.COMMITTEE)
        addPosts(v.representations, CareerCategory.REPRESENTATION)
        addPosts(v.partyAffiliations, CareerCategory.PARTY_AFFILIATION)
        addPosts(v.houseMemberships, CareerCategory.HOUSE_MEMBERSHIP)

        if (entities.isNotEmpty()) {
            mpCareerEventDao.upsertAll(entities)
        }

        entities.map { it.toDomain() }
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * Election results for the career tab (D-05): the latest result for the
     * MP's current seat plus every election they personally contested
     * (candidate.memberId linkage), each with its full candidate list.
     * Bundled data only — no API fallback (D-02/out of scope: live fetch).
     */
    suspend fun getElectionResults(memberId: Int, currentConstituencyId: Int?): MpElectionResults {
        suspend fun withCandidates(e: ConstituencyElectionEntity) = e.toDomain(
            constituencyElectionDao.getCandidatesForElection(e.constituencyId, e.electionId)
                .map { it.toDomain() }
        )

        val contested = constituencyElectionDao.getElectionsContestedByMember(memberId)
            .map { withCandidates(it) }

        val latest = currentConstituencyId
            ?.let { constituencyElectionDao.getElectionsForConstituency(it).firstOrNull() }
            ?.let { withCandidates(it) }

        return MpElectionResults(currentSeatLatest = latest, contested = contested)
    }

    suspend fun getBiography(memberId: Int): List<BiographyItem> {
        val cached = biographyCache[memberId]
        if (cached != null && System.currentTimeMillis() - cached.second < CacheTtl.MPS_MS) {
            return cached.first
        }
        return try {
            val response = membersApi.getMemberBiography(memberId)
            val v = response.value
            val biographies = (
                v.governmentPosts + v.oppositionPosts + v.otherPosts +
                    v.committeeMemberships + v.representations +
                    v.partyAffiliations + v.houseMemberships
                ).map { mapper.toBiographyDomain(it) }
            biographyCache[memberId] = biographies to System.currentTimeMillis()
            biographies
        } catch (e: Exception) {
            cached?.first ?: emptyList()
        }
    }

    private fun MpEntity.toDomain(): Mp = Mp(
        id = id,
        nameListAs = nameListAs,
        nameDisplayAs = nameDisplayAs,
        nameFullTitle = nameFullTitle,
        gender = gender,
        party = com.goveye.app.domain.model.Party(
            partyId,
            partyName,
            partyAbbreviation,
            partyBackgroundColour,
            partyForegroundColour
        ),
        constituency = com.goveye.app.domain.model.Constituency(constituencyId, constituencyName),
        house = house,
        membershipStartDate = membershipStartDate,
        isActive = isActive,
        thumbnailUrl = thumbnailUrl
    )

    private fun stripHtml(html: String): String = html
        .replace(Regex("<[^>]*>"), "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        .trim()

    /**
     * Detects Wikipedia disambiguation pages that were erroneously stored
     * as MP biographies. These typically start with "X may refer to:" and
     * list unrelated people/things. Returns true if the text looks like a
     * disambiguation page rather than a real biography.
     */
    private fun isDisambiguationPage(text: String): Boolean {
        val lower = text.lowercase()
        // "may refer to" is the standard Wikipedia disambiguation phrase
        if (lower.contains("may refer to")) return true
        // Some disambiguation pages use "usually refers to" or "can refer to"
        if (lower.contains("usually refers to") || lower.contains("can refer to")) return true
        return false
    }

    // --- Bundled DB entity → domain mappers ---

    private fun MpContactEntity.toDomain(): Contact = Contact(
        type = type,
        isPreferred = isPreferred,
        isWebAddress = isWebAddress,
        line1 = line1,
        line2 = line2,
        line3 = line3,
        line4 = line4,
        line5 = line5,
        postcode = postcode,
        phone = phone,
        email = email,
        website = website,
        openingHours = openingHours
    )

    private fun MpExperienceEntity.toDomain(): BiographyExperience = BiographyExperience(
        id = id,
        type = type,
        title = title,
        organisation = organisation,
        startMonth = startMonth,
        startYear = startYear,
        endMonth = endMonth,
        endYear = endYear
    )

    private fun MpCareerEventEntity.toDomain(): CareerEvent = CareerEvent(
        id = id,
        category = CareerCategory.fromApiName(category),
        name = name ?: "",
        house = house,
        startDate = startDate,
        endDate = endDate,
        additionalInfo = additionalInfo,
        additionalInfoLink = additionalInfoLink,
        constituencyName = constituencyName,
        constituencyId = constituencyId,
        source = source
    )

    private fun ConstituencyElectionCandidateEntity.toDomain(): ElectionCandidate = ElectionCandidate(
        rankOrder = rankOrder,
        memberId = memberId,
        name = name,
        partyId = partyId,
        partyName = partyName,
        partyAbbreviation = partyAbbreviation,
        partyColour = partyColour,
        resultChange = resultChange,
        votes = votes
    )

    private fun ConstituencyElectionEntity.toDomain(candidates: List<ElectionCandidate>): ConstituencyElection =
        ConstituencyElection(
            constituencyId = constituencyId,
            electionId = electionId,
            result = result,
            isNotional = isNotional,
            electorate = electorate,
            turnout = turnout,
            majority = majority,
            winningPartyId = winningPartyId,
            winningPartyName = winningPartyName,
            winningPartyColour = winningPartyColour,
            electionTitle = electionTitle,
            electionDate = electionDate,
            isGeneralElection = isGeneralElection,
            constituencyName = constituencyName,
            candidates = candidates
        )
}
