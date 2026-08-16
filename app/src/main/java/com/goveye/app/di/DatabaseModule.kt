package com.goveye.app.di

import android.content.Context
import androidx.room.Room
import com.goveye.app.data.local.GovEyeDatabase
import com.goveye.app.data.local.dao.SearchDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideGovEyeDatabase(@ApplicationContext context: Context): GovEyeDatabase = Room
        .databaseBuilder(
            context,
            GovEyeDatabase::class.java,
            GovEyeDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    @Provides
    fun provideSearchDao(database: GovEyeDatabase): SearchDao = database.searchDao()
}
