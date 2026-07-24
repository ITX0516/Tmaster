package com.tmaster.log

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 文件日志写入器 — 将日志写入文件，方便闪退后续排查。
 *
 * 使用独立线程异步写入，避免阻塞主线程。
 */
object FileLogger {
    private const val MAX_LOG_SIZE = 2 * 1024 * 1024 // 2MB
    private const val MAX_LOG_FILES = 3

    @Volatile
    private var logFile: File? = null
    private val queue = LinkedBlockingQueue<String>()
    private val running = AtomicBoolean(false)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val syncLock = Any()

    fun init(context: Context) {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) logDir.mkdirs()
        logFile = File(logDir, "tmaster.log")
        rotateLogs(logDir)
        startWriterThread()
        // 初始化时立即写一条启动日志，同步写入确保不丢失
        writeSync("I", "FileLogger", "Log file initialized: ${logFile?.absolutePath}")
    }

    fun getLogFile(): File? = logFile

    fun getLogDir(): File? = logFile?.parentFile

    fun v(tag: String, msg: String) = add("V", tag, msg, false)
    fun d(tag: String, msg: String) = add("D", tag, msg, false)
    fun i(tag: String, msg: String) = add("I", tag, msg, false)
    fun w(tag: String, msg: String, t: Throwable? = null) {
        add("W", tag, msg, false)
        t?.let { add("W", tag, getStackTraceString(it), false) }
    }
    fun e(tag: String, msg: String, t: Throwable? = null) {
        add("E", tag, msg, true)
        t?.let { add("E", tag, getStackTraceString(it), true) }
    }

    fun flush() {
        // 把队列中所有日志同步写入
        val drained = mutableListOf<String>()
        queue.drainTo(drained)
        if (drained.isNotEmpty()) {
            synchronized(syncLock) {
                try {
                    val f = logFile ?: return
                    f.appendText(drained.joinToString(""))
                    checkRotate()
                } catch (_: Exception) {}
            }
        }
    }

    private fun add(level: String, tag: String, msg: String, sync: Boolean) {
        val time = dateFormat.format(Date())
        val line = "$time [$level] $tag: $msg\n"
        if (sync) {
            writeSyncLine(line)
        } else {
            queue.offer(line)
        }
    }

    private fun writeSync(level: String, tag: String, msg: String) {
        val time = dateFormat.format(Date())
        val line = "$time [$level] $tag: $msg\n"
        writeSyncLine(line)
    }

    private fun writeSyncLine(line: String) {
        synchronized(syncLock) {
            try {
                val f = logFile ?: return
                f.appendText(line)
                checkRotate()
            } catch (_: Exception) {}
        }
    }

    private fun startWriterThread() {
        if (running.getAndSet(true)) return
        Thread {
            while (true) {
                try {
                    val line = queue.take()
                    logFile?.appendText(line)
                    checkRotate()
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    // 日志写入失败不能再打日志，避免死循环
                }
            }
        }.apply {
            name = "FileLogger"
            isDaemon = true
            start()
        }
    }

    private fun checkRotate() {
        val f = logFile ?: return
        if (f.length() < MAX_LOG_SIZE) return
        rotateLogs(f.parentFile ?: return)
    }

    private fun rotateLogs(dir: File) {
        // 滚动: log.2 -> delete, log.1 -> log.2, log -> log.1
        for (i in MAX_LOG_FILES - 1 downTo 1) {
            val old = File(dir, "tmaster.log.$i")
            if (old.exists()) {
                if (i == MAX_LOG_FILES - 1) {
                    old.delete()
                } else {
                    old.renameTo(File(dir, "tmaster.log.${i + 1}"))
                }
            }
        }
        val current = File(dir, "tmaster.log")
        if (current.exists()) {
            current.renameTo(File(dir, "tmaster.log.1"))
        }
    }

    private fun getStackTraceString(t: Throwable): String {
        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }
}
