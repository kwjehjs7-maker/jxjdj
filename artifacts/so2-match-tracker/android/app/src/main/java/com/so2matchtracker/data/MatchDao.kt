package com.so2matchtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY playedAt DESC")
    fun observeAll(): Flow<List<MatchEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(match: MatchEntity)

    @Update
    suspend fun update(match: MatchEntity)

    @Delete
    suspend fun delete(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(matches: List<MatchEntity>): List<Long>

}
