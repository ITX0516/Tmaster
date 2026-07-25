package com.tmaster

import android.app.Application
import com.tmaster.data.db.AppDatabase
import com.tmaster.data.repository.GameRepository
import com.tmaster.engine.ModelManager
import com.tmaster.log.CrashHandler
import com.tmaster.log.FileLogger
import com.tmaster.log.TLogger
import java.io.File

class TmasterApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var gameRepo: GameRepository
        private set
    lateinit var modelManager: ModelManager
        private set

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        try {
            FileLogger.init(base)
            TLogger.i("App", "attachBaseContext: FileLogger initialized")
            FileLogger.flush()
        } catch (e: Throwable) {
            android.util.Log.e("TmasterApp", "attachBaseContext init failed", e)
        }
    }

    override fun onCreate() {
        super.onCreate()

        try {
            CrashHandler.init(this)
            TLogger.i("App", "CrashHandler initialized")
        } catch (e: Throwable) {
            TLogger.e("App", "Failed to init CrashHandler: ${e.message}", e)
        }
        FileLogger.flush()

        try {
            TLogger.i("App", "Tmaster starting...")
            TLogger.i("App", "Package: $packageName")

            val extDir = getExternalFilesDir(null)
            if (extDir != null) {
                val logDir = File(extDir.parentFile, "log")
                TLogger.i("App", "Log dir: ${logDir.absolutePath}")
            }
            TLogger.i("App", "Internal files dir: ${filesDir.absolutePath}")
        } catch (e: Throwable) {
            TLogger.e("App", "Failed to log paths: ${e.message}", e)
        }
        FileLogger.flush()

        try {
            val appDir = getExternalFilesDir(null) ?: filesDir
            val weightDir = File(appDir, "katago/weights")
            TLogger.i("App", "Weight dir: ${weightDir.absolutePath}, exists=${weightDir.exists()}")
            if (weightDir.exists()) {
                val contents = weightDir.list()
                TLogger.i("App", "Weight dir contents: ${contents?.joinToString() ?: "empty"}")
            }
        } catch (e: Throwable) {
            TLogger.e("App", "Failed to check weight dir: ${e.message}", e)
        }
        FileLogger.flush()

        try {
            TLogger.i("App", "Initializing database...")
            database = AppDatabase.getInstance(this)
            TLogger.i("App", "Database initialized")
        } catch (e: Throwable) {
            TLogger.e("App", "Failed to init database: ${e.message}", e)
        }
        FileLogger.flush()

        try {
            TLogger.i("App", "Initializing game repository...")
            gameRepo = GameRepository(database.gameDao())
            TLogger.i("App", "Game repo initialized")
        } catch (e: Throwable) {
            TLogger.e("App", "Failed to init game repo: ${e.message}", e)
        }
        FileLogger.flush()

        try {
            TLogger.i("App", "Initializing model manager...")
            modelManager = ModelManager(this)
            TLogger.i("App", "Model manager initialized")
        } catch (e: Throwable) {
            TLogger.e("App", "Failed to init model manager: ${e.message}", e)
        }
        FileLogger.flush()

        // 在后台线程预提取权重文件，避免阻塞主线程
        Thread {
            try {
                TLogger.i("App", "Pre-extracting weights (background)...")
                val appDir = getExternalFilesDir(null) ?: filesDir
                val weightDir = File(appDir, "katago/weights")
                if (!weightDir.exists()) weightDir.mkdirs()
                val contents = weightDir.list()
                if (contents == null || contents.isEmpty()) {
                    TLogger.i("App", "Weight dir is empty, extracting...")
                    val names = listOf("w_42", "w_5f", "w_8z", "cu", "kp", "eo", "mw", "pg", "s4")
                    for (name in names) {
                        val resId = resources.getIdentifier(name, "raw", packageName)
                        if (resId != 0) {
                            try {
                                resources.openRawResource(resId).use { input ->
                                    java.util.zip.GZIPInputStream(input).use { gz ->
                                        java.io.FileOutputStream(File(weightDir, name)).use { out ->
                                            gz.copyTo(out)
                                        }
                                    }
                                }
                                TLogger.i("App", "Extracted weight: $name")
                            } catch (e: Exception) {
                                TLogger.e("App", "Failed to extract $name: ${e.message}", e)
                            }
                        } else {
                            TLogger.w("App", "Resource not found: $name")
                        }
                    }
                    TLogger.i("App", "Weight extraction complete")
                } else {
                    TLogger.i("App", "Weights already exist (${contents.size} files), skipping")
                }
                FileLogger.flush()
            } catch (e: Throwable) {
                TLogger.e("App", "Background weight extraction failed: ${e.message}", e)
                FileLogger.flush()
            }
        }.apply {
            name = "WeightExtractor"
            isDaemon = true
            start()
        }

        TLogger.i("App", "Application onCreate complete")
        FileLogger.flush()
    }
}
