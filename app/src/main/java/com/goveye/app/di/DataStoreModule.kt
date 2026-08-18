package com.goveye.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")
private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_preferences")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    @Named("theme")
    fun provideThemeDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.themeDataStore

    @Provides
    @Singleton
    @Named("notification")
    fun provideNotificationDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.notificationDataStore
}
