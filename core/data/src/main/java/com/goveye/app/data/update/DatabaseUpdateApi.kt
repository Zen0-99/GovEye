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
 * Six per-API release tags are checked for patch updates (D-10):
 * - [MPS_TAG] (mps-latest) — MP data
 * - [COMMONS_VOTES_TAG] (commons-votes-latest) — Commons divisions + division_votes (house=1)
 * - [LORDS_VOTES_TAG] (lords-votes-latest) — Lords divisions + division_votes (house=2)
 * - [BILLS_TAG] (bills-latest) — bills + bill_stages
 * - [COMMITTEES_TAG] (committees-latest) — committees + mp_committee_cross_ref
 * - [RECESS_TAG] (recess-latest) — recess_dates + recess_dates_meta
 * - [INTERESTS_TAG] (interests-latest) — interests
 *
 * The [SEED_TAG] (seed-latest) release holds the merged goveye.db for
 * first-launch download (D-04, D-10a).
 */
interface DatabaseUpdateApi {
    /**
     * Fetch any release by its tag name. Used for the 7 per-API patch streams.
     *
     * Each per-API release has: manifest.json, patch.json, and a per-API .db.
     */
    @GET("repos/{owner}/{repo}/releases/tags/{tag}")
    suspend fun getReleaseByTag(
        @Path("owner") owner: String = OWNER,
        @Path("repo") repo: String = REPO,
        @Path("tag") tag: String
    ): GithubReleaseDto

    /**
     * Fetch the `seed-latest` release for first-launch DB download (D-04, D-10a).
     *
     * Assets: goveye.db (merged by merge_dbs.py), optionally manifest.json.
     */
    @GET("repos/{owner}/{repo}/releases/tags/seed-latest")
    suspend fun getSeedRelease(
        @Path("owner") owner: String = OWNER,
        @Path("repo") repo: String = REPO
    ): GithubReleaseDto

    companion object {
        const val MPS_TAG = "mps-latest"
        const val COMMONS_VOTES_TAG = "commons-votes-latest"
        const val LORDS_VOTES_TAG = "lords-votes-latest"
        const val BILLS_TAG = "bills-latest"
        const val COMMITTEES_TAG = "committees-latest"
        const val RECESS_TAG = "recess-latest"
        const val INTERESTS_TAG = "interests-latest"
        const val SEED_TAG = "seed-latest"

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
