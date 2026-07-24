package com.tmaster.log

import android.util.Log

object TLogger {
    var minLevel: LogLevel = LogLevel.VERBOSE

    fun v(tag: String, msg: String) {
        LogCollector.add(tag, "V", msg)
        FileLogger.v(tag, msg)
        if (minLevel.priority <= LogLevel.VERBOSE.priority) Log.v(tag, msg)
    }
    fun d(tag: String, msg: String) {
        LogCollector.add(tag, "D", msg)
        FileLogger.d(tag, msg)
        if (minLevel.priority <= LogLevel.DEBUG.priority) Log.d(tag, msg)
    }
    fun i(tag: String, msg: String) {
        LogCollector.add(tag, "I", msg)
        FileLogger.i(tag, msg)
        if (minLevel.priority <= LogLevel.INFO.priority) Log.i(tag, msg)
    }
    fun w(tag: String, msg: String, t: Throwable? = null) {
        LogCollector.add(tag, "W", msg)
        FileLogger.w(tag, msg, t)
        if (minLevel.priority <= LogLevel.WARN.priority) {
            if (t != null) Log.w(tag, msg, t) else Log.w(tag, msg)
        }
    }
    fun e(tag: String, msg: String, t: Throwable? = null) {
        LogCollector.add(tag, "E", msg)
        FileLogger.e(tag, msg, t)
        if (t != null) Log.e(tag, msg, t) else Log.e(tag, msg)
    }
}

class ModuleLogger(private val tag: String) {
    fun v(msg: String) = TLogger.v(tag, msg)
    fun d(msg: String) = TLogger.d(tag, msg)
    fun i(msg: String) = TLogger.i(tag, msg)
    fun w(msg: String, t: Throwable? = null) = TLogger.w(tag, msg, t)
    fun e(msg: String, t: Throwable? = null) = TLogger.e(tag, msg, t)
}
