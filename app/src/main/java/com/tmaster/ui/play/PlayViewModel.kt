package com.tmaster.ui.play

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tmaster.data.model.GameRecord
import com.tmaster.data.repository.GameRepository
import com.tmaster.engine.EngineManager
import com.tmaster.game.*
import com.tmaster.log.ModuleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 对弈 ViewModel — 管理棋盘、落子、AI 回应、棋谱保存。
 */
class PlayViewModel(app: Application) : AndroidViewModel(app) {
    private val logger = ModuleLogger("PlayVM")
    private val gameRepo = (app as com.tmaster.TmasterApp).gameRepo

    // 当前对局配置
    private var config: GameConfig = GameConfig()

    // AI 颜色（用户的对手）
    val aiColor: StoneColor get() = config.userColor.opposite()

    // 棋盘状态
    private val _boardState = MutableStateFlow(BoardState.empty(19, 6.5))
    val boardState: StateFlow<BoardState> = _boardState

    // 引擎状态
    private val _engineState = MutableStateFlow(EngineManager.State.IDLE)
    val engineState: StateFlow<EngineManager.State> = _engineState

    // AI 状态
    private val _aiThinking = MutableStateFlow(false)
    val aiThinking: StateFlow<Boolean> = _aiThinking

    private val _lastAiMove = MutableStateFlow<Coord?>(null)
    val lastAiMove: StateFlow<Coord?> = _lastAiMove

    // 消息提示
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _engineError = MutableStateFlow<String?>(null)
    val engineError: StateFlow<String?> = _engineError

    // 对局结果
    private val _gameResult = MutableStateFlow<String?>(null)
    val gameResult: StateFlow<String?> = _gameResult

    // 是否显示新对局对话框
    private val _showNewGameDialog = MutableStateFlow(true)
    val showNewGameDialog: StateFlow<Boolean> = _showNewGameDialog

    init {
        // 同步引擎状态
        viewModelScope.launch {
            EngineManager.state.collect { state ->
                _engineState.value = state
                if (state == EngineManager.State.READY) {
                    _message.value = "引擎就绪"
                } else if (state == EngineManager.State.ERROR) {
                    _engineError.value = EngineManager.errorMsg.value ?: "引擎启动失败"
                    _message.value = _engineError.value
                }
            }
        }

        // 启动引擎（只启动一次）
        if (EngineManager.state.value == EngineManager.State.IDLE) {
            initializeEngine()
        }
    }

    private fun initializeEngine() {
        viewModelScope.launch {
            _message.value = "正在启动引擎..."
            _engineError.value = null
            try {
                EngineManager.setup(getApplication(), 19, 6.5)
            } catch (e: Exception) {
                _engineError.value = e.message ?: "未知错误"
                _message.value = _engineError.value
            }
        }
    }

    fun retryEngine() {
        _engineError.value = null
        initializeEngine()
    }

    /** 开始新对局 */
    fun startNewGame(newConfig: GameConfig) {
        config = newConfig
        _showNewGameDialog.value = false
        _gameResult.value = null
        _engineError.value = null

        // 重置棋盘
        var state = BoardState.empty(config.boardSize, config.komi)

        // 放置让子
        if (config.handicap > 1) {
            val handicapCoords = getHandicapStones(config.boardSize, config.handicap)
            for (c in handicapCoords) {
                state = state.copyWithGrid(
                    state.stoneAt(c)?.let { state.grid } ?: run {
                        val g = state.grid.clone()
                        g[c.y * config.boardSize + c.x] = StoneColor.BLACK
                        g
                    } ?: state.grid
                )
            }
        }

        _boardState.value = state
        _lastAiMove.value = null
        _message.value = "对局开始"

        // 应用 AI 强度设置
        EngineManager.setAiStrength(config.aiStrength)

        // 黑先，如果 AI 执黑则 AI 先走
        if (aiColor == StoneColor.BLACK) {
            aiMove()
        }
    }

    fun dismissNewGameDialog() {
        _showNewGameDialog.value = false
    }

    fun openNewGameDialog() {
        _showNewGameDialog.value = true
        _gameResult.value = null
    }

    /** 用户落子 */
    fun onUserTap(coord: Coord) {
        if (coord.isPass) return
        if (_aiThinking.value) return
        if (_gameResult.value != null) return

        val state = _boardState.value
        if (state.currentPlayer == aiColor) {
            _message.value = "请等待 AI 落子"
            return
        }

        val next = state.play(coord)
        if (next == null) {
            _message.value = "此处不能落子"
            return
        }

        _boardState.value = next
        _message.value = null

        // 检查对局是否已结束
        if (next.result != null) {
            _gameResult.value = next.result
            saveGame()
            return
        }

        // AI 回应
        aiMove()
    }

    /** 用户 Pass */
    fun onPass() {
        if (_aiThinking.value) return
        if (_gameResult.value != null) return

        val state = _boardState.value
        if (state.currentPlayer == aiColor) return

        val newState = state.pass()
        _boardState.value = newState

        // 检查连 Pass 结束
        if (state.lastMove?.coord?.isPass == true) {
            endGameByPass()
            return
        }

        aiMove()
    }

