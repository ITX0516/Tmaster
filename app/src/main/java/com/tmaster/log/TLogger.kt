package com.tmaster.log

import android.util.Log

/**
 * 统一日志系统 — 替代 Timber 风格的轻量实现。
 *
 * 所有模块通过 [TLogger] 输出日志，自动带 tag 前缀方便过滤。
 * release 构建时 [minLevel] 设为 [LogLevel.WARN] 减少输出。
 */
object TLogger {

    var minLevel: LogLevel = LogLevel.VERBOSE

    fun v(tag: String, msg: () -> String) {
        if (minLevel.priority <= LogLevel.VERBOSE.priority) Log.v(tag, msg())
    }

    fun d(tag: String, msg: () -> String) {
        if (minLevel.priority <= LogLevel.DEBUG.priority) Log.d(tag, msg())
    }

    fun i(tag: String, msg: () -> String) {
        if (minLevel.priority <= LogLevel.INFO.priority) Log.i(tag, msg())
    }

    fun w(tag: String, msg: () -> String, t: Throwable? = null) {
        if (minLevel.priority <= LogLevel.WARN.priority) {
            if (t != null) Log.w(tag, msg(), t) else Log.w(tag, msg())
        }
    }

    fun e(tag: String, msg: () -> String, t: Throwable? = null) {
        if (t != null) Log.e(tag, msg(), t) else Log.e(tag, msg())
    }
}

/** 便捷扩展 — 各模块在 companion object 中定义 TAG，然后调用 logger.xxx { "msg" } */
class ModuleLogger(private val tag: String) {
    fun v(msg: () -> String) = TLogger.v(tag, msg)
    fun d(msg: () -> String) = TLogger.d(tag, msg)
    fun i(msg: () -> String) = TLogger.i(tag, msg)
    fun w(msg: () -> String, t: Throwable? = null) = TLogger.w(tag, msg, t)
    fun e(msg: () -> String, t: Throwable? = null) = TLogger.e(tag, msg, t)
}
