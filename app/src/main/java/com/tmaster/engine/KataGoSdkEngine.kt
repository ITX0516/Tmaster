package com.tmaster.engine

import com.tmaster.error.TmasterException
import com.tmaster.game.BoardState
import com.tmaster.game.Coord
import com.tmaster.game.StoneColor
import com.tmaster.log.ModuleLogger
import ikatagosdk.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KataGoSdkEngine(
    override val id: String = "katago-sdk",
    private val weightDir: String,     // 模型权重目录
    private val configPath: String,    // katago.cfg 路径
) : GtpEngine {
    private val logger = ModuleLogger("KataGoSDK")

    override val name = "KataGo (SDK)"
    override var isReady: Boolean = false
    private var runner: KatagoRunner? = null
    private var initialized = false

    override suspend fun initialize(boardSize: Int, komi: Double) {
        if (initialized) return
        logger.i("=== ENGINE INIT ===")

        // Step 1: Create Client (4-arg constructor per AhQ Go decompilation)
        val client = try {
            logger.i("creating Client(\"\", \"local\", \"\", \"\")")
            Client("", "local", "", "")
        } catch (e: UnsatisfiedLinkError) {
            logger.e("Client constructor failed: ${e.message}")
            throw TmasterException.EngineNotFound("JNI Client(): ${e.message}")
        }

        // Step 2: Create runner
        runner = try {
            logger.i("client.createKatagoRunner()...")
            client.createKatagoRunner()
        } catch (e: UnsatisfiedLinkError) {
            logger.e("createKatagoRunner failed: ${e.message}")
            throw TmasterException.EngineNotFound("JNI createKatagoRunner: ${e.message}")
        }

        // Step 3: Configure
        try {
            runner!!.apply {
                setKataName("KataGo")
                logger.i("  setName OK")
                setKataConfig(configPath)
                logger.i("  setConfig OK")
                setKataWeight(weightDir, configPath)
                logger.i("  setWeight OK")
                setKataLocalConfig("numSearchThreads", "4")
                logger.i("  setThreads OK")
                setRefreshInterval(100)
                logger.i("  setInterval OK")
            }
        } catch (e: UnsatisfiedLinkError) {
            logger.e("Config JNI: ${e.message}")
            throw TmasterException.EngineNotFound("JNI config: ${e.message}")
        }

        // Step 4: Start engine
        try {
            logger.i("runner.run()...")
            val started = runner!!.run()
            logger.i("run() = $started")
            if (!started) throw TmasterException.EngineCrashed(-1, "run()=false")
        } catch (e: UnsatisfiedLinkError) {
            logger.e("run() JNI: ${e.message}")
            throw TmasterException.EngineNotFound("JNI run: ${e.message}")
        }

        // Step 5: GTP init (commands need newline per decompilation)
        try {
            sendGtp("boardsize $boardSize")
            sendGtp("clear_board")
            sendGtp("komi $komi")
            sendGtp("kata-set-rules chinese")
            logger.i("GTP init OK")
        } catch (e: Exception) {
            logger.e("GTP init failed: ${e.message}")
            throw TmasterException.EngineCrashed(-1, "GTP: ${e.message}")
        }

        initialized = true
        isReady = true
        logger.i("=== ENGINE READY ===")
    }

    override fun analyze(state: BoardState): Flow<KataAnalysisResult> = flow {
        sendGtp("kata-analyze interval 100")
        var count = 0
        while (count < 50) {
            kotlinx.coroutines.delay(200)
            count++
        }
    }

    override suspend fun generateMove(state: BoardState, temperature: Double): Coord {
        syncBoard(state)
        val color = if (state.currentPlayer == StoneColor.BLACK) "B" else "W"
        val response = runner!!.sendGTPCommand("genmove $color\n")
        logger.d("genmove → $response")
        val parsed = GtpProtocol.parseResponse(response)
        return when (parsed) {
            is GtpResponse.Success -> Coord.fromSgf(parsed.content.trim())
            else -> Coord.PASS
        }
    }

    override suspend fun playMove(state: BoardState) {
        val last = state.lastMove ?: return
        val color = if (last.player == StoneColor.BLACK) "B" else "W"
        sendGtp("play $color ${last.coord.toSgf(state.boardSize)}")
    }

    override suspend fun stopAnalysis() { sendGtp("kata-stop") }

    override suspend fun dispose() {
        try { runner?.stop() } catch (_: Exception) {}
        runner = null
        initialized = false
        isReady = false
    }

    private fun syncBoard(state: BoardState) {
        sendGtp("clear_board")
        for (move in state.moveHistory) {
            val color = if (move.player == StoneColor.BLACK) "B" else "W"
            sendGtp("play $color ${move.coord.toSgf(state.boardSize)}")
        }
    }

    private fun sendGtp(cmd: String) {
        val r = runner ?: throw TmasterException.EngineNotFound("runner null")
        logger.d("GTP → $cmd")
        // GTP commands need trailing newline (per AhQ Go decompilation)
        val response = r.sendGTPCommand("$cmd\n")
        logger.d("GTP ← ${response.take(100)}")
    }
}
