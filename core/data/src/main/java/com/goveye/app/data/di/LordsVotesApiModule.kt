package com.goveye.app.data.di

import com.goveye.app.data.api.LordsVotesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object LordsVotesApiModule {
    @Provides
    @Singleton
    fun provideLordsVotesApi(@Named("lordsVotesApi") retrofit: Retrofit): LordsVotesApi =
        retrofit.create(LordsVotesApi::class.java)
}
