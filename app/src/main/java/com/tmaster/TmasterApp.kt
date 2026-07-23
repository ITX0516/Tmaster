package com.tmaster

import android.app.Application
import com.tmaster.data.db.AppDatabase
import com.tmaster.data.repository.GameRepository
import com.tmaster.engine.ModelManager
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
        TLogger.i("App") { "Tmaster starting..." }

        database = AppDatabase.getInstance(this)
        gameRepo = GameRepository(database.gameDao())
        modelManager = ModelManager(this)
    }
}
