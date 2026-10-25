package com.so2matchtracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "matches",
    indices = [Index(value = ["playedAt"])],
)
data class MatchEntity(
    @PrimaryKey val id: String,
    val result: String,
    val kills: Int,
    val deaths: Int,
    val mapName: String?,
    val mmrChange: Int?,
    val problemCategory: String?,
    val note: String?,
    val playedAt: Long,
)
