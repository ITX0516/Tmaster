package com.tmaster.engine

import com.tmaster.error.TmasterException
import com.tmaster.game.BoardState
import com.tmaster.game.Coord
import com.tmaster.game.StoneColor
import com.tmaster.log.ModuleLogger
import ikatagosdk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * KataGo 引擎 — 通过 ikatagosdk (libgojni.so) 对接。
 *
 * 使用 AhQ Go Lite 的预编译 native 库，通过 GTP 协议通信。
 */
class KataGoSdkEngine(
    override val id: String = "katago-sdk",
    private val modelPath: String,
    private val configPath: String,
    private val boardSize: Int = 19,
    private val komi: Double = 6.5,
) : GtpEngine {
    private val logger = ModuleLogger("KataGoSDK")

    override val name = "KataGo (SDK)"

    @Volatile
    override var isReady: Boolean = false
        private set

    private var runner: KatagoRunner? = null
    private var initialized = false
    private val gtpLock = Any()

    override suspend fun initialize(boardSize: Int, komi: Double) {
        if (initialized) return
        logger.i("initializing KataGo SDK (board=$boardSize)")

        try {
            // 创建 runner 并配置
            runner = KatagoRunner().apply {
                setKataName("KataGo")
                setKataConfig(configPath)
                setKataWeight(modelPath, configPath)
                setKataLocalConfig("numSearchThreads", "4")
                setRefreshInterval(100)
            }

            // 启动引擎
            val started = runner!!.run()
            if (!started) {
                throw TmasterException.EngineCrashed(-1, "runner.run() returned false")
            }

            // 基本设置
            sendGtp("boardsize $boardSize")
            sendGtp("clear_board")
            sendGtp("komi $komi")
            sendGtp("kata-set-rules chinese")

            initialized = true
            isReady = true
            logger.i("KataGo SDK ready")
        } catch (e: TmasterException) {
            throw e
        } catch (e: UnsatisfiedLinkError) {
            logger.e("JNI not found: ${e.message}")
            throw TmasterException.EngineNotFound(
                "Native library mismatch. libgojni.so may be from incompatible version.",
            )
        } catch (e: Exception) {
            logger.e("init failed: ${e.message}")
            throw TmasterException.EngineCrashed(-1, e.message ?: "unknown")
        }
    }

    override fun analyze(state: BoardState): Flow<KataAnalysisResult> = flow {
        syncBoard(state)

        sendGtp("kata-analyze interval 100")
        // TODO: parse async analysis results from the runner
        // KatagoRunner may provide results via DataCallback

        var count = 0
        while (count < 50) {
            delay(200)
            // Send another analyze command to get fresh results
            // The actual analysis JSON parsing needs DataCallback integration
            count++
        }
    }

    override suspend fun generateMove(state: BoardState, temperature: Double): Coord {
        syncBoard(state)
        val color = if (state.currentPlayer == StoneColor.BLACK) "B" else "W"

        return synchronized(gtpLock) {
            val response = runner!!.sendGTPCommand("genmove $color")
            val parsed = GtpProtocol.parseResponse(response)
            when (parsed) {
                is GtpResponse.Success -> Coord.fromSgf(parsed.content.trim())
                else -> {
                    logger.e("genmove failed: ${(parsed as GtpResponse.Error).message}")
                    Coord.PASS
                }
            }
        }
    }

    override suspend fun playMove(state: BoardState) {
        val last = state.lastMove ?: return
        val color = if (last.player == StoneColor.BLACK) "B" else "W"
        sendGtp("play $color ${last.coord.toSgf(state.boardSize)}")
    }

    override suspend fun stopAnalysis() {
        sendGtp("kata-stop")
    }

    override suspend fun dispose() {
        stopAnalysis()
        runner = null
        initialized = false
        isReady = false
        logger.i("disposed")
    }

    private fun syncBoard(state: BoardState) {
        sendGtp("clear_board")
        for (move in state.moveHistory) {
            val color = if (move.player == StoneColor.BLACK) "B" else "W"
            sendGtp("play $color ${move.coord.toSgf(state.boardSize)}")
        }
    }

    private fun sendGtp(cmd: String) {
        synchronized(gtpLock) {
            logger.d("GTP → $cmd")
            val response = runner!!.sendGTPCommand(cmd)
            logger.d("GTP ← $response")
        }
    }
}
