package com.tmaster.data.repository

import com.tmaster.data.db.GameDao
import com.tmaster.data.model.GameRecord
import kotlinx.coroutines.flow.Flow

class GameRepository(private val dao: GameDao) {

    fun allGames(): Flow<List<GameRecord>> = dao.allGames()

    suspend fun getById(id: String): GameRecord? = dao.getById(id)

    fun findByPlayer(player: String): Flow<List<GameRecord>> = dao.findByPlayer(player)

    suspend fun save(game: GameRecord) = dao.insert(game)

    suspend fun delete(game: GameRecord) = dao.delete(game)

    fun search(query: String): Flow<List<GameRecord>> {
        // 简单搜索: 按棋手名匹配
        return dao.findByPlayer(query)
    }
}
