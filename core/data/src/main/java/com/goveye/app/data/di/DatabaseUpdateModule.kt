package com.goveye.app.data.di

import com.goveye.app.data.update.DatabaseUpdateApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import retrofit2.Retrofit

/**
 * Hilt module providing the database update API (D-06, DATA-03).
 *
 * [DatabaseUpdateManager] and [DatabasePreferences] are constructed by Hilt
 * via their @Inject constructors. Only the Retrofit interface needs a
 * @Provides method.
 *
 * Follows the [MembersApiModule] pattern.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseUpdateModule {
    @Provides
    @Singleton
    fun provideDatabaseUpdateApi(@Named("githubApi") retrofit: Retrofit): DatabaseUpdateApi =
        retrofit.create(DatabaseUpdateApi::class.java)
}
