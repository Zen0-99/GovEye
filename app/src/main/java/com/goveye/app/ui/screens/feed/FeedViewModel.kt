package com.goveye.app.ui.screens.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.DebateSpeechDao
import com.goveye.app.data.local.dao.SpeechWithDivision
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.data.preference.OnboardingPreferences
import com.goveye.app.data.repo.ExpensesRepository
import com.goveye.app.data.repo.FeedRepository
import com.goveye.app.data.repo.GovernmentAnnouncementsRepository
import com.goveye.app.data.repo.InterestsRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.model.Interest
import com.goveye.app.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class FeedFilterState(
    val followingOnly: Boolean,
    val query: String,
    val house: Int,
    val limit: Int,
    val tagFilter: Set<String>,
    val sourceFilter: Set<String>,
    val departmentFilter: Set<String>,
    val typeFilter: Set<CardType>
)

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class FilterQuad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val governmentAnnouncementsRepository: GovernmentAnnouncementsRepository,
    private val interestsRepository: InterestsRepository,
    private val expensesRepository: ExpensesRepository,
    private val membersRepository: MembersRepository,
    private val debateSpeechDao: DebateSpeechDao,
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    private val followingOnlyState = MutableStateFlow(false)
    private val searchQueryState = MutableStateFlow("")
    private val houseFilterState = MutableStateFlow(0)
    private val currentRecess = MutableStateFlow<RecessDateEntity?>(null)

    // New filter states for announcement curation (D-12, D-14)
    private val tagFilterState = MutableStateFlow<Set<String>>(emptySet())
    private val sourceFilterState = MutableStateFlow<Set<String>>(emptySet())
    private val departmentFilterState = MutableStateFlow<Set<String>>(emptySet())
    private val typeFilterState = MutableStateFlow<Set<CardType>>(emptySet())

    // Pagination — start with 10 for fast initial render, then increase
    // to 50 after the first emission. The user only sees the first 5-10
    // cards on initial load, so loading 50 upfront wastes time.
    private val feedLimit = MutableStateFlow(10)
    private var hasExpandedLimit = false

    init {
        Log.i("GovEye/Feed", "FeedViewModel init — fetching recess status + onboarding tags")
        viewModelScope.launch {
            currentRecess.value = feedRepository.getCurrentRecess(1)
            Log.i("GovEye/Feed", "Recess status fetched: ${currentRecess.value}")
        }
        // Load onboarding-selected tags as the initial tag filter
        viewModelScope.launch {
            val savedTags = onboardingPreferences.selectedTags.first()
            if (savedTags.isNotEmpty()) {
                tagFilterState.value = savedTags
                Log.i("GovEye/Feed", "Loaded ${savedTags.size} onboarding tags: $savedTags")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val state: StateFlow<FeedUiState> =
        combine(
            combine(
                followingOnlyState,
                searchQueryState,
                houseFilterState,
                feedLimit
            ) { followingOnly, query, house, limit ->
                Quad(followingOnly, query, house, limit)
            },
            combine(
                tagFilterState,
                sourceFilterState,
                departmentFilterState,
                typeFilterState
            ) { tags, sources, departments, types ->
                FilterQuad(tags, sources, departments, types)
            }
        ) { core, filters ->
            FeedFilterState(
                followingOnly = core.a,
                query = core.b,
                house = core.c,
                limit = core.d,
                tagFilter = filters.a,
                sourceFilter = filters.b,
                departmentFilter = filters.c,
                typeFilter = filters.d
            )
        }.flatMapLatest { filter ->
            Log.i(
                "GovEye/Feed",
                "flatMapLatest — followingOnly=${filter.followingOnly} query='${filter.query}' " +
                    "house=${filter.house} limit=${filter.limit} tags=${filter.tagFilter} " +
                    "sources=${filter.sourceFilter} depts=${filter.departmentFilter} types=${filter.typeFilter}"
            )
            val feedFlow = if (filter.followingOnly) {
                feedRepository.observeFeedDataFiltered(filter.limit)
            } else {
                feedRepository.observeFeedData(filter.limit)
            }
            // Combine divisions + publications + statements + legislation flows
            val publicationsFlow = governmentAnnouncementsRepository.observePublications(filter.limit)
            val statementsFlow = governmentAnnouncementsRepository.observeStatements(filter.limit)
            val legislationFlow = governmentAnnouncementsRepository.observeLegislation(filter.limit)

            combine(feedFlow, publicationsFlow, statementsFlow, legislationFlow, currentRecess) {
                    feedData,
                    publications,
                    statements,
                    legislation,
                    recess
                ->
                val processingStart = System.currentTimeMillis()

                // Build division items (filter-based, soft — D-12)
                val divisionItems = feedData.divisions
                    .filter { filter.house == 0 || it.house == filter.house }
                    .filter { filter.query.isBlank() || it.title.contains(filter.query, ignoreCase = true) }
                    .map { division ->
                        FeedItem.DivisionItem(
                            division = division,
                            tags = feedData.divisionTags[division.id] ?: emptyList(),
                            followedVotes = feedData.followedMpVotes[division.id] ?: emptyList()
                        )
                    }
                    .filter { filter.tagFilter.isEmpty() || it.tags.any { tag -> tag in filter.tagFilter } }

                // Batch fetch all announcement tags in 3 queries (instead of
                // 150 individual per-item queries). This eliminates N+1 query
                // overhead — the biggest contributor to feed loading time.
                // Tags are ordered by hitCount desc in the SQL, so grouping
                // preserves priority order.
                val publicationTagsById = if (publications.isNotEmpty()) {
                    governmentAnnouncementsRepository.getTagsForPublications(publications.map { it.id })
                } else {
                    emptyMap()
                }
                val statementTagsById = if (statements.isNotEmpty()) {
                    governmentAnnouncementsRepository.getTagsForStatements(statements.map { it.id })
                } else {
                    emptyMap()
                }
                val legislationTagsById = if (legislation.isNotEmpty()) {
                    governmentAnnouncementsRepository.getTagsForLegislationBatch(legislation.map { it.id })
                } else {
                    emptyMap()
                }

                // Build publication items (filter-based, soft — D-12)
                // Tags fetched in batch above — no per-item DB calls here.
                val publicationTagsMap = mutableMapOf<String, List<String>>()
                val publicationItems = publications
                    .filter { filter.query.isBlank() || it.title.contains(filter.query, ignoreCase = true) }
                    .filter { filter.departmentFilter.isEmpty() || it.organisationSlug in filter.departmentFilter }
                    .map { publication ->
                        val tags = publicationTagsById[publication.id] ?: emptyList()
                        publicationTagsMap["publication-${publication.id}"] = tags
                        FeedItem.PublicationItem(
                            publication = publication,
                            tags = tags
                        )
                    }
                    .filter { filter.tagFilter.isEmpty() || it.tags.any { tag -> tag in filter.tagFilter } }

                // Build statement items (filter-based, soft — D-12)
                val statementTagsMap = mutableMapOf<String, List<String>>()
                val statementItems = statements
                    .filter { filter.query.isBlank() || it.title.contains(filter.query, ignoreCase = true) }
                    .filter { filter.departmentFilter.isEmpty() || it.answeringBodyName in filter.departmentFilter }
                    .map { statement ->
                        val tags = statementTagsById[statement.id] ?: emptyList()
                        statementTagsMap["statement-${statement.id}"] = tags
                        FeedItem.StatementItem(
                            statement = statement,
                            tags = tags
                        )
                    }
                    .filter { filter.tagFilter.isEmpty() || it.tags.any { tag -> tag in filter.tagFilter } }

                // Build legislation items (filter-based, soft — D-12)
                val legislationTagsMap = mutableMapOf<String, List<String>>()
                val legislationItems = legislation
                    .filter { filter.query.isBlank() || it.title.contains(filter.query, ignoreCase = true) }
                    .map { legislation ->
                        val tags = legislationTagsById[legislation.id] ?: emptyList()
                        legislationTagsMap["legislation-${legislation.id}"] = tags
                        FeedItem.LegislationItem(
                            legislation = legislation,
                            tags = tags
                        )
                    }
                    .filter { filter.tagFilter.isEmpty() || it.tags.any { tag -> tag in filter.tagFilter } }

                // Apply type filter
                val allItems = mutableListOf<FeedItem>()
                if (filter.typeFilter.isEmpty() || CardType.DIVISION in filter.typeFilter) {
                    allItems.addAll(divisionItems)
                }
                if (filter.typeFilter.isEmpty() || CardType.PUBLICATION in filter.typeFilter) {
                    allItems.addAll(publicationItems)
                }
                if (filter.typeFilter.isEmpty() || CardType.STATEMENT in filter.typeFilter) {
                    allItems.addAll(statementItems)
                }
                if (filter.typeFilter.isEmpty() || CardType.LEGISLATION in filter.typeFilter) {
                    allItems.addAll(legislationItems)
                }

                // --- Followed MP financial activity + speeches (17-02-06) ---
                // Income (registered interests), expenses, and debate speeches
                // for followed MPs are merged into the feed as FinancialItem
                // and SpeechItem cards.
                val followedIds = feedData.followedMemberIds
                val financialItems = mutableListOf<FeedItem.FinancialItem>()
                val speechItems = mutableListOf<FeedItem.SpeechItem>()
                val mpVoteItems = mutableListOf<FeedItem.MpVoteItem>()
                if (followedIds.isNotEmpty()) {
                    // Load MP profile data (name, party color, photo) for followed members
                    val memberProfiles: Map<Int, MpEntity> = try {
                        membersRepository.getMpsByIds(followedIds.toList()).associateBy { it.id }
                    } catch (e: Exception) {
                        Log.w("GovEye/Feed", "Failed to load followed MP profiles", e)
                        emptyMap()
                    }

                    // Income — registered interests per followed member.
                    // Only include interests from 2026 onwards so the feed
                    // shows recent activity, not historical entries from 2024/2025.
                    followedIds.forEach { memberId ->
                        val profile = memberProfiles[memberId] ?: return@forEach
                        try {
                            val interests: List<Interest> =
                                interestsRepository.observeInterestsForMember(memberId).first().data
                            interests
                                .filter {
                                    it.publishedDate?.take(4) == "2026" || it.registrationDate?.take(4) == "2026"
                                }
                                .take(10)
                                .forEach { interest ->
                                    val pence = interest.parsedAmountPence ?: return@forEach
                                    val descLine = interestDescriptionLine(
                                        interest.paymentDescription,
                                        interest.visitPurpose,
                                        interest.organisationDescription
                                    )
                                    val structuredFields = formatInterestStructuredFields(
                                        interest.donorName, interest.paymentType, interest.paymentDescription,
                                        interest.donorStatus, interest.donorAddress, interest.donorCompanyIdentifier,
                                        interest.destination, interest.visitPurpose, interest.organisationName,
                                        interest.organisationDescription, interest.propertyLocation,
                                        interest.propertyType, interest.hoursWorked, interest.familyMemberName,
                                        interest.familyMemberRelationship, interest.familyMemberRole,
                                        descriptionLine = descLine
                                    )
                                    financialItems.add(
                                        FeedItem.FinancialItem(
                                            memberId = memberId,
                                            memberName = profile.nameDisplayAs,
                                            memberPartyColorHex = profile.partyBackgroundColour,
                                            memberPhotoUrl = profile.thumbnailUrl,
                                            amount = formatFeedPence(pence),
                                            whoOrWhere = interest.donorName?.takeIf { it.isNotBlank() }
                                                ?: interest.summary.lineSequence().firstOrNull()?.take(80) ?: "",
                                            description = descLine ?: "",
                                            category = interest.categoryName,
                                            isIncome = true,
                                            date = interest.publishedDate ?: interest.registrationDate ?: "",
                                            expandableFields = structuredFields.takeIf { it.isNotEmpty() },
                                            bucket = interest.bucket
                                        )
                                    )
                                }
                        } catch (e: Exception) {
                            Log.w("GovEye/Feed", "Failed to load interests for member $memberId", e)
                        }
                    }

                    // Expenses — recent expense claims per followed member.
                    // Only include 2026 claims to keep the feed current.
                    followedIds.forEach { memberId ->
                        val profile = memberProfiles[memberId] ?: return@forEach
                        try {
                            val expenses = expensesRepository.getExpenses(memberId)
                            expenses
                                .filter { (it.claimDate ?: it.supplyMonth ?: "").take(4) == "2026" }
                                .take(10)
                                .forEach { expense ->
                                    financialItems.add(
                                        FeedItem.FinancialItem(
                                            memberId = memberId,
                                            memberName = profile.nameDisplayAs,
                                            memberPartyColorHex = profile.partyBackgroundColour,
                                            memberPhotoUrl = profile.thumbnailUrl,
                                            amount = formatFeedPence(expense.amountPence),
                                            whoOrWhere = expense.bucket,
                                            description = expense.shortDescription ?: expense.category,
                                            category = expense.bucket,
                                            isIncome = false,
                                            date = expense.claimDate ?: expense.supplyMonth ?: ""
                                        )
                                    )
                                }
                        } catch (e: Exception) {
                            Log.w("GovEye/Feed", "Failed to load expenses for member $memberId", e)
                        }
                    }

                    // Speeches — batch query for all followed members
                    try {
                        val speeches: List<SpeechWithDivision> =
                            debateSpeechDao.getSpeechesByMemberIds(followedIds.toList(), 100)
                        speeches.forEach { speech ->
                            val profile = memberProfiles[speech.memberId] ?: return@forEach
                            speechItems.add(
                                FeedItem.SpeechItem(
                                    memberId = speech.memberId,
                                    memberName = profile.nameDisplayAs,
                                    memberPartyColorHex = profile.partyBackgroundColour,
                                    memberPhotoUrl = profile.thumbnailUrl,
                                    speechText = speech.speechText,
                                    speechGid = speech.speechGid,
                                    divisionId = speech.divisionId,
                                    divisionTitle = speech.divisionTitle,
                                    date = speech.divisionDate,
                                    tags = feedData.divisionTags[speech.divisionId] ?: emptyList()
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("GovEye/Feed", "Failed to load speeches for followed MPs", e)
                    }

                    // MP votes — build individual vote cards from followedMpVotes
                    // data (already fetched in FeedRepository). Each followed MP's
                    // vote on a division becomes a separate card in the feed.
                    feedData.followedMpVotes.forEach { (divisionId, votes) ->
                        votes.forEach { vote ->
                            val profile = memberProfiles[vote.memberId] ?: return@forEach
                            mpVoteItems.add(
                                FeedItem.MpVoteItem(
                                    memberId = vote.memberId,
                                    memberName = profile.nameDisplayAs,
                                    memberPartyColorHex = profile.partyBackgroundColour,
                                    memberPhotoUrl = profile.thumbnailUrl,
                                    vote = vote.vote,
                                    divisionId = vote.divisionId,
                                    divisionTitle = vote.divisionTitle,
                                    divisionHouse = vote.divisionHouse,
                                    ayeCount = vote.ayeCount,
                                    noCount = vote.noCount,
                                    date = vote.divisionDate,
                                    tags = feedData.divisionTags[vote.divisionId] ?: emptyList()
                                )
                            )
                        }
                    }
                }

                if (filter.typeFilter.isEmpty() || CardType.FINANCIAL in filter.typeFilter) {
                    allItems.addAll(financialItems)
                }
                if (filter.typeFilter.isEmpty() || CardType.SPEECH in filter.typeFilter) {
                    allItems.addAll(speechItems)
                }
                if (filter.typeFilter.isEmpty() || CardType.MP_VOTE in filter.typeFilter) {
                    allItems.addAll(mpVoteItems)
                }

                // Limit feed to a reasonable size (200 items) to avoid performance
                // issues — sort by date descending first so the most recent items
                // are retained.
                val cappedItems = allItems
                    .sortedByDescending { it.date }
                    .take(200)

                // Group by date (substring 0,10 of ISO date).
                // Some items (e.g. legislation without a CreationDate) may have
                // an empty date string — group them under "Unknown" instead of
                // crashing on substring.
                val dateGroups = cappedItems
                    .groupBy { if (it.date.length >= 10) it.date.substring(0, 10) else "Unknown" }
                    .entries
                    .sortedByDescending { it.key }
                    .map { (dateKey, items) ->
                        FeedDateGroup(
                            dateHeader = DateUtils.formatRelativeDate(dateKey),
                            dateKey = dateKey,
                            items = items.sortedByDescending { it.date }
                        )
                    }

                // Determine empty state
                val isEmpty = cappedItems.isEmpty() && !feedData.isLoading
                val isRecessEmpty = isEmpty && recess != null && filter.query.isBlank()
                val recentForRecess = if (isRecessEmpty) {
                    feedData.divisions.take(3)
                } else {
                    emptyList()
                }
                val hasMore = feedData.divisions.size >= filter.limit
                val totalDivisions = divisionItems.size
                val processingTime = System.currentTimeMillis() - processingStart
                Log.i(
                    "GovEye/Feed",
                    "State built — dateGroups=${dateGroups.size} divisions=${divisionItems.size} " +
                        "publications=${publicationItems.size} statements=${statementItems.size} " +
                        "legislation=${legislationItems.size} financial=${financialItems.size} " +
                        "speeches=${speechItems.size} mpVotes=${mpVoteItems.size} isEmpty=$isEmpty " +
                        "isRecessEmpty=$isRecessEmpty hasMore=$hasMore processingTime=${processingTime}ms"
                )
                // Progressive loading: after the first emission with the
                // small initial limit (10), expand to 50 in the background.
                // The user sees the first few cards quickly, then the rest
                // loads without any additional interaction.
                if (!hasExpandedLimit && filter.limit < 50) {
                    hasExpandedLimit = true
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(100)
                        feedLimit.value = 50
                        Log.i("GovEye/Feed", "Progressive load — expanding limit to 50")
                    }
                }
                FeedUiState(
                    dateGroups = dateGroups,
                    followedMemberIds = feedData.followedMemberIds,
                    divisionsWithFollowedVotes = feedData.divisionsWithFollowedVotes,
                    followedMpVotes = feedData.followedMpVotes,
                    divisionTags = feedData.divisionTags,
                    announcementTags = publicationTagsMap + statementTagsMap + legislationTagsMap,
                    followingOnly = filter.followingOnly,
                    searchQuery = filter.query,
                    houseFilter = filter.house,
                    tagFilter = filter.tagFilter,
                    sourceFilter = filter.sourceFilter,
                    departmentFilter = filter.departmentFilter,
                    typeFilter = filter.typeFilter,
                    currentRecess = recess,
                    isLoading = feedData.isLoading,
                    isEmpty = isEmpty,
                    isRecessEmpty = isRecessEmpty,
                    recentDivisionsForRecess = recentForRecess,
                    hasMore = hasMore,
                    totalDivisions = totalDivisions
                )
            }
        }.flowOn(Dispatchers.Default)
            // Cache each emission so the next ViewModel instance (after tab
            // switch) can use it as the initial state — avoids showing a
            // loading skeleton on every navigation.
            .onEach { state -> FeedCache.update(state) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                FeedCache.cached ?: FeedUiState()
            )

    fun setFollowingOnly(value: Boolean) {
        followingOnlyState.value = value
    }

    fun setSearchQuery(query: String) {
        searchQueryState.value = query
    }

    fun setHouseFilter(house: Int) {
        houseFilterState.value = house
    }

    fun setTagFilter(tags: Set<String>) {
        tagFilterState.value = tags
    }

    fun setSourceFilter(sources: Set<String>) {
        sourceFilterState.value = sources
    }

    fun setDepartmentFilter(departments: Set<String>) {
        departmentFilterState.value = departments
    }

    fun setTypeFilter(types: Set<CardType>) {
        typeFilterState.value = types
    }

    fun clearFilters() {
        followingOnlyState.value = false
        searchQueryState.value = ""
        houseFilterState.value = 0
        tagFilterState.value = emptySet()
        sourceFilterState.value = emptySet()
        departmentFilterState.value = emptySet()
        typeFilterState.value = emptySet()
    }

    /**
     * Load 50 more divisions. Called by the UI when the user scrolls near
     * the bottom of the current list.
     */
    fun loadMore() {
        feedLimit.value += 50
    }
}

/**
 * Formats pence as a GBP string for feed financial cards (e.g. 123456 -> "£1,235").
 * Matches the [com.goveye.app.ui.screens.mpprofile] formatAmount helper.
 */
private fun formatFeedPence(pence: Long): String {
    val pounds = pence / 100.0
    return "£${String.format(Locale.UK, "%,.0f", pounds)}"
}
