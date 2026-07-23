package com.tmaster.ui.play

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tmaster.engine.EngineManager
import com.tmaster.game.*
import com.tmaster.log.ModuleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 对弈 ViewModel — 管理棋盘、落子、AI 回应。
 */
class PlayViewModel(app: Application) : AndroidViewModel(app) {
    private val logger = ModuleLogger("PlayVM")

    // 用户设置 (后续从 DataStore 读取)
    var aiColor: StoneColor = StoneColor.WHITE  // 用户执黑
    var boardSize: Int = 19
    var komi: Double = 6.5
    var aiStrength: String = "3d"          // 对应 maxVisits 的值
    var playStyle: String = "balanced"      // balanced/aggressive/solid

    // 棋盘状态
    private val _boardState = MutableStateFlow(
        BoardState.empty(boardSize, komi)
    )
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

    init {
        // 同步引擎状态
        viewModelScope.launch {
            EngineManager.state.collect { state ->
                _engineState.value = state
                if (state == EngineManager.State.READY) {
                    _message.value = "引擎就绪，点击棋盘开始对局"
                    // 如果用户执黑，黑先，不用 AI 走
                    // 如果用户执白，AI 走黑先
                    if (aiColor == StoneColor.WHITE) {
                        aiMove()
                    }
                } else if (state == EngineManager.State.ERROR) {
                    _message.value = "引擎启动失败: ${EngineManager.errorMsg.value}"
                }
            }
        }

        // 启动引擎
        initializeEngine()
    }

    private fun initializeEngine() {
        viewModelScope.launch {
            _message.value = "正在启动引擎..."
            try {
                EngineManager.setup(getApplication(), boardSize, komi)
            } catch (e: Exception) {
                _message.value = "引擎启动失败: ${e.message}"
            }
        }
    }

    /** 用户落子 */
    fun onUserTap(coord: Coord) {
        if (coord.isPass) return
        if (_aiThinking.value) return  // AI 正在思考，忽略

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

        // AI 回应
        aiMove()
    }

    /** 用户 Pass */
    fun onPass() {
        if (_aiThinking.value) return
        val state = _boardState.value
        if (state.currentPlayer == aiColor) return

        _boardState.value = state.pass()

        // 检查连 Pass 结束
        val lastMove = state.lastMove
        if (lastMove?.coord?.isPass == true) {
            _message.value = "双方 Pass，对局结束"
            return
        }

        aiMove()
    }

    /** 撤销 */
    fun undo() {
        val history = _boardState.value.moveHistory
        if (history.size < 2) return
        // 回退 2 步 (用户 + AI)
        var state = BoardState.empty(boardSize, komi)
        for (i in 0 until history.size - 2) {
            val m = history[i]
            state = if (m.coord.isPass) state.pass()
            else state.play(m.coord) ?: state.pass()
        }
        _boardState.value = state
        _lastAiMove.value = null
    }

    /** 新游戏 */
    fun newGame() {
        _boardState.value = BoardState.empty(boardSize, komi)
        _lastAiMove.value = null
        _message.value = "新对局开始"
        if (aiColor == StoneColor.WHITE) aiMove()
    }

    /** 认输 */
    fun resign() {
        _boardState.value = _boardState.value.resign()
        _message.value = "认输"
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
                }
                _message.value = null
            } catch (e: Exception) {
                _message.value = "AI 出错: ${e.message}"
                logger.e { "AI move failed: ${e.message}" }
            } finally {
                _aiThinking.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 不关闭引擎 — EngineManager 是全局单例
    }
}
