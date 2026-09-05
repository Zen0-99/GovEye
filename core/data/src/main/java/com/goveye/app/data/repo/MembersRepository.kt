package com.goveye.app.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.goveye.app.data.api.MembersApi
import com.goveye.app.data.local.dao.HistoricalMemberDao
import com.goveye.app.data.local.dao.MpContactDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.MpExperienceDao
import com.goveye.app.data.local.dao.MpSynopsisDao
import com.goveye.app.data.local.dao.SearchDao
import com.goveye.app.data.local.entity.MpContactEntity
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.MpExperienceEntity
import com.goveye.app.data.local.entity.MpSynopsisEntity
import com.goveye.app.data.mapper.MemberMapper
import com.goveye.app.domain.model.BiographyExperience
import com.goveye.app.domain.model.BiographyItem
import com.goveye.app.domain.model.Contact
import com.goveye.app.domain.model.Mp
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
        isActive = isCurrent == 1,
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

    suspend fun getBiography(memberId: Int): List<BiographyItem> {
        val cached = biographyCache[memberId]
        if (cached != null && System.currentTimeMillis() - cached.second < CacheTtl.MPS_MS) {
            return cached.first
        }
        return try {
            val response = membersApi.getMemberBiography(memberId)
            val biographies = response.value.map { mapper.toBiographyDomain(it) }
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
}
