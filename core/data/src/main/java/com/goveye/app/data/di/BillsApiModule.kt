package com.goveye.app.data.di

import com.goveye.app.data.api.BillsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object BillsApiModule {
    @Provides
    @Singleton
    fun provideBillsApi(@Named("billsApi") retrofit: Retrofit): BillsApi = retrofit.create(BillsApi::class.java)
}
