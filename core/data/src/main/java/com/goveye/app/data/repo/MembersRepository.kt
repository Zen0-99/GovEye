package com.goveye.app.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.goveye.app.data.api.MembersApi
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.SearchDao
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.mapper.MemberMapper
import com.goveye.app.domain.model.BiographyExperience
import com.goveye.app.domain.model.BiographyItem
import com.goveye.app.domain.model.Contact
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class MembersRepository @Inject constructor(
    private val mpDao: MpDao,
    private val searchDao: SearchDao,
    private val membersApi: MembersApi,
    private val mapper: MemberMapper
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
        val sanitized = sanitizeFtsQuery(query)
        return if (sanitized.isBlank()) {
            flowOf(emptyList())
        } else {
            searchDao.searchMpsFts(sanitized).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    /**
     * Sanitizes user input for FTS4 MATCH query.
     * Strategy A (RESEARCH.md §1.5): strip FTS special chars, prefix-match each token.
     * "Green Party" → "Green* Party*"
     */
    private fun sanitizeFtsQuery(input: String): String {
        return input.trim()
            .replace(Regex("[*\"`]"), "") // strip FTS special chars
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { "$it*" } // prefix match each token
    }

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
        return try {
            val response = membersApi.getMemberSynopsis(memberId)
            val synopsis = stripHtml(response.value ?: "")
            synopsisCache[memberId] = synopsis to System.currentTimeMillis()
            synopsis
        } catch (e: Exception) {
            cached?.first
        }
    }

    suspend fun getContact(memberId: Int): List<Contact> {
        val cached = contactCache[memberId]
        if (cached != null && System.currentTimeMillis() - cached.second < CacheTtl.MPS_MS) {
            return cached.first
        }
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
        return try {
            val response = membersApi.getMemberExperience(memberId)
            val experiences = response.value.map { mapper.toExperienceDomain(it) }
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
}
