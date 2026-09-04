package com.goveye.app.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the 4-tab bottom nav (D-14, D-19).
 *
 * Each route is a `@Serializable data object` implementing [NavKey] so
 * Nav3 can save/restore the back stack across process death.
 */
@Serializable
data object FeedRoute : NavKey

@Serializable
data object DirectoryRoute : NavKey

@Serializable
data object FollowingRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data class ProfileRoute(
    val memberId: Int,
    // Optimistic header data — passed from the source screen so the profile
    // header (name, party, photo, gradient, activity score, age) can render
    // instantly before the Stage 1 DB load completes. null when navigating
    // from places that don't have this data (e.g. deep links).
    val fallbackName: String? = null,
    val fallbackPartyName: String? = null,
    val fallbackPartyColor: String? = null,
    val fallbackThumbnailUrl: String? = null,
    val fallbackConstituency: String? = null,
    val fallbackActivityScore: Float? = null,
    val fallbackDateOfBirth: String? = null,
    // Initial tab index to open (0=Profile, 1=Career, 2=Committees, 3=Stats,
    // 4=Activity, 5=Interests). Used when navigating from microview expand
    // to open the finances/interests tab directly.
    val initialTab: Int = 0
) : NavKey

/**
 * Lightweight MP header data for optimistic profile rendering.
 * Passed from source screens (directory, feed, division detail) so the
 * profile header can render instantly before the DB load completes.
 */
@Serializable
data class MpHeaderFallback(
    val name: String,
    val partyName: String? = null,
    val partyColor: String? = null,
    val thumbnailUrl: String? = null,
    val constituency: String? = null,
    val activityScore: Float? = null,
    val dateOfBirth: String? = null
) {
    fun toRoute(memberId: Int, initialTab: Int = 0) = ProfileRoute(
        memberId = memberId,
        fallbackName = name,
        fallbackPartyName = partyName,
        fallbackPartyColor = partyColor,
        fallbackThumbnailUrl = thumbnailUrl,
        fallbackConstituency = constituency,
        fallbackActivityScore = activityScore,
        fallbackDateOfBirth = dateOfBirth,
        initialTab = initialTab
    )

    companion object {
        /**
         * Build from a domain [Mp] — the common case when navigating from
         * the directory, feed, or division detail screens.
         */
        fun fromMp(mp: com.goveye.app.domain.model.Mp, activityScore: Float? = null, dateOfBirth: String? = null) =
            MpHeaderFallback(
                name = mp.nameDisplayAs,
                partyName = mp.party?.name,
                partyColor = mp.party?.backgroundColour,
                thumbnailUrl = mp.thumbnailUrl,
                constituency = mp.constituency?.name,
                activityScore = activityScore,
                dateOfBirth = dateOfBirth
            )
    }
}

@Serializable
data class DivisionDetailRoute(val divisionId: Int, val house: Int = 1) : NavKey

@Serializable
data class BillDetailRoute(val billId: Int) : NavKey

@Serializable
data class InterestBucketDetailRoute(val memberId: Int, val bucketLabel: String, val entryType: String = "INCOME") :
    NavKey

@Serializable
data class TranscriptRoute(val divisionId: Int, val divisionTitle: String, val speechGid: String = "") : NavKey

@Serializable
data class PartyRoute(val partyId: Int) : NavKey

@Serializable
data class CommitteeRoute(val committeeId: Int) : NavKey

@Serializable
data class CouncilRoute(val councilId: Int) : NavKey

@Serializable
data class VotingRecordRoute(val memberId: Int) : NavKey

@Serializable
data class PublicationDetailRoute(val publicationId: Int) : NavKey

@Serializable
data class StatementDetailRoute(val statementId: Int) : NavKey

@Serializable
data class LegislationDetailRoute(val legislationId: Int) : NavKey

@Serializable
data class MpTagBrowseRoute(val tag: String) : NavKey
