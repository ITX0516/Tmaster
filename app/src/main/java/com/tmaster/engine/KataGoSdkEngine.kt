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
        syncBoard(state)
        val raw = sendGtpWithResponse("kata-analyze interval 100")
        val result = parseKataAnalyzeResponse(raw)
        emit(result)
    }

    override suspend fun generateMove(state: BoardState, temperature: Double): Coord {
        syncBoard(state)
        val color = if (state.currentPlayer == StoneColor.BLACK) "B" else "W"
        val response = sendGtpWithResponse("genmove $color")
        val parsed = GtpProtocol.parseResponse(response)
        return when (parsed) {
            is GtpResponse.Success -> {
                val content = parsed.content.trim().lowercase()
                when {
                    content == "pass" -> Coord.PASS
                    content == "resign" -> Coord.PASS
                    content.length >= 2 -> Coord.fromSgf(content)
                    else -> Coord.PASS
                }
            }
            is GtpResponse.Error -> Coord.PASS
        }
    }

    override suspend fun playMove(state: BoardState) {
        val last = state.lastMove ?: return
        val color = if (last.player == StoneColor.BLACK) "B" else "W"
        sendGtp("play $color ${last.coord.toSgf(state.boardSize)}")
    }

    override suspend fun stopAnalysis() {
        try { sendGtp("kata-stop") } catch (_: Exception) {}
    }

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
        sendGtpWithResponse(cmd)
    }

    private fun sendGtpWithResponse(cmd: String): String {
        val r = runner ?: throw TmasterException.EngineNotFound("runner null")
        logger.d("GTP → $cmd")
        val response = r.sendGTPCommand("$cmd\n")
        logger.d("GTP ← ${response.take(100)}")
        return response
    }

    private fun parseKataAnalyzeResponse(raw: String): KataAnalysisResult {
        val lines = raw.lines().filter { it.isNotBlank() }
        val jsonLine = lines.firstOrNull { it.trimStart().startsWith("{") }
            ?: return KataAnalysisResult(emptyList(), 0.5, 0.0, 0, false)

        return try {
            val moveInfos = mutableListOf<KataMoveInfo>()
            var rootWinRate = 0.5
            var rootScoreLead = 0.0
            var totalVisits = 0
            var isDuringSearch = true

            val rootWinRateRegex = """"winrate"\s*:\s*([\d.]+)""".toRegex()
            val rootScoreRegex = """"scoreLead"\s*:\s*([-\d.]+)""".toRegex()
            val visitsRegex = """"visits"\s*:\s*(\d+)""".toRegex()

            rootWinRateRegex.find(jsonLine)?.let { rootWinRate = it.groupValues[1].toDouble() }
            rootScoreRegex.find(jsonLine)?.let { rootScoreLead = it.groupValues[1].toDouble() }
            visitsRegex.find(jsonLine)?.let { totalVisits = it.groupValues[1].toInt() }

            val moveInfoRegex = """"moveInfos"\s*:\s*\[(.*?)\]""".toRegex()
            moveInfoRegex.find(jsonLine)?.let { match ->
                val moveStr = match.groupValues[1]
                val singleMoveRegex = """\{[^}]*\}""".toRegex()
                singleMoveRegex.findAll(moveStr).forEachIndexed { idx, moveMatch ->
                    val moveJson = moveMatch.value
                    val moveRegex = """"move"\s*:\s*"([a-z]+)"""".toRegex()
                    val winRegex = """"winrate"\s*:\s*([\d.]+)""".toRegex()
                    val scoreRegex = """"scoreLead"\s*:\s*([-\d.]+)""".toRegex()
                    val pvRegex = """"pv"\s*:\s*\[(.*?)\]""".toRegex()

                    val move = moveRegex.find(moveJson)?.groupValues?.get(1) ?: ""
                    val wr = winRegex.find(moveJson)?.groupValues?.get(1)?.toDouble() ?: 0.0
                    val sl = scoreRegex.find(moveJson)?.groupValues?.get(1)?.toDouble() ?: 0.0
                    val vis = visitsRegex.find(moveJson)?.groupValues?.get(1)?.toInt() ?: 0
                    val pv = pvRegex.find(moveJson)?.groupValues?.get(1)
                        ?.replace("\"", "")?.split(",")?.map { it.trim() } ?: emptyList()

                    if (move.isNotEmpty()) {
                        moveInfos.add(KataMoveInfo(move, vis, wr, sl, idx + 1, pv))
                    }
                }
            }

            KataAnalysisResult(moveInfos, rootWinRate, rootScoreLead, totalVisits, isDuringSearch)
        } catch (e: Exception) {
            logger.e("parse analyze failed: ${e.message}")
            KataAnalysisResult(emptyList(), 0.5, 0.0, 0, false)
        }
    }
}
