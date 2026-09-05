package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.AnnouncementTagDao
import com.goveye.app.data.local.dao.BioDataDao
import com.goveye.app.data.local.dao.GovernmentPublicationDao
import com.goveye.app.data.local.dao.LegislationDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.MpTagDao
import com.goveye.app.data.local.dao.PartyLeaderDao
import com.goveye.app.data.local.dao.SourceRecommendationDao
import com.goveye.app.data.local.dao.WrittenStatementDao
import com.goveye.app.data.local.entity.BioDataEntity
import com.goveye.app.data.local.entity.GovernmentPublicationEntity
import com.goveye.app.data.local.entity.LegislationEntity
import com.goveye.app.data.local.entity.MpTagEntity
import com.goveye.app.data.local.entity.PartyLeaderEntity
import com.goveye.app.data.local.entity.SourceRecommendationEntity
import com.goveye.app.data.local.entity.WrittenStatementEntity
import com.goveye.app.domain.model.GovernmentPublication
import com.goveye.app.domain.model.Legislation
import com.goveye.app.domain.model.MpTag
import com.goveye.app.domain.model.PartyLeader
import com.goveye.app.domain.model.PartyLeaderDetail
import com.goveye.app.domain.model.SourceRecommendation
import com.goveye.app.domain.model.WrittenStatement
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * DB-only repository for government announcements (publications, written
 * statements, legislation, tags, MP tags, party leaders, source
 * recommendations).
 *
 * No Retrofit/API imports — all data comes from [BundledDatabase] tables
 * populated at build time by the goveye-data build scripts.
 *
 * Follows the [InterestsRepository] pattern.
 */
