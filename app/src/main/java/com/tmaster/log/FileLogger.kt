package com.tmaster.log

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

object FileLogger {
    private const val MAX_LOG_SIZE = 2 * 1024 * 1024 // 2MB
    private const val MAX_LOG_FILES = 3
    private const val LOG_DIR_NAME = "log"
    private const val LOG_FILE_NAME = "log.txt"

    @Volatile
    private var logFile: File? = null
    private val queue = LinkedBlockingQueue<String>()
    private val running = AtomicBoolean(false)
    private val syncLock = Any()

    private fun formatTime(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())

    fun init(context: Context) {
        val extDir = context.getExternalFilesDir(null)
        val logDir = if (extDir != null) {
            val parentDir = extDir.parentFile
            val targetDir = File(parentDir, LOG_DIR_NAME)
            if (!targetDir.exists()) targetDir.mkdirs()
            targetDir
        } else {
            val fallback = File(context.filesDir, LOG_DIR_NAME)
            if (!fallback.exists()) fallback.mkdirs()
            fallback
        }
        logFile = File(logDir, LOG_FILE_NAME)
        rotateLogs(logDir)
        startWriterThread()
        writeSyncLine("${formatTime()} [I] FileLogger: Log file initialized: ${logFile?.absolutePath}\n")
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
        val drained = mutableListOf<String>()
        queue.drainTo(drained)
        if (drained.isNotEmpty()) {
            writeLinesSync(drained)
        }
    }

    private fun add(level: String, tag: String, msg: String, sync: Boolean) {
        val line = "${formatTime()} [$level] $tag: $msg\n"
        if (sync) {
            writeSyncLine(line)
        } else {
            queue.offer(line)
        }
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

    private fun writeLinesSync(lines: List<String>) {
        synchronized(syncLock) {
            try {
                val f = logFile ?: return
                f.appendText(lines.joinToString(""))
                checkRotate()
            } catch (_: Exception) {}
        }
    }

    private fun startWriterThread() {
        if (running.getAndSet(true)) return
        Thread {
            val buffer = mutableListOf<String>()
            while (true) {
                try {
                    buffer.clear()
                    buffer.add(queue.take())
                    queue.drainTo(buffer)
                    writeLinesSync(buffer)
                } catch (e: InterruptedException) {
                    break
                } catch (_: Exception) {}
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
        val logFiles = dir.listFiles { _, name ->
            name.startsWith(LOG_FILE_NAME) && name != LOG_FILE_NAME
        }?.sortedBy { it.name }

        logFiles?.forEachIndexed { index, file ->
            val num = logFiles.size - index
            if (num >= MAX_LOG_FILES) {
                file.delete()
            } else {
                file.renameTo(File(dir, "$LOG_FILE_NAME.$num"))
            }
        }

        val current = File(dir, LOG_FILE_NAME)
        if (current.exists()) {
            current.renameTo(File(dir, "$LOG_FILE_NAME.1"))
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
