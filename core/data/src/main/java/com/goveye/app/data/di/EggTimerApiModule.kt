package com.goveye.app.data.di

import com.goveye.app.data.api.EggTimerApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object EggTimerApiModule {
    @Provides
    @Singleton
    fun provideEggTimerApi(client: OkHttpClient): EggTimerApi = EggTimerApi(client)
}
