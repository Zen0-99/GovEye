package com.goveye.app.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for the GitHub Releases API (D-06).
 *
 * Fetches the `database-latest` release from the `Zen0-99/goveye-data` repo.
 * The repo must be public so that `browser_download_url` works without auth
 * (Pitfall 6).
 */
interface DatabaseUpdateApi {
    /**
     * Fetch the `database-latest` release from the goveye-data repo.
     * Assets: manifest.json, patch.json, goveye.db.
     */
    @GET("repos/{owner}/{repo}/releases/tags/database-latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String = OWNER,
        @Path("repo") repo: String = REPO,
    ): GithubReleaseDto

    private companion object {
        const val OWNER = "Zen0-99"
        const val REPO = "goveye-data"
    }
}

@Serializable
data class GithubReleaseDto(
    val assets: List<ReleaseAssetDto> = emptyList(),
)

@Serializable
data class ReleaseAssetDto(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0,
)
