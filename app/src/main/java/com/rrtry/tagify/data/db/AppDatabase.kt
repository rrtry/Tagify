package com.rrtry.tagify.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rrtry.tagify.data.entities.Track

@Database(entities = [Track::class], version = 5)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
}