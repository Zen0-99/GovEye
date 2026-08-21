package com.goveye.app.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for the GitHub Releases API (D-06, D-10, D-10a).
 *
 * Fetches releases from the `Zen0-99/goveye-data` repo. The repo must be
 * public so that `browser_download_url` works without auth (Pitfall 6).
 *
 * Seven per-API release tags are used for both first-launch DB downloads
 * and incremental patch updates:
 * - [MPS_TAG] (mps-latest) — MP data
 * - [COMMONS_VOTES_TAG] (commons-votes-latest) — Commons divisions + division_votes (house=1)
 * - [LORDS_VOTES_TAG] (lords-votes-latest) — Lords divisions + division_votes (house=2)
 * - [BILLS_TAG] (bills-latest) — bills + bill_stages
 * - [COMMITTEES_TAG] (committees-latest) — committees + mp_committee_cross_ref
 * - [RECESS_TAG] (recess-latest) — recess_dates + recess_dates_meta
 * - [INTERESTS_TAG] (interests-latest) — interests
 * - [DEBATES_TAG] (debates-latest) — debate_speeches (transcripts scraped from TWFY)
 *
 * First launch downloads all 7 per-API .db files and merges them on-device
 * into goveye.db. No separate seed release is needed.
 */
interface DatabaseUpdateApi {
    /**
     * Fetch any release by its tag name. Used for the 7 per-API streams.
     *
     * Each per-API release has: manifest.json, patch.json, and a per-API .db.
     */
    @GET("repos/{owner}/{repo}/releases/tags/{tag}")
    suspend fun getReleaseByTag(
        @Path("owner") owner: String = OWNER,
        @Path("repo") repo: String = REPO,
        @Path("tag") tag: String
    ): GithubReleaseDto

    companion object {
        const val MPS_TAG = "mps-latest"
        const val COMMONS_VOTES_TAG = "commons-votes-latest"
        const val LORDS_VOTES_TAG = "lords-votes-latest"
        const val BILLS_TAG = "bills-latest"
        const val COMMITTEES_TAG = "committees-latest"
        const val RECESS_TAG = "recess-latest"
        const val INTERESTS_TAG = "interests-latest"
        const val DEBATES_TAG = "debates-latest"
        const val BIO_DATA_TAG = "bio-data-latest"
        const val EXPENSES_TAG = "expenses-latest"
        const val MP_LINKS_TAG = "mp-links-latest"
        const val MANIFESTOS_TAG = "manifestos-latest"
        const val PARTY_STATS_TAG = "party-stats-latest"

        private const val OWNER = "Zen0-99"
        private const val REPO = "goveye-data"
    }
}

@Serializable
data class GithubReleaseDto(val assets: List<ReleaseAssetDto> = emptyList())

@Serializable
data class ReleaseAssetDto(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0
)
