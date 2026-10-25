package com.so2matchtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MatchEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao

    companion object {
        @Volatile
        private var instance: TrackerDatabase? = null

        fun get(context: Context): TrackerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrackerDatabase::class.java,
                    "so2-match-tracker.db",
                ).build().also { instance = it }
            }
    }
}
