package com.tmaster.error

import com.tmaster.log.ModuleLogger

/**
 * 统一错误上报 — 生产环境可接入 Firebase Crashlytics 或 Sentry。
 * 当前版本将错误写入日志，并回调 [onError] 通知 UI 层展示 Toast/Dialog。
 */
object ErrorReporter {
    private val logger = ModuleLogger("ErrorReporter")
    var onError: ((TmasterException) -> Unit)? = null

    fun report(ex: Throwable, context: String = "") {
        when (ex) {
            is TmasterException -> {
                logger.e("[${ex.code}] $context → ${ex.message}")
                onError?.invoke(ex)
            }
            else -> {
                logger.e("UNEXPECTED $context → ${ex.message}")
                logger.e(ex.stackTraceToString())
            }
        }
    }
}
