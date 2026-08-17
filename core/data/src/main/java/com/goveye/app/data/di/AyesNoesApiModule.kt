package com.goveye.app.data.di

import com.goveye.app.data.api.AyesNoesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object AyesNoesApiModule {
    @Provides
    @Singleton
    fun provideAyesNoesApi(@Named("ayesNoesApi") retrofit: Retrofit): AyesNoesApi =
        retrofit.create(AyesNoesApi::class.java)
}
