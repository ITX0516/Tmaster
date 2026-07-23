package com.tmaster.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 计时规则类型。
 */
enum class TimerType {
    ABSOLUTE,   // 包干制
    BYO_YOMI,   // 读秒
    CANADIAN,   // 加拿大式 (N 手 / M 分钟)
}

data class TimerConfig(
    val type: TimerType = TimerType.BYO_YOMI,
    val mainTimeSec: Int = 600,    // 基本用时 (秒)
    val byoYomiPeriods: Int = 3,  // 读秒次数
    val byoYomiTimeSec: Int = 30, // 每次读秒时间 (秒)
    val canadianMoves: Int = 25,  // 加拿大式 N 手
    val canadianTimeSec: Int = 600, // 加拿大式 M 分钟
)

data class TimerState(
    val blackTimeMs: Long,
    val whiteTimeMs: Long,
    val blackPeriods: Int,
    val whitePeriods: Int,
    val isRunning: Boolean,
    val activePlayer: StoneColor?,
)

class GameTimer(private val config: TimerConfig) {
    private val _state = MutableStateFlow(TimerState(
        blackTimeMs = config.mainTimeSec * 1000L,
        whiteTimeMs = config.mainTimeSec * 1000L,
        blackPeriods = config.byoYomiPeriods,
        whitePeriods = config.byoYomiPeriods,
        isRunning = false,
        activePlayer = null,
    ))
    val state: StateFlow<TimerState> = _state

    private var running = false
    private var job: kotlinx.coroutines.Job? = null

    fun start(scope: kotlinx.coroutines.CoroutineScope) {
        running = true
        job = scope.launch {
            _state.value = _state.value.copy(isRunning = true, activePlayer = StoneColor.BLACK)
            while (running) {
                delay(100)
                val s = _state.value
                val player = s.activePlayer ?: continue
                val newTime = if (player == StoneColor.BLACK) s.blackTimeMs - 100 else s.whiteTimeMs - 100
                if (newTime <= 0) {
                    handleTimeout(player)
                    break
                }
                _state.value = if (player == StoneColor.BLACK) s.copy(blackTimeMs = newTime)
                else s.copy(whiteTimeMs = newTime)
            }
        }
    }

    fun tap() {
        val s = _state.value
        val nextPlayer = (s.activePlayer ?: StoneColor.BLACK).opposite()
        _state.value = s.copy(activePlayer = nextPlayer)
    }

    fun pause() {
        running = false
        _state.value = _state.value.copy(isRunning = false)
    }

    fun stop() {
        running = false
        _state.value = _state.value.copy(isRunning = false, activePlayer = null)
    }

    private fun handleTimeout(player: StoneColor) {
        running = false
        val s = _state.value
        if (config.type == TimerType.BYO_YOMI) {
            val newPeriods = if (player == StoneColor.BLACK) s.blackPeriods - 1 else s.whitePeriods - 1
            if (newPeriods > 0) {
                val resetTime = config.byoYomiTimeSec * 1000L
                _state.value = if (player == StoneColor.BLACK)
                    s.copy(blackTimeMs = resetTime, blackPeriods = newPeriods)
                else
                    s.copy(whiteTimeMs = resetTime, whitePeriods = newPeriods)
                running = true
                return
            }
        }
        // 超时负
        _state.value = s.copy(isRunning = false)
    }
}
