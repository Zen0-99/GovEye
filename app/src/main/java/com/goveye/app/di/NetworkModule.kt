package com.goveye.app.di

import com.goveye.app.data.api.RetryInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val MEMBERS_BASE_URL = "https://members-api.parliament.uk/api/"
    private const val HANSARD_BASE_URL = "https://hansard-api.parliament.uk/"
    private const val GITHUB_API_BASE_URL = "https://api.github.com/"
    private const val POSTCODES_BASE_URL = "https://api.postcodes.io/"
    private const val TIMEOUT_SECONDS = 30L
    private const val HANSARD_TIMEOUT_SECONDS = 15L
    private const val DB_DOWNLOAD_TIMEOUT_MINUTES = 10L
    private const val USER_AGENT = "GovEye/0.1.0 (open-source; https://github.com/GovEye)"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        return OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                val request =
                    chain
                        .request()
                        .newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .build()
                chain.proceed(request)
            }.addInterceptor(RetryInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("hansardClient")
    fun provideHansardOkHttpClient(okHttpClient: OkHttpClient): OkHttpClient = okHttpClient
        .newBuilder()
        .readTimeout(HANSARD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * OkHttpClient with a 10-minute timeout for large DB downloads.
     *
     * Built from scratch (NOT via okHttpClient.newBuilder()) to avoid
     * inheriting the HttpLoggingInterceptor with Level.BODY — that
     * interceptor buffers the entire response body in memory, which
     * causes OutOfMemoryError when downloading the 557MB seed DB.
     *
     * Used by DatabaseUpdateManager.downloadSeedDb.
     */
    @Provides
    @Singleton
    @Named("dbDownloadClient")
    fun provideDbDownloadOkHttpClient(): OkHttpClient = OkHttpClient
        .Builder()
        .addInterceptor { chain ->
            val request =
                chain
                    .request()
                    .newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
            chain.proceed(request)
        }.connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(DB_DOWNLOAD_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .writeTimeout(DB_DOWNLOAD_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .build()

    @Provides
    @Singleton
    @Named("membersApi")
    fun provideMembersRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        buildRetrofit(MEMBERS_BASE_URL, okHttpClient, json)

    @Provides
    @Singleton
    @Named("hansardApi")
    fun provideHansardRetrofit(@Named("hansardClient") okHttpClient: OkHttpClient, json: Json): Retrofit =
        buildRetrofit(HANSARD_BASE_URL, okHttpClient, json)

    /**
     * Retrofit for the GitHub Releases API (D-06).
     * Used by DatabaseUpdateApi to fetch the database-latest release from
     * Zen0-99/goveye-data. The existing 30s timeout is sufficient for
     * manifest.json (~200B) and patch.json (5-50KB).
     */
    @Provides
    @Singleton
    @Named("githubApi")
    fun provideGithubRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        buildRetrofit(GITHUB_API_BASE_URL, okHttpClient, json)

    /**
     * Retrofit for postcodes.io — free UK postcode lookup API.
     * Used by PostcodeRepository to resolve postcodes to constituencies.
     * 15s timeout is sufficient for single-postcode lookups.
     */
    @Provides
    @Singleton
    @Named("postcodesApi")
    fun providePostcodesRetrofit(@Named("hansardClient") okHttpClient: OkHttpClient, json: Json): Retrofit =
        buildRetrofit(POSTCODES_BASE_URL, okHttpClient, json)

    @Provides
    @Singleton
    fun providePostcodesApi(@Named("postcodesApi") retrofit: Retrofit): com.goveye.app.data.api.PostcodesApi =
        retrofit.create(com.goveye.app.data.api.PostcodesApi::class.java)

    private fun buildRetrofit(baseUrl: String, okHttpClient: OkHttpClient, json: Json): Retrofit = Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
