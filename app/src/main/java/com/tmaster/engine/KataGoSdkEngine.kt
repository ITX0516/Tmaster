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

        logger.i("=== ENGINE INIT START ===")

        // Step 1: Test library loading
        try {
            logger.i("loading libkatago.so...")
            System.loadLibrary("katago")
            logger.i("libkatago.so loaded OK")
        } catch (e: Throwable) {
            logger.e("FAILED to load libkatago.so: ${e.javaClass.name}: ${e.message}", e)
            throw TmasterException.EngineNotFound("libkatago.so: ${e.message}")
        }

        try {
            logger.i("loading libgojni.so...")
            System.loadLibrary("gojni")
            logger.i("libgojni.so loaded OK")
        } catch (e: Throwable) {
            logger.e("FAILED to load libgojni.so: ${e.javaClass.name}: ${e.message}", e)
            throw TmasterException.EngineNotFound("libgojni.so: ${e.message}")
        }

        // Step 2: Create runner
        try {
            logger.i("creating KatagoRunner...")
            runner = KatagoRunner()
            logger.i("KatagoRunner created OK")
        } catch (e: UnsatisfiedLinkError) {
            logger.e("UNSATISFIED LINK: ${e.message}", e)
            throw TmasterException.EngineNotFound("JNI method missing: ${e.message}")
        } catch (e: Throwable) {
            logger.e("Runner creation failed: ${e.javaClass.name}: ${e.message}", e)
            throw TmasterException.EngineCrashed(-1, "runner create: ${e.message}")
        }

        // Step 3: Configure runner
        try {
            logger.i("configuring runner...")
            runner!!.apply {
                setKataName("KataGo")
                logger.i("  setName OK")
                setKataConfig(configPath)
                logger.i("  setConfig OK ($configPath)")
                setKataWeight(modelPath, configPath)
                logger.i("  setWeight OK ($modelPath)")
                setKataLocalConfig("numSearchThreads", "4")
                logger.i("  setThreads OK")
                setRefreshInterval(100)
                logger.i("  setInterval OK")
            }
        } catch (e: UnsatisfiedLinkError) {
            logger.e("UNSATISFIED LINK during config: ${e.message}", e)
            throw TmasterException.EngineNotFound("JNI config: ${e.message}")
        } catch (e: Throwable) {
            logger.e("Config failed: ${e.javaClass.name}: ${e.message}", e)
            throw TmasterException.EngineCrashed(-1, "config: ${e.message}")
        }

        // Step 4: Start engine
        try {
            logger.i("starting engine (run)...")
            val started = runner!!.run()
            logger.i("run() returned: $started")
            if (!started) {
                throw TmasterException.EngineCrashed(-1, "runner.run() = false")
            }
        } catch (e: UnsatisfiedLinkError) {
            logger.e("UNSATISFIED LINK run(): ${e.message}", e)
            throw TmasterException.EngineNotFound("JNI run: ${e.message}")
        } catch (e: TmasterException) {
            throw e
        } catch (e: Throwable) {
            logger.e("run failed: ${e.javaClass.name}: ${e.message}", e)
            throw TmasterException.EngineCrashed(-1, "run: ${e.message}")
        }

        // Step 5: GTP init
        try {
            logger.i("sending GTP init commands...")
            sendGtp("boardsize $boardSize")
            logger.i("  boardsize OK")
            sendGtp("clear_board")
            logger.i("  clear_board OK")
            sendGtp("komi $komi")
            logger.i("  komi OK")
            sendGtp("kata-set-rules chinese")
            logger.i("  rules OK")
        } catch (e: Throwable) {
            logger.e("GTP init failed: ${e.javaClass.name}: ${e.message}", e)
            throw TmasterException.EngineCrashed(-1, "GTP init: ${e.message}")
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
        return synchronized(gtpLock) {
            val response = runner!!.sendGTPCommand("genmove $color")
            val parsed = GtpProtocol.parseResponse(response)
            when (parsed) {
                is GtpResponse.Success -> Coord.fromSgf(parsed.content.trim())
                else -> {
                    logger.e("genmove failed: response=$response")
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

    override suspend fun stopAnalysis() { sendGtp("kata-stop") }

    override suspend fun dispose() {
        stopAnalysis()
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
        synchronized(gtpLock) {
            val r = runner ?: throw TmasterException.EngineNotFound("runner is null")
            logger.d("GTP → $cmd")
            val response = r.sendGTPCommand(cmd)
            logger.d("GTP ← ${response.take(100)}")
        }
    }
}
