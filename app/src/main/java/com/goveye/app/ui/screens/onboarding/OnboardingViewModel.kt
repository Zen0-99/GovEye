package com.goveye.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.PartySummary
import com.goveye.app.data.local.dao.TagDao
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.preference.OnboardingPreferences
import com.goveye.app.data.repo.FollowRepository
import com.goveye.app.data.repo.GovernmentAnnouncementsRepository
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.MpTag
import com.goveye.app.domain.model.PartyLeader
import com.goveye.app.domain.model.SourceRecommendation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the 5-step onboarding redesign (Phase 14).
 *
 * Manages all selection state across the 5 steps:
 * - Step 1: Government (existing — selectedGov stored in OnboardingScreen)
 * - Step 2: Tags (selectedTags)
 * - Step 3: Sources (selectedSources — "{orgSlug}:{streamType}" pairs)
 * - Step 4: Parties (selectedParties — partyIds)
 * - Step 5: MPs (followedMpIds — via FollowRepository)
 *
 * Reads precomputed tables from 14-02/14-03:
 * - source_recommendations (D-06 hybrid tag→department mapping)
 * - mp_tags (D-08 recency-weighted tag hits)
 * - party_leaders (D-07 precomputed party leaders)
 *
 * Seed download runs in background throughout all 5 steps (D-02) — the
 * ViewModel works with whatever data is available from the BundledDatabase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnboardingViewModel
