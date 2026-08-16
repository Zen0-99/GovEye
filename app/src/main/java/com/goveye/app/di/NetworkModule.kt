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
    private const val VOTES_BASE_URL = "https://commonsvotes-api.parliament.uk/data/"
    private const val BILLS_BASE_URL = "https://bills-api.parliament.uk/api/v1/"
    private const val HANSARD_BASE_URL = "https://hansard-api.parliament.uk/"
    private const val INTERESTS_BASE_URL = "https://interests-api.parliament.uk/api/v1/"
    private const val COMMITTEES_BASE_URL = "https://committees-api.parliament.uk/api/"
    private const val TIMEOUT_SECONDS = 30L
    private const val HANSARD_TIMEOUT_SECONDS = 15L
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
    fun provideHansardOkHttpClient(okHttpClient: OkHttpClient): OkHttpClient =
        okHttpClient
            .newBuilder()
            .readTimeout(HANSARD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @Named("membersApi")
    fun provideMembersRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(MEMBERS_BASE_URL, okHttpClient, json)

    @Provides
    @Singleton
    @Named("votesApi")
    fun provideVotesRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(VOTES_BASE_URL, okHttpClient, json)

    @Provides
    @Singleton
    @Named("billsApi")
    fun provideBillsRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(BILLS_BASE_URL, okHttpClient, json)

    @Provides
    @Singleton
    @Named("hansardApi")
    fun provideHansardRetrofit(
        @Named("hansardClient") okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(HANSARD_BASE_URL, okHttpClient, json)

    @Provides
    @Singleton
    @Named("interestsApi")
    fun provideInterestsRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(INTERESTS_BASE_URL, okHttpClient, json)

    @Provides
    @Singleton
    @Named("committeesApi")
    fun provideCommitteesRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(COMMITTEES_BASE_URL, okHttpClient, json)

    private fun buildRetrofit(
        baseUrl: String,
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
