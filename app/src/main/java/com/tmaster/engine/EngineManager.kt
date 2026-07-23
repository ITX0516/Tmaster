package com.tmaster.engine

import android.content.Context
import com.tmaster.error.ErrorReporter
import com.tmaster.error.TmasterException
import com.tmaster.game.BoardState
import com.tmaster.game.Coord
import com.tmaster.log.ModuleLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * 引擎管理器 — 全局单例，管理 KataGo 生命周期。
 * 负责模型下载、配置生成、引擎初始化和运转。
 */
object EngineManager {
    private val logger = ModuleLogger("EngineMgr")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var engine: LocalKataGo? = null
    private var modelPath: String? = null
    private var configPath: String? = null

    enum class State { IDLE, DOWNLOADING, INITIALIZING, READY, ERROR }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg

    fun getEngine(): GtpEngine? = engine

    /**
     * 一键启动引擎 — 下载模型(如需) → 配置 → 初始化 → READY
     * 15b 失败自动回退 8b。
     */
    suspend fun setup(context: Context, boardSize: Int = 19, komi: Double = 6.5) {
        if (_state.value == State.READY) return
        _state.value = State.INITIALIZING
        _errorMsg.value = null

        try {
            configPath = copyDefaultConfig(context)
            logger.i("config ready: $configPath")

            val modelMgr = ModelManager(context)
            var modelLoaded = false
            for (modelId in listOf("15b", "8b")) {
                try {
                    _state.value = State.DOWNLOADING
                    modelPath = modelMgr.selectModel(modelId)
                    modelLoaded = true
                    logger.i("model $modelId ready")
                    break
                } catch (e: TmasterException.ModelDownloadFailed) {
                    logger.w("$modelId download failed: ${e.message}")
                }
            }
            if (!modelLoaded) {
                throw TmasterException.ModelNotLoaded(
                    "无法下载模型，请检查网络连接",
                )
            }

            _state.value = State.INITIALIZING
            engine = LocalKataGo(modelPath = modelPath!!, configPath = configPath!!)
            engine!!.initialize(boardSize, komi)

            _state.value = State.READY
            logger.i("engine ready: ${modelMgr.currentModel.value?.name}")
        } catch (e: Exception) {
            _state.value = State.ERROR
            _errorMsg.value = e.message ?: "未知错误"
            logger.e("engine setup failed: ${e.message}")
            ErrorReporter.report(e, "engine setup")
        }
    }

    /** AI 下一步棋 — 对弈模式用。 */
    suspend fun genMove(state: BoardState): Coord {
        val eng = engine ?: throw TmasterException.EngineNotFound("not initialized")
        return eng.generateMove(state)
    }

    /** 销毁引擎 — App 退出时调用。 */
    suspend fun shutdown() {
        engine?.dispose()
        engine = null
        _state.value = State.IDLE
        scope.cancel()
    }

    /** 把 assets/katago.cfg 复制到私有目录，返回路径。 */
    private fun copyDefaultConfig(context: Context): String {
        val dest = File(context.filesDir, "katago/katago.cfg")
        dest.parentFile?.mkdirs()
        if (!dest.exists()) {
            context.assets.open("katago.cfg").use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return dest.absolutePath
    }
}
