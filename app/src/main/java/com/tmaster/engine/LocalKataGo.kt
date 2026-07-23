package com.tmaster.engine

import com.tmaster.error.TmasterException
import com.tmaster.game.BoardState
import com.tmaster.game.Coord
import com.tmaster.log.ModuleLogger
import go.ikatagosdk.gojni.KataGoBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalKataGo(
    override val id: String = "katago-local",
    private val modelPath: String,
    private val configPath: String,
    private val threads: Int = 4,
) : GtpEngine {
    private val logger = ModuleLogger("KataGo")

    override val name = "KataGo (Local)"

    @Volatile
    override var isReady: Boolean = false
        private set

    private var initialized = false

    override suspend fun initialize(boardSize: Int, komi: Double) {
        if (initialized) return
        logger.i("initializing local KataGo (board=$boardSize)")

        try {
            val ok = KataGoBridge.init(modelPath, configPath, boardSize)
            if (!ok) throw TmasterException.EngineNotFound(modelPath)

            sendGtp(GtpProtocol.boardsize(boardSize))
            sendGtp(GtpProtocol.komi(komi))
            sendGtp(GtpProtocol.kataSetRule(true))

            initialized = true
            isReady = true
            logger.i("KataGo initialized")
        } catch (e: Exception) {
            logger.e("KataGo init failed: ${e.message}")
            throw TmasterException.EngineCrashed(-1, e.message ?: "")
        }
    }

    override fun analyze(state: BoardState): Flow<KataAnalysisResult> = flow {
        syncBoard(state)
        sendGtp("kata-analyze interval 100")

        var lastVisits = 0
        while (true) {
            delay(100)
            // 轮询 KataGo 分析输出 — 这里需要 libgojni 提供正确的回调机制
            // 临时: 返回空结果
            emit(KataAnalysisResult(
                moveInfos = emptyList(), rootWinRate = 0.5,
                rootScoreLead = 0.0, totalVisits = 0, isDuringSearch = true,
            ))
            if (lastVisits > 0) break
        }
    }

    override suspend fun generateMove(state: BoardState, temperature: Double): Coord {
        syncBoard(state)
        val color = if (state.currentPlayer == com.tmaster.game.StoneColor.BLACK) "B" else "W"
        val resp = sendGtp(GtpProtocol.genmove(color))

        return if (resp is GtpResponse.Success) {
            Coord.fromSgf(resp.content.trim())
        } else {
            Coord.PASS
        }
    }

    override suspend fun playMove(state: BoardState) {
        val lastMove = state.lastMove ?: return
        val color = if (lastMove.player == com.tmaster.game.StoneColor.BLACK) "B" else "W"
        sendGtp(GtpProtocol.play(color, lastMove.coord.toSgf(state.boardSize)))
    }

    override suspend fun stopAnalysis() {
        sendGtp("kata-stop")
    }

    override suspend fun dispose() {
        KataGoBridge.destroy()
        initialized = false
        isReady = false
        logger.i("KataGo disposed")
    }

    private fun syncBoard(state: BoardState) {
        sendGtp(GtpProtocol.clearBoard())
        for (move in state.moveHistory) {
            val color = if (move.player == com.tmaster.game.StoneColor.BLACK) "B" else "W"
            sendGtp(GtpProtocol.play(color, move.coord.toSgf(state.boardSize)))
        }
    }

    private fun sendGtp(cmd: String): GtpResponse {
        logger.d("GTP -> $cmd")
        val raw = KataGoBridge.sendGtp(cmd)
        logger.d("GTP <- $raw")
        return GtpProtocol.parseResponse(raw)
    }
}
