package com.goveye.app.data.di

import com.goveye.app.data.api.CommitteesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object CommitteesApiModule {
    @Provides
    @Singleton
    fun provideCommitteesApi(@Named("committeesApi") retrofit: Retrofit): CommitteesApi = retrofit.create(CommitteesApi::class.java)
}
