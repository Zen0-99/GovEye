package com.goveye.app.data.di

import com.goveye.app.data.api.HansardApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object HansardApiModule {
    @Provides
    @Singleton
    fun provideHansardApi(@Named("hansardApi") retrofit: Retrofit): HansardApi = retrofit.create(HansardApi::class.java)
}
