package com.goveye.app.data.di

import com.goveye.app.data.api.InterestsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object InterestsApiModule {
    @Provides
    @Singleton
    fun provideInterestsApi(@Named("interestsApi") retrofit: Retrofit): InterestsApi = retrofit.create(InterestsApi::class.java)
}