@Singleton
class GovernmentAnnouncementsRepository @Inject constructor(
    private val writtenStatementDao: WrittenStatementDao,
    private val governmentPublicationDao: GovernmentPublicationDao,
    private val legislationDao: LegislationDao,
    private val announcementTagDao: AnnouncementTagDao,
    private val mpTagDao: MpTagDao,
    private val partyLeaderDao: PartyLeaderDao,
    private val sourceRecommendationDao: SourceRecommendationDao,
    private val mpDao: MpDao,
    private val bioDataDao: BioDataDao
) {
    // --- Written statements ---

    fun observeStatements(limit: Int = 50): Flow<List<WrittenStatement>> =
        writtenStatementDao.observeStatements(limit).map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeStatementsByHouse(house: Int, limit: Int = 50): Flow<List<WrittenStatement>> =
        writtenStatementDao.observeStatementsByHouse(house, limit).map { entities ->
            entities.map { it.toDomain() }
        }

    fun searchStatements(query: String, limit: Int = 50): Flow<List<WrittenStatement>> =
        writtenStatementDao.searchStatements(query, limit).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getStatement(id: Int): WrittenStatement? = writtenStatementDao.getStatement(id)?.toDomain()

    // --- Government publications ---

    fun observePublications(limit: Int = 50): Flow<List<GovernmentPublication>> =
        governmentPublicationDao.observePublications(limit).map { entities ->
            entities.map { it.toDomain() }
        }

    fun observePublicationsByOrg(orgSlug: String, limit: Int = 50): Flow<List<GovernmentPublication>> =
        governmentPublicationDao.observePublicationsByOrg(orgSlug, limit).map { entities ->
            entities.map { it.toDomain() }
        }

    fun searchPublications(query: String, limit: Int = 50): Flow<List<GovernmentPublication>> =
        governmentPublicationDao.searchPublications(query, limit).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getPublication(id: Int): GovernmentPublication? =
        governmentPublicationDao.getPublication(id)?.toDomain()

    // --- Legislation ---

    fun observeLegislation(limit: Int = 50): Flow<List<Legislation>> =
        legislationDao.observeLegislation(limit).map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeLegislationByType(type: String, limit: Int = 50): Flow<List<Legislation>> =
        legislationDao.observeLegislationByType(type, limit).map { entities ->
            entities.map { it.toDomain() }
        }

    fun searchLegislation(query: String, limit: Int = 50): Flow<List<Legislation>> =
        legislationDao.searchLegislation(query, limit).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getLegislation(id: Int): Legislation? = legislationDao.getLegislation(id)?.toDomain()

    // --- Announcement tags ---

    /**
     * All distinct tags across publication, statement, and legislation tag tables.
     * Used by the FilterBottomSheet Tags section (D-14).
     */
    fun observeAllAnnouncementTags(): Flow<List<String>> = combine(
        announcementTagDao.observeAllPublicationTags(),
        announcementTagDao.observeAllStatementTags(),
        announcementTagDao.observeAllLegislationTags()
    ) { pubs, stmts, leg ->
        (pubs + stmts + leg).distinct().sorted()
    }

    fun observeTagsForPublication(publicationId: Int): Flow<List<String>> =
        announcementTagDao.observeTagsForPublication(publicationId)

    suspend fun getTagsForPublication(publicationId: Int): List<String> =
        announcementTagDao.getTagsForPublication(publicationId)

    fun observeTagsForStatement(statementId: Int): Flow<List<String>> =
        announcementTagDao.observeTagsForStatement(statementId)

    suspend fun getTagsForStatement(statementId: Int): List<String> =
        announcementTagDao.getTagsForStatement(statementId)

    fun observeTagsForLegislation(legislationId: Int): Flow<List<String>> =
        announcementTagDao.observeTagsForLegislation(legislationId)

    suspend fun getTagsForLegislation(legislationId: Int): List<String> =
        announcementTagDao.getTagsForLegislation(legislationId)

    /**
     * Batch fetch tags for multiple publications in a single query.
     * Returns a map of publicationId → list of tags (ordered by hitCount desc).
     * Use this instead of calling [getTagsForPublication] in a loop —
     * eliminates N+1 query overhead in the feed.
     */
    suspend fun getTagsForPublications(ids: List<Int>): Map<Int, List<String>> =
        announcementTagDao.getTagRowsForPublications(ids)
            .groupBy { it.publicationId }
            .mapValues { (_, rows) -> rows.map { it.tag } }

    /**
     * Batch fetch tags for multiple statements in a single query.
     * Returns a map of statementId → list of tags (ordered by hitCount desc).
     */
    suspend fun getTagsForStatements(ids: List<Int>): Map<Int, List<String>> =
        announcementTagDao.getTagRowsForStatements(ids)
            .groupBy { it.statementId }
            .mapValues { (_, rows) -> rows.map { it.tag } }

    /**
     * Batch fetch tags for multiple legislation items in a single query.
     * Returns a map of legislationId → list of tags (ordered by hitCount desc).
     */
    suspend fun getTagsForLegislationBatch(ids: List<Int>): Map<Int, List<String>> =
        announcementTagDao.getTagRowsForLegislation(ids)
            .groupBy { it.legislationId }
            .mapValues { (_, rows) -> rows.map { it.tag } }

    suspend fun getPublicationIdsForTag(tag: String): List<Int> = announcementTagDao.getPublicationIdsForTag(tag)

    suspend fun getStatementIdsForTag(tag: String): List<Int> = announcementTagDao.getStatementIdsForTag(tag)

    suspend fun getLegislationIdsForTag(tag: String): List<Int> = announcementTagDao.getLegislationIdsForTag(tag)

    // --- MP tags ---

    fun observeTagsForMp(memberId: Int) = mpTagDao.observeTagsForMp(memberId)

    fun observeMpsForTag(tag: String): Flow<List<Int>> = mpTagDao.observeMpsForTag(tag)

    suspend fun getMpsForTag(tag: String): List<Int> = mpTagDao.getMpsForTag(tag)

    /** All mp_tags rows for the given tags — used by MpCurationHelper (D-08). */
    suspend fun getMpTagsForTags(tags: List<String>): List<MpTag> =
        mpTagDao.getMpTagsForTags(tags).map { it.toDomain() }

    /** All mp_tags rows as a reactive flow — used by OnboardingViewModel (D-08). */
    fun observeAllMpTagRows(): Flow<List<MpTag>> =
        mpTagDao.observeAllMpTagRows().map { entities -> entities.map { it.toDomain() } }

    // --- Party leaders ---

    fun observePartyLeaders(): Flow<List<PartyLeader>> = partyLeaderDao.observePartyLeaders().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getLeaderForParty(partyId: Int): PartyLeader? = partyLeaderDao.getLeaderForParty(partyId)?.toDomain()

    /**
     * Returns the partyId of the ruling party (the party whose leader has
     * the title "Prime Minister"), or null if not found.
     */
    suspend fun getRulingPartyId(): Int? {
        val leaders = partyLeaderDao.getAllPartyLeaders()
        return leaders.find { it.title.contains("Prime Minister", ignoreCase = true) }?.partyId
    }

    /**
     * Fetch rich party leader detail by joining party_leaders → mps → bio_data.
     *
     * - Name, constituency, thumbnailUrl, party colour come from the mps table
     * - Age is computed from bio_data.dateOfBirth
     * - "Leader for X years" is computed from bio_data.postsJson — the post
     *   matching the leader title has a startDate field
     *
     * Returns null if the party has no leader in the table, or if the MP
     * referenced by party_leaders.memberId is not in the mps table.
     */
    suspend fun getPartyLeaderDetail(partyId: Int): PartyLeaderDetail? {
        val leaderEntity = partyLeaderDao.getLeaderForParty(partyId) ?: return null
        val mp = mpDao.getMp(leaderEntity.memberId) ?: return null
        val bioData = try {
            bioDataDao.getByMpId(leaderEntity.memberId)
        } catch (e: Exception) {
            null
        }

        val age = bioData?.dateOfBirth?.let { computeAge(it) }
        val leaderSinceYears = bioData?.postsJson?.let { postsJson ->
            parseLeaderStartDate(postsJson, leaderEntity.title)?.let { startDate ->
                computeYearsSince(startDate)
            }
        }

        return PartyLeaderDetail(
            memberId = mp.id,
            name = mp.nameDisplayAs,
            constituency = mp.constituencyName,
            thumbnailUrl = mp.thumbnailUrl,
            partyBackgroundColour = mp.partyBackgroundColour,
            title = leaderEntity.title,
            age = age,
            leaderSinceYears = leaderSinceYears
        )
    }

    /**
     * Compute age from a date-of-birth string (expected format: "YYYY-MM-DD").
     */
    private fun computeAge(dob: String): Int? {
        return try {
            val parts = dob.split("-")
            if (parts.size < 3) return null
            val birthYear = parts[0].toInt()
            val birthMonth = parts[1].toInt()
            val birthDay = parts[2].toInt()
            val now = java.time.LocalDate.now()
            var age = now.year - birthYear
            if (now.monthValue < birthMonth || (now.monthValue == birthMonth && now.dayOfMonth < birthDay)) {
                age--
            }
            age
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Compute whole years since the given date (expected format: "YYYY-MM-DD").
     */
    private fun computeYearsSince(startDate: String): Int? {
        return try {
            val parts = startDate.split("-")
            if (parts.size < 3) return null
            val startYear = parts[0].toInt()
            val startMonth = parts[1].toInt()
            val startDay = parts[2].toInt()
            val now = java.time.LocalDate.now()
            var years = now.year - startYear
            if (now.monthValue < startMonth || (now.monthValue == startMonth && now.dayOfMonth < startDay)) {
                years--
            }
            years
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse bio_data.postsJson (a JSON array of posts) and find the startDate
     * of the post whose title matches the leader title.
     *
     * postsJson format: [{"type":"...","title":"...","department":"...","startDate":"...","endDate":"..."}, ...]
     */
    private fun parseLeaderStartDate(postsJson: String, leaderTitle: String): String? {
        return try {
            val array = org.json.JSONArray(postsJson)
            for (i in 0 until array.length()) {
                val post = array.getJSONObject(i)
                val title = post.optString("title", "")
                if (title.contains(leaderTitle, ignoreCase = true)) {
                    val startDate = post.optString("startDate", null)
                    if (!startDate.isNullOrBlank()) return startDate
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // --- Source recommendations ---

    fun observeRecommendationsForTag(tag: String): Flow<List<SourceRecommendation>> =
        sourceRecommendationDao.observeRecommendationsForTag(tag).map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeAllRecommendations(): Flow<List<SourceRecommendation>> =
        sourceRecommendationDao.observeAllRecommendations().map { entities ->
            entities.map { it.toDomain() }
        }

    // --- Domain mappers ---

    private fun WrittenStatementEntity.toDomain(): WrittenStatement = WrittenStatement(
        id = id,
        memberId = memberId,
        memberRole = memberRole,
        uin = uin,
        dateMade = dateMade,
        answeringBodyId = answeringBodyId,
        answeringBodyName = answeringBodyName,
        title = title,
        text = text,
        house = house,
        url = "https://questions-statements.parliament.uk/written-statements/detail/$uin"
    )

    private fun GovernmentPublicationEntity.toDomain(): GovernmentPublication = GovernmentPublication(
        id = id,
        title = title,
        summary = summary,
        url = url,
        documentType = documentType,
        organisation = organisation,
        organisationSlug = organisationSlug,
        firstPublishedAt = firstPublishedAt,
        publicUpdatedAt = publicUpdatedAt,
        imageUrl = imageUrl,
        bodyText = bodyText
    )

    private fun LegislationEntity.toDomain(): Legislation = Legislation(
        id = id,
        title = title,
        type = type,
        year = year,
        number = number,
        date = date,
        url = url,
        bodyText = bodyText
    )

    private fun PartyLeaderEntity.toDomain(): PartyLeader = PartyLeader(
        partyId = partyId,
        memberId = memberId,
        title = title
    )

    private fun SourceRecommendationEntity.toDomain(): SourceRecommendation = SourceRecommendation(
        tag = tag,
        organisationSlug = organisationSlug,
        organisationName = organisationName,
        hitCount = hitCount,
        isRecommended = isRecommended
    )

    private fun MpTagEntity.toDomain(): MpTag = MpTag(
        memberId = memberId,
        tag = tag,
        hitCount = hitCount
    )
}
