package com.goveye.app.data.di

import com.goveye.app.data.api.VotesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object VotesApiModule {
    @Provides
    @Singleton
    fun provideVotesApi(@Named("votesApi") retrofit: Retrofit): VotesApi = retrofit.create(VotesApi::class.java)
}
