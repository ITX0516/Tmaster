package com.tmaster.log

import android.content.Context
import kotlin.system.exitProcess

/**
 * 全局崩溃捕获器 — 捕获未处理异常并写入日志文件。
 */
object CrashHandler : Thread.UncaughtExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        TLogger.i("CrashHandler", "CrashHandler initialized")
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            TLogger.e("CrashHandler", "FATAL in thread ${thread.name}", throwable)
            FileLogger.flush()
            // 再给一点时间确保写入
            Thread.sleep(500)
        } catch (_: Exception) {}

        // 交给系统默认处理（弹出崩溃对话框等）
        defaultHandler?.uncaughtException(thread, throwable)
            ?: run {
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(1)
            }
    }
}