    /** 认输 */
    fun resign() {
        if (_gameResult.value != null) return
        val result = if (config.userColor == StoneColor.BLACK) "W+R" else "B+R"
        _boardState.value = _boardState.value.copy(result = result)
        _gameResult.value = result
        _message.value = "认输"
        saveGame()
    }

    /** 撤销 */
    fun undo() {
        if (_aiThinking.value) return
        if (_gameResult.value != null) return

        val history = _boardState.value.moveHistory
        if (history.size < 2) return

        // 回退 2 步 (用户 + AI)
        var state = BoardState.empty(config.boardSize, config.komi)
        for (i in 0 until history.size - 2) {
            val m = history[i]
            state = if (m.coord.isPass) state.pass()
            else state.play(m.coord) ?: state.pass()
        }
        _boardState.value = state
        _lastAiMove.value = null
    }

    /** 数棋结束对局 */
    fun scoreAndEnd() {
        if (_gameResult.value != null) return
        val score = _boardState.value.scoreChinese()
        val result = when {
            score > 0 -> "B+$score"
            score < 0 -> "W+${-score}"
            else -> "Draw"
        }
        _boardState.value = _boardState.value.copy(result = result)
        _gameResult.value = result
        _message.value = "对局结束: $result"
        saveGame()
    }

    // ── AI ─────────────────────────────────────────────────

    private fun aiMove() {
        viewModelScope.launch {
            _aiThinking.value = true
            _message.value = "AI 思考中..."
            _lastAiMove.value = null

            try {
                val coord = withContext(Dispatchers.IO) {
                    EngineManager.genMove(_boardState.value)
                }

                val next = _boardState.value.play(coord)
                if (next != null) {
                    _boardState.value = next
                    if (!coord.isPass) _lastAiMove.value = coord

                    // 检查对局是否已结束
                    if (next.result != null) {
                        _gameResult.value = next.result
                        saveGame()
                    }
                }
                _message.value = null
            } catch (e: Exception) {
                _message.value = "AI 出错: ${e.message}"
                logger.e("AI move failed: ${e.message}")
            } finally {
                _aiThinking.value = false
            }
        }
    }

    // ── 让子 ───────────────────────────────────────────────

    private fun getHandicapStones(size: Int, handicap: Int): List<Coord> {
        val stars19 = listOf(
            Coord(3, 3), Coord(3, 9), Coord(3, 15),
            Coord(9, 3), Coord(9, 9), Coord(9, 15),
            Coord(15, 3), Coord(15, 9), Coord(15, 15),
        )
        val stars13 = listOf(
            Coord(3, 3), Coord(3, 6), Coord(3, 9),
            Coord(6, 3), Coord(6, 6), Coord(6, 9),
            Coord(9, 3), Coord(9, 6), Coord(9, 9),
        )
        val stars9 = listOf(
            Coord(2, 2), Coord(2, 4), Coord(2, 6),
            Coord(4, 2), Coord(4, 4), Coord(4, 6),
            Coord(6, 2), Coord(6, 4), Coord(6, 6),
        )
        val stars = when (size) {
            19 -> stars19
            13 -> stars13
            else -> stars9
        }
        return when (handicap) {
            2 -> listOf(stars[0], stars[8])
            3 -> listOf(stars[0], stars[2], stars[6])
            4 -> listOf(stars[0], stars[2], stars[6], stars[8])
            5 -> listOf(stars[0], stars[2], stars[4], stars[6], stars[8])
            6 -> listOf(stars[0], stars[2], stars[3], stars[5], stars[6], stars[8])
            7 -> listOf(stars[0], stars[2], stars[3], stars[4], stars[5], stars[6], stars[8])
            8 -> stars.subList(0, 8)
            9 -> stars
            else -> emptyList()
        }
    }

    // ── 棋谱保存 ───────────────────────────────────────────

    private fun saveGame() {
        val state = _boardState.value
        if (state.moveHistory.isEmpty()) return

        val sgf = SgfWriter.write(state)
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val id = UUID.randomUUID().toString()

        val record = GameRecord(
            id = id,
            sgfData = sgf,
            blackPlayer = if (config.userColor == StoneColor.BLACK) "玩家" else "AI",
            whitePlayer = if (config.userColor == StoneColor.WHITE) "玩家" else "AI",
            result = state.result,
            datePlayed = date,
            source = "local",
            boardSize = config.boardSize,
        )

        viewModelScope.launch {
            try {
                gameRepo.insert(record)
                // 同时保存到 SGF 文件
                val appDir = getApplication<com.tmaster.TmasterApp>().getExternalFilesDir(null)
                if (appDir != null) {
                    val sgfDir = File(appDir, "sgf")
                    sgfDir.mkdirs()
                    val file = File(sgfDir, "game_${id.take(8)}.sgf")
                    file.writeText(sgf)
                    logger.i("Game saved: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                logger.e("Failed to save game: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
