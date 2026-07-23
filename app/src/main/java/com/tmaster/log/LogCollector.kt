package com.tmaster.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 内存日志收集器 — 保留最近 200 条日志供调试面板查看。
 */
object LogCollector {
    private val maxLines = 200
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    fun add(tag: String, level: String, msg: String) {
        val line = "[$level] $tag: $msg"
        val current = _lines.value.toMutableList()
        current.add(line)
        if (current.size > maxLines) {
            current.removeAt(0)
        }
        _lines.value = current
    }
}
