package com.tmaster.game

import com.tmaster.log.ModuleLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 对弈流程控制 —— 管理棋盘状态、走子、撤销、计时器。
 */
class GameController(initialState: BoardState) {
    private val logger = ModuleLogger("GameCtrl")
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<BoardState> = _state

    private var timer: GameTimer? = null

    /** 落子，返回是否成功。 */
    fun play(coord: Coord): Boolean {
        val current = _state.value
        val next = current.play(coord)
        if (next == null) {
            logger.d("invalid move: $coord (${current.currentPlayer})")
            return false
        }
        _state.value = next
        timer?.tap()
        return true
    }

    fun pass(): Boolean {
        _state.value = _state.value.pass()
        timer?.tap()
        return true
    }

    fun resign() {
        _state.value = _state.value.resign()
        timer?.stop()
    }

    fun startTimer(config: TimerConfig) {
        timer = GameTimer(config)
        // launch in coroutine scope managed by ViewModel
    }

    fun pauseTimer() = timer?.pause()
    fun resumeTimer(scope: kotlinx.coroutines.CoroutineScope) = timer?.start(scope)
}