@Inject
constructor(
    private val governmentAnnouncementsRepository: GovernmentAnnouncementsRepository,
    private val followRepository: FollowRepository,
    private val onboardingPreferences: OnboardingPreferences,
    private val tagDao: TagDao,
    private val mpDao: MpDao
) : ViewModel() {

    // --- Selection state (in-memory, persisted on finish) ---

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    private val _selectedSources = MutableStateFlow<Set<String>>(emptySet())
    val selectedSources: StateFlow<Set<String>> = _selectedSources.asStateFlow()

    private val _selectedParties = MutableStateFlow<Set<Int>>(emptySet())
    val selectedParties: StateFlow<Set<Int>> = _selectedParties.asStateFlow()

    private val _followedMpIds = MutableStateFlow<Set<Int>>(emptySet())
    val followedMpIds: StateFlow<Set<Int>> = _followedMpIds.asStateFlow()

    // --- Available data (from BundledDatabase, populated by seed download) ---

    /** All 26 tags from TAG_DICTIONARY (via TagDao.getAllTags UNION query). */
    val availableTags: StateFlow<List<String>> =
        flow {
            try {
                val dbTags = tagDao.getAllTags()
                // Fall back to the static list if the DB is empty (first launch
                // before seed download completes) — onboarding must always show
                // all 26 tags so the user can pick topics.
                emit(if (dbTags.isEmpty()) ALL_TAG_NAMES else dbTags)
            } catch (e: Exception) {
                emit(ALL_TAG_NAMES)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ALL_TAG_NAMES)

    /** All source recommendations (D-06 hybrid tag→department mapping). */
    val allRecommendations: StateFlow<List<SourceRecommendation>> =
        governmentAnnouncementsRepository
            .observeAllRecommendations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Recommended departments derived from selected tags (D-05, D-06). */
    val recommendedDepartments: StateFlow<List<RecommendedDepartment>> =
        combine(_selectedTags, allRecommendations) { tags, recs ->
            SourceRecommendationHelper.getRecommendedDepartments(tags, recs)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All department-stream combinations for the "All sources" section. */
    val allDepartments: StateFlow<List<DepartmentGroup>> =
        flow {
            try {
                // Read distinct orgs from government_publications if DB is populated
                val pubs = governmentAnnouncementsRepository.observePublications(500)
                pubs.collect { publications ->
                    val orgs = publications
                        .map { it.organisationSlug to it.organisation }
                        .distinct()
                        .sortedBy { it.second }
                    emit(
                        SourceRecommendationHelper.getAllSourcesFromDb(orgs)
                    )
                    return@collect
                }
            } catch (e: Exception) {
                emit(SourceRecommendationHelper.getAllSources())
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SourceRecommendationHelper.getAllSources()
        )

    /** Active parties with seat counts (from MpDao). Falls back to a
     *  static list when the DB is empty (first launch before seed download). */
    val parties: StateFlow<List<PartyInfo>> =
        flow {
            try {
                val summaries = mpDao.getActiveParties()
                emit(if (summaries.isEmpty()) emptyList() else summaries.map { it.toPartyInfo() })
            } catch (e: Exception) {
                emit(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Party leaders from precomputed party_leaders table (D-07). */
    val partyLeaders: StateFlow<List<PartyLeader>> =
        governmentAnnouncementsRepository
            .observePartyLeaders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Party leader info with MP details resolved from MpDao (D-07). */
    val partyLeaderInfos: StateFlow<List<PartyLeaderInfo>> =
        partyLeaders
            .map { leaders ->
                if (leaders.isEmpty()) {
                    emptyList()
                } else {
                    val mps = mpDao.getMpsByIds(leaders.map { it.memberId })
                    val mpsById = mps.associateBy { it.id }
                    leaders.mapNotNull { leader ->
                        val mp = mpsById[leader.memberId] ?: return@mapNotNull null
                        PartyLeaderInfo(
                            memberId = leader.memberId,
                            name = mp.nameDisplayAs,
                            partyAbbreviation = mp.partyAbbreviation,
                            partyBackgroundColour = mp.partyBackgroundColour,
                            title = leader.title
                        )
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All mp_tags rows (D-08 recency-weighted). */
    val allMpTags: StateFlow<List<MpTag>> =
        governmentAnnouncementsRepository
            .observeAllMpTagRows()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Recommended MPs derived from selected tags + mp_tags (D-08, D-09). */
    val recommendedMps: StateFlow<List<RecommendedMp>> =
        combine(_selectedTags, allMpTags, partyLeaders) { tags, mpTags, leaders ->
            MpCurationHelper.getRecommendedMps(tags, mpTags, leaders)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Recommended MPs with MP details resolved from MpDao (for the MPs step). */
    val recommendedMpDetails: StateFlow<List<Mp>> =
        recommendedMps
            .map { recs ->
                if (recs.isEmpty()) {
                    emptyList()
                } else {
                    val mps = mpDao.getMpsByIds(recs.map { it.memberId })
                    val mpsById = mps.associateBy { it.id }
                    // Preserve the recommended order (sorted by score)
                    recs.mapNotNull { rec -> mpsById[rec.memberId]?.toDomain() }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Paged MP list for the "All MPs" section (reuses MpDao.pagingSource). */
    val pagedMps: Flow<PagingData<Mp>> = Pager(
        config = PagingConfig(
            pageSize = 30,
            prefetchDistance = 15,
            initialLoadSize = 60,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { mpDao.pagingSource() }
    ).flow
        .map { pagingData -> pagingData.map { it.toDomain() } }
        .cachedIn(viewModelScope)

    // --- Toggle functions ---

    fun toggleTag(tag: String) {
        val current = _selectedTags.value.toMutableSet()
        if (tag in current) current.remove(tag) else current.add(tag)
        _selectedTags.value = current
    }

    fun toggleSource(source: String) {
        val current = _selectedSources.value.toMutableSet()
        if (source in current) current.remove(source) else current.add(source)
        _selectedSources.value = current
    }

    fun toggleParty(partyId: Int) {
        val current = _selectedParties.value.toMutableSet()
        if (partyId in current) current.remove(partyId) else current.add(partyId)
        _selectedParties.value = current
    }

    fun toggleFollowMp(memberId: Int) {
        val current = _followedMpIds.value.toMutableSet()
        if (memberId in current) {
            current.remove(memberId)
            viewModelScope.launch { followRepository.unfollow(memberId) }
        } else {
            current.add(memberId)
            viewModelScope.launch { followRepository.follow(memberId) }
        }
        _followedMpIds.value = current
    }

    // --- Persistence ---

    /**
     * Persists all onboarding selections to DataStore before onComplete fires.
     * Followed MPs are already persisted to LocalDatabase via FollowRepository
     * on each toggle — no extra persistence needed for follows.
     */
    fun persistSelections() {
        viewModelScope.launch {
            onboardingPreferences.setSelectedTags(_selectedTags.value)
            onboardingPreferences.setSelectedSources(_selectedSources.value)
            onboardingPreferences.setSelectedParties(_selectedParties.value)
        }
    }

    // --- Mappers ---

    private fun MpEntity.toDomain(): Mp = Mp(
        id = id,
        nameListAs = nameListAs,
        nameDisplayAs = nameDisplayAs,
        nameFullTitle = nameFullTitle,
        gender = gender,
        party = com.goveye.app.domain.model.Party(
            id = partyId,
            name = partyName,
            abbreviation = partyAbbreviation,
            backgroundColour = partyBackgroundColour,
            foregroundColour = partyForegroundColour
        ),
        constituency = com.goveye.app.domain.model.Constituency(
            id = constituencyId,
            name = constituencyName
        ),
        house = house,
        membershipStartDate = membershipStartDate,
        isActive = isActive,
        thumbnailUrl = thumbnailUrl
    )

    private fun PartySummary.toPartyInfo(): PartyInfo = PartyInfo(
        partyId = partyId,
        partyName = partyName,
        partyAbbreviation = partyAbbreviation,
        partyBackgroundColour = partyBackgroundColour,
        seatCount = seats
    )
}
