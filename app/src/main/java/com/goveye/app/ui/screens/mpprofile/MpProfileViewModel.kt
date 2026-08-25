package com.goveye.app.ui.screens.mpprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.ExpenseBucketTotal
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.TagDao
import com.goveye.app.data.local.entity.BioDataEntity
import com.goveye.app.data.local.entity.ExpenseEntity
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.MpLinkEntity
import com.goveye.app.data.preference.ActivityFilterPreferences
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
import com.goveye.app.data.repo.WrittenQuestionsRepository
import com.goveye.app.domain.model.ActivityEntry
import com.goveye.app.domain.model.ActivityEntryType
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
import java.time.LocalDate
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
    val incomeEnabled: Boolean = false,
    val expensesEnabled: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.EMPTY,
    val isLoading: Boolean = true,
    // Activity tab — mixed chronological feed (D-01)
    val activityEntries: List<ActivityEntry> = emptyList(),
    val activityEnabledTypes: Set<ActivityEntryType> = ActivityEntryType.entries.toSet(),
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
    private val statsRepository: StatsRepository,
    private val writtenQuestionsRepository: WrittenQuestionsRepository,
    private val activityFilterPreferences: ActivityFilterPreferences,
    private val tagDao: TagDao
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
                        speechesNotificationsEnabled = notifPref.speechesEnabled,
                        incomeEnabled = notifPref.incomeEnabled,
                        expensesEnabled = notifPref.expensesEnabled
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

            // 3. Load the mixed activity feed (votes, questions, income, expenses, committee, career)
            loadActivityFeed(memberId)
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

    // --- Activity tab — mixed chronological feed (D-01) ---

    /**
     * Load all activity types and merge into a chronological feed.
     *
     * Loads votes, written questions, income declarations, expense claims,
     * committee memberships, and career milestones in parallel, then merges
     * them into a single list sorted by date descending. Applies the 6-month
     * activity window (D-09) and the activity type filter (D-05, D-06).
     */
    fun loadActivityFeed(memberId: Int) {
        viewModelScope.launch {
            // Load persisted filter preferences
            val enabledTypeStrings = activityFilterPreferences.enabledActivityTypes.first()
            val enabledTypes = enabledTypeStrings.mapNotNull { name ->
                runCatching { ActivityEntryType.valueOf(name) }.getOrNull()
            }.toSet()
            _uiState.value = _uiState.value.copy(activityEnabledTypes = enabledTypes)

            val sixMonthsAgo = LocalDate.now().minusMonths(6).toString()
            val state = _uiState.value

            coroutineScope {
                val votesDeferred = async { loadVoteEntries(memberId, sixMonthsAgo, state.memberVotes) }
                val questionsDeferred = async { loadQuestionEntries(memberId, sixMonthsAgo) }
                val incomeDeferred = async { loadIncomeEntries(memberId, sixMonthsAgo, state.interests) }
                val expenseDeferred = async { loadExpenseEntries(memberId, sixMonthsAgo, state.expenses) }
                val committeeDeferred = async { loadCommitteeEntries(memberId, sixMonthsAgo, state.committees) }
                val careerDeferred = async { loadCareerEntries(memberId, sixMonthsAgo, state.bioData) }
                val speechDeferred = async { loadSpeechEntries(memberId, sixMonthsAgo) }

                val allEntries = (
                    votesDeferred.await() +
                        questionsDeferred.await() +
                        incomeDeferred.await() +
                        expenseDeferred.await() +
                        committeeDeferred.await() +
                        careerDeferred.await() +
                        speechDeferred.await()
                    )
                    .filter { it.date.take(10) >= sixMonthsAgo }
                    .filter { it.entryType in enabledTypes }
                    .sortedByDescending { it.date }

                _uiState.value = _uiState.value.copy(
                    activityEntries = allEntries,
                    activityTotalCount = allEntries.size
                )
            }
        }
    }

    private fun loadVoteEntries(
        memberId: Int,
        sixMonthsAgo: String,
        memberVotes: List<MemberVoteWithDivision>
    ): List<ActivityEntry> = memberVotes
        .filter { it.divisionDate.take(10) >= sixMonthsAgo }
        .map { vote ->
            ActivityEntry(
                entryType = ActivityEntryType.VOTE,
                id = "vote_${vote.divisionId}",
                date = vote.divisionDate,
                summary = vote.divisionTitle,
                divisionTitle = vote.divisionTitle,
                divisionId = vote.divisionId,
                voteResult = when (vote.vote) {
                    com.goveye.app.domain.model.VoteType.AYE -> "Aye"
                    com.goveye.app.domain.model.VoteType.NO -> "No"
                    com.goveye.app.domain.model.VoteType.NO_VOTE_RECORDED -> "—"
                },
                house = vote.house
            )
        }

    private suspend fun loadQuestionEntries(memberId: Int, sixMonthsAgo: String): List<ActivityEntry> = runCatching {
        writtenQuestionsRepository.getQuestionsByMemberAndDateRange(memberId, sixMonthsAgo)
            .map { q ->
                ActivityEntry(
                    entryType = ActivityEntryType.QUESTION,
                    id = "question_${q.id}",
                    date = q.dateTabled,
                    summary = q.questionText.take(100),
                    questionText = q.questionText,
                    answeringBodyName = q.answeringBodyName,
                    uin = q.uin
                )
            }
    }.getOrDefault(emptyList())

    private suspend fun loadIncomeEntries(
        memberId: Int,
        sixMonthsAgo: String,
        interests: List<Interest>
    ): List<ActivityEntry> = runCatching {
        interests
            .filter { it.registrationDate != null && it.registrationDate!!.take(10) >= sixMonthsAgo }
            .map { interest ->
                ActivityEntry(
                    entryType = ActivityEntryType.INCOME,
                    id = "income_${interest.id}",
                    date = interest.registrationDate!!,
                    summary = interest.summary,
                    categoryName = interest.categoryName,
                    amountPence = interest.parsedAmountPence,
                    bucket = interest.bucket,
                    donorName = interest.donorName,
                    paymentType = interest.paymentType,
                    paymentDescription = interest.paymentDescription,
                    donorStatus = interest.donorStatus,
                    donorAddress = interest.donorAddress,
                    donorCompanyIdentifier = interest.donorCompanyIdentifier,
                    destination = interest.destination,
                    visitPurpose = interest.visitPurpose,
                    organisationName = interest.organisationName,
                    organisationDescription = interest.organisationDescription,
                    propertyLocation = interest.propertyLocation,
                    propertyType = interest.propertyType,
                    hoursWorked = interest.hoursWorked,
                    familyMemberName = interest.familyMemberName,
                    familyMemberRelationship = interest.familyMemberRelationship,
                    familyMemberRole = interest.familyMemberRole
                )
            }
    }.getOrDefault(emptyList())

    private suspend fun loadExpenseEntries(
        memberId: Int,
        sixMonthsAgo: String,
        expenses: List<ExpenseEntity>
    ): List<ActivityEntry> = runCatching {
        // Group by bucket + month (from claimDate), aggregate total + count (D-11)
        // claimDate is in DD/MM/YYYY format — normalize to YYYY-MM-DD for comparison
        // and YYYY-MM for grouping
        expenses
            .mapNotNull { e ->
                val isoDate = normalizeDdMmYyyyToIso(e.claimDate) ?: return@mapNotNull null
                Triple(e, isoDate, isoDate.take(7)) // (expense, ISO date, YYYY-MM)
            }
            .filter { (_, isoDate, _) -> isoDate >= sixMonthsAgo }
            .groupBy { (e, _, month) -> e.bucket to month }
            .map { (key, triples) ->
                val (bucket, month) = key
                val total = triples.sumOf { it.first.amountPence }
                // Use the last day of the month as the event date
                val monthEndDate = monthEndDate(month)
                ActivityEntry(
                    entryType = ActivityEntryType.EXPENSE,
                    id = "expense_${bucket}_$month",
                    date = monthEndDate,
                    summary = "$bucket: £${total / 100}",
                    bucketLabel = bucket,
                    totalAmountPence = total,
                    claimCount = triples.size
                )
            }
    }.getOrDefault(emptyList())

    /**
     * Convert DD/MM/YYYY to YYYY-MM-DD. Returns null if the input doesn't match.
     */
    private fun normalizeDdMmYyyyToIso(date: String?): String? {
        if (date == null) return null
        val parts = date.split("/")
        if (parts.size != 3) return null
        val (day, month, year) = parts
        return "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
    }

    private fun monthEndDate(ym: String): String = try {
        val parts = ym.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val lastDay = java.time.YearMonth.of(year, month).lengthOfMonth()
        "%04d-%02d-%02d".format(year, month, lastDay)
    } catch (e: Exception) {
        ym
    }

    private suspend fun loadCommitteeEntries(
        memberId: Int,
        sixMonthsAgo: String,
        committees: List<Committee>
    ): List<ActivityEntry> = runCatching {
        committees.flatMap { committee ->
            val entries = mutableListOf<ActivityEntry>()
            // Join event on committee startDate (or lastUpdated if no startDate)
            val joinDate = committee.startDate
            if (joinDate != null && joinDate.take(10) >= sixMonthsAgo) {
                entries.add(
                    ActivityEntry(
                        entryType = ActivityEntryType.COMMITTEE,
                        id = "committee_join_${committee.id}",
                        date = joinDate,
                        summary = committee.name,
                        committeeName = committee.name,
                        isJoin = true
                    )
                )
            }
            // Leave event if committee is inactive and has an endDate
            if (!committee.isActive && committee.endDate != null && committee.endDate!!.take(10) >= sixMonthsAgo) {
                entries.add(
                    ActivityEntry(
                        entryType = ActivityEntryType.COMMITTEE,
                        id = "committee_leave_${committee.id}",
                        date = committee.endDate!!,
                        summary = committee.name,
                        committeeName = committee.name,
                        isJoin = false
                    )
                )
            }
            entries
        }
    }.getOrDefault(emptyList())

    private suspend fun loadCareerEntries(
        memberId: Int,
        sixMonthsAgo: String,
        bioData: BioDataEntity?
    ): List<ActivityEntry> = runCatching {
        if (bioData == null) return@runCatching emptyList<ActivityEntry>()
        val entries = mutableListOf<ActivityEntry>()

        // Parse postsJson — government/opposition posts (D-04)
        if (bioData.postsJson != null) {
            try {
                val posts = org.json.JSONArray(bioData.postsJson)
                (0 until posts.length()).forEach { i ->
                    val post = posts.getJSONObject(i)
                    val startDate = post.optStringOrNull("startDate")
                    if (startDate != null && startDate.take(10) >= sixMonthsAgo) {
                        entries.add(
                            ActivityEntry(
                                entryType = ActivityEntryType.CAREER,
                                id = "career_post_$i",
                                date = startDate,
                                summary = post.optStringOrNull("title") ?: "",
                                roleTitle = post.optStringOrNull("title"),
                                contextLine = post.optStringOrNull("department"),
                                milestoneType = "POST"
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Malformed JSON — skip
            }
        }

        // Maiden speech event
        if (bioData.maidenSpeechDate != null && bioData.maidenSpeechDate!!.take(10) >= sixMonthsAgo) {
            entries.add(
                ActivityEntry(
                    entryType = ActivityEntryType.CAREER,
                    id = "career_maiden",
                    date = bioData.maidenSpeechDate!!,
                    summary = "Maiden Speech",
                    roleTitle = "Maiden Speech",
                    contextLine = "First speech in Parliament",
                    milestoneType = "MAIDEN_SPEECH"
                )
            )
        }

        // Parse honoursJson
        if (bioData.honoursJson != null) {
            try {
                val honours = org.json.JSONArray(bioData.honoursJson)
                (0 until honours.length()).forEach { i ->
                    val honour = honours.getJSONObject(i)
                    val date = honour.optStringOrNull("date")
                    if (date != null && date.take(10) >= sixMonthsAgo) {
                        entries.add(
                            ActivityEntry(
                                entryType = ActivityEntryType.CAREER,
                                id = "career_honour_$i",
                                date = date,
                                summary = honour.optStringOrNull("title") ?: "Honour",
                                roleTitle = honour.optStringOrNull("title"),
                                contextLine = honour.optStringOrNull("type"),
                                milestoneType = "HONOUR"
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Malformed JSON — skip
            }
        }

        entries
    }.getOrDefault(emptyList())

    private suspend fun loadSpeechEntries(memberId: Int, sixMonthsAgo: String): List<ActivityEntry> = runCatching {
        val speeches = votesRepository.getSpeechesByMember(memberId, 20)
        speeches
            .filter { it.divisionDate.take(10) >= sixMonthsAgo }
            .map { speech ->
                val tags = runCatching { tagDao.getTagsForDivision(speech.divisionId) }.getOrDefault(emptyList())
                ActivityEntry(
                    entryType = ActivityEntryType.SPEECH,
                    id = "speech_${speech.divisionId}_${speech.memberId}",
                    date = speech.divisionDate,
                    summary = speech.speechText.take(100),
                    divisionId = speech.divisionId,
                    divisionTitle = speech.divisionTitle,
                    speechText = speech.speechText,
                    speechTags = tags
                )
            }
    }.getOrDefault(emptyList())

    /**
     * Toggle an activity type in the filter and persist via DataStore (D-05, D-06).
     * Re-filters the already-loaded entries — no need to re-fetch.
     */
    fun toggleActivityFilter(memberId: Int, type: ActivityEntryType) {
        val current = _uiState.value.activityEnabledTypes
        val newTypes = if (type in current) current - type else current + type
        _uiState.value = _uiState.value.copy(activityEnabledTypes = newTypes)
        viewModelScope.launch {
            activityFilterPreferences.setEnabledActivityTypes(newTypes.map { it.name }.toSet())
            // Re-filter from already-loaded data
            val sixMonthsAgo = LocalDate.now().minusMonths(6).toString()
            val allEntries = _uiState.value.activityEntries
            // Re-apply filter: we need to re-derive from the full set, but since
            // activityEntries is already filtered, we need to reload from source.
            // Instead, reload the feed which re-fetches and re-filters.
            reloadFilteredEntries(memberId, newTypes, sixMonthsAgo)
        }
    }

    /**
     * Clear the activity filter — re-enable all 6 types (D-05).
     */
    fun clearActivityFilter(memberId: Int) {
        val allTypes = ActivityEntryType.entries.toSet()
        _uiState.value = _uiState.value.copy(activityEnabledTypes = allTypes)
        viewModelScope.launch {
            activityFilterPreferences.clearFilter()
            val sixMonthsAgo = LocalDate.now().minusMonths(6).toString()
            reloadFilteredEntries(memberId, allTypes, sixMonthsAgo)
        }
    }

    private suspend fun reloadFilteredEntries(
        memberId: Int,
        enabledTypes: Set<ActivityEntryType>,
        sixMonthsAgo: String
    ) {
        coroutineScope {
            val state = _uiState.value
            val votesDeferred = async { loadVoteEntries(memberId, sixMonthsAgo, state.memberVotes) }
            val questionsDeferred = async { loadQuestionEntries(memberId, sixMonthsAgo) }
            val incomeDeferred = async { loadIncomeEntries(memberId, sixMonthsAgo, state.interests) }
            val expenseDeferred = async { loadExpenseEntries(memberId, sixMonthsAgo, state.expenses) }
            val committeeDeferred = async { loadCommitteeEntries(memberId, sixMonthsAgo, state.committees) }
            val careerDeferred = async { loadCareerEntries(memberId, sixMonthsAgo, state.bioData) }
            val speechDeferred = async { loadSpeechEntries(memberId, sixMonthsAgo) }

            val allEntries = (
                votesDeferred.await() +
                    questionsDeferred.await() +
                    incomeDeferred.await() +
                    expenseDeferred.await() +
                    committeeDeferred.await() +
                    careerDeferred.await() +
                    speechDeferred.await()
                )
                .filter { it.date.take(10) >= sixMonthsAgo }
                .filter { it.entryType in enabledTypes }
                .sortedByDescending { it.date }

            _uiState.value = _uiState.value.copy(
                activityEntries = allEntries,
                activityTotalCount = allEntries.size
            )
        }
    }

    fun toggleFollow(memberId: Int) {
        // Optimistic UI: flip the follow icon immediately so the user sees
        // the change without waiting for the repository round-trip (issue #7).
        val wasFollowing = _uiState.value.isFollowing
        _uiState.value = _uiState.value.copy(isFollowing = !wasFollowing)
        // When following, auto-enable vote notifications optimistically
        if (!wasFollowing) {
            _uiState.value = _uiState.value.copy(
                votesNotificationsEnabled = true,
                notificationsEnabled = true
            )
        }
        viewModelScope.launch {
            try {
                if (wasFollowing) {
                    followRepository.unfollow(memberId)
                } else {
                    followRepository.follow(memberId)
                    // Auto-enable vote notifications when following (FotMob behavior)
                    notificationPrefRepository.setVotesEnabled(memberId, true)
                }
            } catch (e: Exception) {
                // Revert on failure to avoid stale state
                _uiState.value = _uiState.value.copy(isFollowing = wasFollowing)
                if (!wasFollowing) {
                    _uiState.value = _uiState.value.copy(
                        votesNotificationsEnabled = _uiState.value.votesNotificationsEnabled,
                        notificationsEnabled = _uiState.value.notificationsEnabled
                    )
                }
                android.util.Log.e("GovEye/Profile", "toggleFollow failed", e)
            }
        }
    }

    // --- Notification preferences ---

    fun setNotificationsEnabled(memberId: Int, enabled: Boolean) {
        // Optimistic UI: update the master toggle and derived type states
        // immediately (issue #7).
        val previous = _uiState.value
        _uiState.value = if (enabled) {
            _uiState.value.copy(
                notificationsEnabled = true,
                votesNotificationsEnabled = true
            )
        } else {
            _uiState.value.copy(
                notificationsEnabled = false,
                votesNotificationsEnabled = false,
                speechesNotificationsEnabled = false,
                incomeEnabled = false,
                expensesEnabled = false
            )
        }
        viewModelScope.launch {
            try {
                notificationPrefRepository.setNotificationsEnabled(memberId, enabled)
            } catch (e: Exception) {
                _uiState.value = previous
                android.util.Log.e("GovEye/Profile", "setNotificationsEnabled failed", e)
            }
        }
    }

    fun setVotesNotificationsEnabled(memberId: Int, enabled: Boolean) {
        val previous = _uiState.value.votesNotificationsEnabled
        _uiState.value = _uiState.value.copy(votesNotificationsEnabled = enabled)
        viewModelScope.launch {
            try {
                notificationPrefRepository.setVotesEnabled(memberId, enabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(votesNotificationsEnabled = previous)
                android.util.Log.e("GovEye/Profile", "setVotesNotificationsEnabled failed", e)
            }
        }
    }

    fun setSpeechesNotificationsEnabled(memberId: Int, enabled: Boolean) {
        val previous = _uiState.value.speechesNotificationsEnabled
        _uiState.value = _uiState.value.copy(speechesNotificationsEnabled = enabled)
        viewModelScope.launch {
            try {
                notificationPrefRepository.setSpeechesEnabled(memberId, enabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(speechesNotificationsEnabled = previous)
                android.util.Log.e("GovEye/Profile", "setSpeechesNotificationsEnabled failed", e)
            }
        }
    }

    fun setIncomeNotificationsEnabled(memberId: Int, enabled: Boolean) {
        val previous = _uiState.value.incomeEnabled
        _uiState.value = _uiState.value.copy(incomeEnabled = enabled)
        viewModelScope.launch {
            try {
                notificationPrefRepository.setIncomeEnabled(memberId, enabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(incomeEnabled = previous)
                android.util.Log.e("GovEye/Profile", "setIncomeNotificationsEnabled failed", e)
            }
        }
    }

    fun setExpensesNotificationsEnabled(memberId: Int, enabled: Boolean) {
        val previous = _uiState.value.expensesEnabled
        _uiState.value = _uiState.value.copy(expensesEnabled = enabled)
        viewModelScope.launch {
            try {
                notificationPrefRepository.setExpensesEnabled(memberId, enabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(expensesEnabled = previous)
                android.util.Log.e("GovEye/Profile", "setExpensesNotificationsEnabled failed", e)
            }
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
