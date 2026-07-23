package com.tmaster.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameRecord(
    @PrimaryKey val id: String,
    val sgfData: String,
    val blackPlayer: String? = null,
    val whitePlayer: String? = null,
    val result: String? = null,
    val datePlayed: String? = null,
    val source: String? = null, // "local", "fox", "ogs", etc.
    val boardSize: Int = 19,
    val aiMatchRate: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
