package com.goveye.app.data.di

import com.goveye.app.data.api.MembersApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object MembersApiModule {
    @Provides
    @Singleton
    fun provideMembersApi(@Named("membersApi") retrofit: Retrofit): MembersApi = retrofit.create(MembersApi::class.java)
}
