package com.tmaster

import android.app.Application
import com.tmaster.data.db.AppDatabase
import com.tmaster.data.repository.GameRepository
import com.tmaster.engine.ModelManager
import com.tmaster.log.TLogger
import ikatagosdk.NativeLoader

class TmasterApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var gameRepo: GameRepository
        private set
    lateinit var modelManager: ModelManager
        private set

    override fun onCreate() {
        super.onCreate()
        TLogger.i("App", "Tmaster starting...")

        try {
            NativeLoader.ensureLoaded()
            TLogger.i("App", "Native libraries loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            TLogger.e("App", "Failed to load native libraries: ${e.message}")
        }

        database = AppDatabase.getInstance(this)
        gameRepo = GameRepository(database.gameDao())
        modelManager = ModelManager(this)
    }
}
