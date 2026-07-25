package com.tmaster.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tmaster.data.model.GameRecord
import com.tmaster.data.repository.GameRepository
import com.tmaster.game.SgfNode
import com.tmaster.game.SgfParser
import com.tmaster.game.toBoardState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as com.tmaster.TmasterApp).gameRepo

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val games: StateFlow<List<GameRecord>> = repo.allGames()
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) list
            else list.filter {
                it.blackPlayer.contains(query, ignoreCase = true) ||
                it.whitePlayer.contains(query, ignoreCase = true) ||
                it.result.contains(query, ignoreCase = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) {
        _searchQuery.value = q
    }

    fun deleteGame(game: GameRecord) {
        viewModelScope.launch { repo.delete(game) }
    }

    fun importSgf(file: File) {
        viewModelScope.launch {
            try {
                val text = file.readText()
                val parser = SgfParser(text)
                val root = parser.parse()
                val state = root.toBoardState()

                val black = root.prop("PB") ?: "黑方"
                val white = root.prop("PW") ?: "白方"
                val result = root.prop("RE") ?: state.result ?: "?"
                val date = root.prop("DT") ?: ""
                val size = root.prop("SZ")?.toIntOrNull() ?: 19

                val record = GameRecord(
                    id = UUID.randomUUID().toString(),
                    sgfData = text,
                    blackPlayer = black,
                    whitePlayer = white,
                    result = result,
                    datePlayed = date,
                    source = "import",
                    boardSize = size,
                )
                repo.save(record)
            } catch (_: Exception) {
            }
        }
    }
}
