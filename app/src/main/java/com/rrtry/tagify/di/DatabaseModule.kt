package com.rrtry.tagify.di

import com.rrtry.tagify.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context
import androidx.room.Room
import com.rrtry.tagify.DATABASE_NAME
import com.rrtry.tagify.data.db.TrackDao

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    fun provideTrackDao(appDatabase: AppDatabase): TrackDao {
        return appDatabase.trackDao()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }
}