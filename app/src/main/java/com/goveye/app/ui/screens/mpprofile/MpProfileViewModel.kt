package com.goveye.app.ui.screens.mpprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.ExpenseBucketTotal
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.entity.BioDataEntity
import com.goveye.app.data.local.entity.ExpenseEntity
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.MpLinkEntity
import com.goveye.app.data.repo.BioDataRepository
import com.goveye.app.data.repo.CommitteesRepository
import com.goveye.app.data.repo.ExpensesRepository
import com.goveye.app.data.repo.FollowRepository
import com.goveye.app.data.repo.InterestsRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.MpLinksRepository
import com.goveye.app.data.repo.NotificationPreferenceRepository
import com.goveye.app.data.repo.StatsRepository
import com.goveye.app.data.repo.VotesRepository
import com.goveye.app.domain.model.BiographyExperience
import com.goveye.app.domain.model.Committee
import com.goveye.app.domain.model.Contact
import com.goveye.app.domain.model.DivisionVote
import com.goveye.app.domain.model.Interest
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.SyncStatus
import com.goveye.app.domain.stats.ActivityScore
import com.goveye.app.domain.stats.RebellionCalculator
import com.goveye.app.domain.stats.RebellionStats
import com.goveye.app.domain.stats.TraitBar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProfileUiState(
    val mp: Mp? = null,
    val synopsis: String? = null,
    val contacts: List<Contact> = emptyList(),
    val committees: List<Committee> = emptyList(),
    val experiences: List<BiographyExperience> = emptyList(),
    val samePartyMps: List<Mp> = emptyList(),
    val committeePeerMps: List<Mp> = emptyList(),
    val memberVotes: List<MemberVoteWithDivision> = emptyList(),
    val rebellionStats: RebellionStats? = null,
    val allDivisionDates: List<String> = emptyList(),
    val allVotesByDivision: Map<Int, List<DivisionVote>> = emptyMap(),
    val memberPartyName: String? = null,
    val interests: List<Interest> = emptyList(),
    val bioData: BioDataEntity? = null,
    val expenseBucketTotals: List<ExpenseBucketTotal> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val mpLinks: MpLinkEntity? = null,
    val activityScore: ActivityScore? = null,
    val traitBars: List<TraitBar> = emptyList(),
    val isFollowing: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val votesNotificationsEnabled: Boolean = false,
    val speechesNotificationsEnabled: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.EMPTY,
    val isLoading: Boolean = true,
    // Activity tab — paginated voting with search
    val activityVotes: List<MemberVoteWithDivision> = emptyList(),
    val activitySearchQuery: String = "",
    val activityIsLoadingMore: Boolean = false,
    val activityHasMore: Boolean = true,
    val activityTotalCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
    private val committeesRepository: CommitteesRepository,
    private val votesRepository: VotesRepository,
    private val followRepository: FollowRepository,
    private val notificationPrefRepository: NotificationPreferenceRepository,
    private val mpDao: MpDao,
    private val interestsRepository: InterestsRepository,
    private val bioDataRepository: BioDataRepository,
    private val expensesRepository: ExpensesRepository,
    private val mpLinksRepository: MpLinksRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(memberId: Int) {
        // Load the MP basic data first so the header renders immediately,
        // then load everything else in parallel and update the state in
        // a single batch. This prevents the section-by-section "trickle"
        // effect where each DB query triggers a separate recomposition.
        viewModelScope.launch {
            // 1. MP basic data — needed for the header, party color, etc.
            // Try bundled DB first; fall back to live API for Lords and other
            // members not in the Commons-only bundled directory.
            val mpResult = membersRepository.observeMp(memberId).first()
            val mp = mpResult.data ?: membersRepository.fetchMemberFromApi(memberId)
            _uiState.value = _uiState.value.copy(
                mp = mp,
                syncStatus = if (mp != null) SyncStatus.FRESH else SyncStatus.EMPTY,
                isLoading = false
            )

            // 2. Load everything else in parallel — each section updates
            //    the UI independently as soon as its data is ready, so the
            //    user sees content appear progressively instead of waiting
            //    for the slowest query.
            val house = mp?.house ?: 1
            val partyName = mp?.party?.name
            val partyId = mp?.party?.id

            coroutineScope {
                // Launch each load as an independent coroutine that updates
                // state on completion. Fast DB reads appear instantly; slow
                // API fallbacks trickle in without blocking the rest.
                launch {
                    val isFollowing = followRepository.observeIsFollowing(memberId).first()
                    _uiState.value = _uiState.value.copy(isFollowing = isFollowing)
                }
                launch {
                    val notifPref = notificationPrefRepository.observe(memberId).first()
                    _uiState.value = _uiState.value.copy(
                        notificationsEnabled = notifPref.notificationsEnabled,
                        votesNotificationsEnabled = notifPref.votesEnabled,
                        speechesNotificationsEnabled = notifPref.speechesEnabled
                    )
                }
                launch {
                    val synopsis = runCatching { membersRepository.getSynopsis(memberId) }.getOrNull()
                    _uiState.value = _uiState.value.copy(synopsis = synopsis)
                }
                launch {
                    val contacts = runCatching { membersRepository.getContact(memberId) }.getOrDefault(emptyList())
                    _uiState.value = _uiState.value.copy(contacts = contacts)
                }
                launch {
                    // bioData is a fast DB read — load it first so maiden speech
                    // and other stats appear instantly with the header.
                    val bioData = runCatching { bioDataRepository.getBioData(memberId) }.getOrNull()
                    _uiState.value = _uiState.value.copy(bioData = bioData)
                }
                launch {
                    // experiences may involve API fallback — load separately so
                    // it doesn't delay bioData (maiden speech, etc.)
                    // Re-fetch bioData for the merge (cheap DB read, avoids race)
                    val bioData = runCatching { bioDataRepository.getBioData(memberId) }.getOrNull()
                    val experiences = runCatching {
                        membersRepository.getExperience(memberId)
                    }.getOrDefault(emptyList())
                    val merged = mergeExperiencesWithMnisPosts(experiences, bioData)
                    _uiState.value = _uiState.value.copy(experiences = merged)
                }
                launch {
                    val mpLinks = runCatching { mpLinksRepository.getLinks(memberId) }.getOrNull()
                    _uiState.value = _uiState.value.copy(mpLinks = mpLinks)
                }
                launch {
                    val committees = committeesRepository.observeCommitteesForMember(memberId).first()
                    _uiState.value = _uiState.value.copy(committees = committees.data)
                }
                launch {
                    val samePartyMps = partyId?.let {
                        mpDao.getMpsByParty(it, memberId).map { it.toDomainMp() }
                    } ?: emptyList()
                    _uiState.value = _uiState.value.copy(samePartyMps = samePartyMps)
                }
                launch {
                    val votesResult = runCatching {
                        val allDates = votesRepository.getAllDivisionDates(house)
                        val votes = votesRepository.getMemberVotingWithDivisions(memberId)
                        android.util.Log.i(
                            "GovEye/Profile",
                            "Loaded ${votes.size} votes for MP $memberId (house=$house)"
                        )
                        Triple(allDates, votes, votes.isNotEmpty() && partyName != null)
                    }.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        allDivisionDates = votesResult?.first ?: emptyList(),
                        memberVotes = votesResult?.second ?: emptyList(),
                        memberPartyName = partyName
                    )
                    // Rebellion stats depend on votes being loaded
                    if (votesResult?.third == true) {
                        val rebellionStats = runCatching {
                            val memberVotes = votesRepository.getMemberVotes(memberId)
                            val divisionIds = memberVotes.map { it.divisionId }.distinct()
                            val partyVoteCounts = votesRepository.getPartyVoteCounts(divisionIds, partyName!!)
                            RebellionCalculator.computeAggregated(memberVotes, partyVoteCounts)
                        }.getOrNull()
                        _uiState.value = _uiState.value.copy(rebellionStats = rebellionStats)
                    }
                }
                launch {
                    val stats = runCatching {
                        statsRepository.getActivityScore(memberId, house, partyName) to
                            statsRepository.getTraitBars(memberId, house, partyName)
                    }.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        activityScore = stats?.first,
                        traitBars = stats?.second ?: emptyList()
                    )
                }
                launch {
                    val interests = interestsRepository.observeInterestsForMember(memberId).first()
                    _uiState.value = _uiState.value.copy(interests = interests.data)
                }
                launch {
                    val bucketTotals = runCatching {
                        expensesRepository.getBucketTotals(memberId)
                    }.getOrDefault(emptyList())
                    _uiState.value = _uiState.value.copy(expenseBucketTotals = bucketTotals)
                }
                launch {
                    val expenses = runCatching {
                        expensesRepository.getExpenses(memberId)
                    }.getOrDefault(emptyList())
                    _uiState.value = _uiState.value.copy(expenses = expenses)
                }
            }

            // 3. Load first page of activity votes (separate — paginated)
            loadActivityVotes(memberId)
        }
    }

    /**
     * Merge MNIS government/opposition posts into the existing career timeline (D-02).
     *
     * Parses postsJson (JSON array of {type, title, department, startDate, endDate}),
     * converts each to a BiographyExperience, merges with existing experiences,
     * and sorts by startYear descending.
     */
    private fun mergeExperiencesWithMnisPosts(
        experiences: List<BiographyExperience>,
        bioData: BioDataEntity?
    ): List<BiographyExperience> {
        if (bioData?.postsJson == null) return experiences

        val mnisPosts = try {
            val posts = org.json.JSONArray(bioData.postsJson)
            (0 until posts.length()).map { i ->
                val post = posts.getJSONObject(i)
                val startDate = post.optStringOrNull("startDate")
                val endDate = post.optStringOrNull("endDate")
                BiographyExperience(
                    id = -(i + 1), // Negative IDs to avoid collisions with API experiences
                    type = post.optStringOrNull("type"),
                    title = post.optStringOrNull("title"),
                    organisation = post.optStringOrNull("department"),
                    startMonth = startDate?.substring(5, 7)?.toIntOrNull(),
                    startYear = startDate?.substring(0, 4)?.toIntOrNull(),
                    endMonth = endDate?.substring(5, 7)?.toIntOrNull(),
                    endYear = endDate?.substring(0, 4)?.toIntOrNull()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        return (experiences + mnisPosts).sortedByDescending { it.startYear ?: 0 }
    }

    /** Safely get an optional string from a JSONObject, returning null if missing, empty, or JSON null. */
    private fun org.json.JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key)
        return value.ifEmpty { null }
    }

    private fun loadSamePartyMps(memberId: Int, partyId: Int?) {
        if (partyId == null) return
        viewModelScope.launch {
            val entities = mpDao.getMpsByParty(partyId, memberId)
            val mps = entities.map { it.toDomainMp() }
            _uiState.value = _uiState.value.copy(samePartyMps = mps)
        }
    }

    /**
     * No-op — the bundled DB is the source of truth, updated via patches.
     * Kept for pull-to-refresh UI compatibility; the observe flows re-emit
     * on their own when the DB changes.
     */
    fun refresh(memberId: Int) {
        // No-op — DB is pre-populated and updated via patches (D-09, D-10a)
    }

    // --- Activity tab — paginated voting with search ---

    private val activityPageSize = 30

    /**
     * Load the first page of activity votes. Called when the Activity tab is first shown.
     * If a search query is active, searches by division title; otherwise loads most recent.
     */
    fun loadActivityVotes(memberId: Int) {
        viewModelScope.launch {
            val query = _uiState.value.activitySearchQuery
            val (votes, total) = if (query.isBlank()) {
                votesRepository.getPagedMemberVoting(memberId, activityPageSize, 0) to
                    votesRepository.countVotesForMember(memberId)
            } else {
                votesRepository.searchPagedMemberVoting(memberId, query, activityPageSize, 0) to
                    votesRepository.countSearchVotesForMember(memberId, query)
            }
            _uiState.value = _uiState.value.copy(
                activityVotes = votes,
                activityTotalCount = total,
                activityHasMore = votes.size < total,
                activityIsLoadingMore = false
            )
        }
    }

    /**
     * Load the next page of activity votes (infinite scroll).
     */
    fun loadMoreActivityVotes(memberId: Int) {
        val state = _uiState.value
        if (state.activityIsLoadingMore || !state.activityHasMore) return
        _uiState.value = state.copy(activityIsLoadingMore = true)
        viewModelScope.launch {
            val offset = state.activityVotes.size
            val query = state.activitySearchQuery
            val more = if (query.isBlank()) {
                votesRepository.getPagedMemberVoting(memberId, activityPageSize, offset)
            } else {
                votesRepository.searchPagedMemberVoting(memberId, query, activityPageSize, offset)
            }
            _uiState.value = _uiState.value.copy(
                activityVotes = state.activityVotes + more,
                activityHasMore = (offset + more.size) < state.activityTotalCount,
                activityIsLoadingMore = false
            )
        }
    }

    /**
     * Update the activity search query and reload from page 0.
     */
    fun updateActivitySearchQuery(memberId: Int, query: String) {
        _uiState.value = _uiState.value.copy(activitySearchQuery = query)
        loadActivityVotes(memberId)
    }

    fun toggleFollow(memberId: Int) {
        viewModelScope.launch {
            if (_uiState.value.isFollowing) {
                followRepository.unfollow(memberId)
            } else {
                followRepository.follow(memberId)
                // Auto-enable vote notifications when following (FotMob behavior)
                notificationPrefRepository.setVotesEnabled(memberId, true)
            }
        }
    }

    // --- Notification preferences ---

    fun setNotificationsEnabled(memberId: Int, enabled: Boolean) {
        viewModelScope.launch {
            notificationPrefRepository.setNotificationsEnabled(memberId, enabled)
        }
    }

    fun setVotesNotificationsEnabled(memberId: Int, enabled: Boolean) {
        viewModelScope.launch {
            notificationPrefRepository.setVotesEnabled(memberId, enabled)
        }
    }

    fun setSpeechesNotificationsEnabled(memberId: Int, enabled: Boolean) {
        viewModelScope.launch {
            notificationPrefRepository.setSpeechesEnabled(memberId, enabled)
        }
    }

    private fun MpEntity.toDomainMp(): Mp = Mp(
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
}
