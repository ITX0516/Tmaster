package com.tmaster.data.db

import androidx.room.*
import com.tmaster.data.model.GameRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun allGames(): Flow<List<GameRecord>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: String): GameRecord?

    @Query("SELECT * FROM games WHERE blackPlayer LIKE '%' || :player || '%' OR whitePlayer LIKE '%' || :player || '%' ORDER BY createdAt DESC")
    fun findByPlayer(player: String): Flow<List<GameRecord>>

    @Query("SELECT * FROM games WHERE source = :source ORDER BY createdAt DESC")
    fun findBySource(source: String): Flow<List<GameRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: GameRecord)

    @Delete
    suspend fun delete(game: GameRecord)
}
