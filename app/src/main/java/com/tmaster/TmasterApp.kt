package com.tmaster

import android.app.Application
import com.tmaster.data.db.AppDatabase
import com.tmaster.data.repository.GameRepository
import com.tmaster.engine.ModelManager
import com.tmaster.log.CrashHandler
import com.tmaster.log.FileLogger
import com.tmaster.log.TLogger

class TmasterApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var gameRepo: GameRepository
        private set
    lateinit var modelManager: ModelManager
        private set

    override fun onCreate() {
        super.onCreate()

        // 最先初始化文件日志和崩溃捕获
        FileLogger.init(this)
        CrashHandler.init(this)

        TLogger.i("App", "Tmaster starting...")
        TLogger.i("App", "Files dir: ${filesDir.absolutePath}")
        TLogger.i("App", "Log file: ${FileLogger.getLogFile()?.absolutePath}")
        FileLogger.flush()

        // 注意：不在 Application.onCreate 中预加载 native 库
        // 因为 native 库加载失败可能导致 native 崩溃，使应用无法启动
        // native 库将在第一次使用时延迟加载
        TLogger.i("App", "Native libraries will be loaded on first use")
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

        TLogger.i("App", "Application onCreate complete")
        FileLogger.flush()
    }
}
