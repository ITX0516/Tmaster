package com.tmaster.engine

import com.tmaster.error.ErrorReporter
import com.tmaster.error.TmasterException
import com.tmaster.game.BoardState
import com.tmaster.game.Coord
import com.tmaster.log.ModuleLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 本地 KataGo 引擎 — 通过 JNI 调用 libkatago.so。
 *
 * 使用 GTP 协议与引擎通信。分析结果通过流式输出支持增量更新。
 *
 * 模型策略:
 * - 默认使用 15 block 模型
 * - 如果设备性能不足 (e.g. RAM < 2GB 或初始化超时) 自动降级到 8 block
 */
class LocalKataGo(
    override val id: String = "katago-local",
    private val modelPath: String,       // 权重文件路径
    private val configPath: String,      // KataGo 配置文件路径
    private val threads: Int = 4,
) : GtpEngine {
    private val logger = ModuleLogger("KataGo")

    override val name = "KataGo (Local)"

    @Volatile
    override var isReady: Boolean = false
        private set

    private var initialized = false

    // ── JNI 方法声明 ─────────────────────────────────────────
    // 实际实现由 C++ 层提供，通过 NDK 编译进 libkatago-bridge.so
    private external fun nativeInit(modelPath: String, configPath: String, boardSize: Int): Boolean
    private external fun nativeSend(cmd: String): String
    private external fun nativeAnalyzeStart(boardSize: Int): Boolean
    private external fun nativeAnalyzePoll(): String   // returns JSON or ""
    private external fun nativeAnalyzeStop()
    private external fun nativeDestroy()

    override suspend fun initialize(boardSize: Int, komi: Double) {
        if (initialized) return
        logger.i { "initializing local KataGo (board=$boardSize, threads=$threads)" }

        try {
            val ok = nativeInit(modelPath, configPath, boardSize)
            if (!ok) throw TmasterException.EngineNotFound(modelPath)

            // 发送基本设置
            sendGtp(GtpProtocol.boardsize(boardSize))
            sendGtp(GtpProtocol.komi(komi))
            sendGtp(GtpProtocol.kataSetRule(true))

            initialized = true
            isReady = true
            logger.i { "KataGo initialized successfully" }
        } catch (e: TmasterException) {
            // 15b 失败 → 尝试 8b
            if (modelPath.contains("15b")) {
                logger.w { "15b model failed, falling back to 8b" }
                ErrorReporter.report(e, "15b init failed")
                // 外部会重新用 8b 模型路径调用 initialize
            }
            throw e
        } catch (e: Exception) {
            logger.e { "KataGo init failed: ${e.message}" }
            throw TmasterException.EngineCrashed(-1, e.message ?: "")
        }
    }

    override fun analyze(state: BoardState): Flow<KataAnalysisResult> = flow {
        // 同步棋盘状态
        syncBoard(state)

        nativeAnalyzeStart(state.boardSize)

        var lastVisits = 0
        while (true) {
            delay(100)
            val raw = nativeAnalyzePoll()
            if (raw.isEmpty()) {
                if (lastVisits > 0) break // 搜索收敛
                else continue              // 等待首批结果
            }
            val result = parseAnalysis(raw)
            lastVisits = result.totalVisits
            emit(result)
        }
    }

    override suspend fun generateMove(state: BoardState, temperature: Double): Coord {
        syncBoard(state)
        val color = if (state.currentPlayer == com.tmaster.game.StoneColor.BLACK) "B" else "W"
        val cmd = GtpProtocol.genmove(color)
        val resp = sendGtp(cmd)

        return if (resp is GtpResponse.Success) {
            Coord.fromSgf(resp.content)
        } else {
            Coord.PASS
        }
    }

    override suspend fun playMove(state: BoardState) {
        // 只需要同步最后一步
        val lastMove = state.lastMove ?: return
        val color = if (lastMove.player == com.tmaster.game.StoneColor.BLACK) "B" else "W"
        val cmd = GtpProtocol.play(color, lastMove.coord.toSgf(state.boardSize))
        sendGtp(cmd)
    }

    override suspend fun stopAnalysis() {
        nativeAnalyzeStop()
    }

    override suspend fun dispose() {
        nativeDestroy()
        initialized = false
        isReady = false
        logger.i { "KataGo disposed" }
    }

    // ── 内部方法 ──────────────────────────────────────────────

    private fun syncBoard(state: BoardState) {
        sendGtp(GtpProtocol.clearBoard())
        for (move in state.moveHistory) {
            val color = if (move.player == com.tmaster.game.StoneColor.BLACK) "B" else "W"
            sendGtp(GtpProtocol.play(color, move.coord.toSgf(state.boardSize)))
        }
    }

    private fun sendGtp(cmd: String): GtpResponse {
        logger.d { "GTP → $cmd" }
        val raw = nativeSend(cmd)
        logger.d { "GTP ← $raw" }
        return GtpProtocol.parseResponse(raw)
    }

    private fun parseAnalysis(rawJson: String): KataAnalysisResult {
        if (rawJson.isBlank()) return KataAnalysisResult(
            moveInfos = emptyList(), rootWinRate = 0.5,
            rootScoreLead = 0.0, totalVisits = 0, isDuringSearch = true,
        )
        return try {
            val root = org.json.JSONObject(rawJson)
            val rootInfo = root.optJSONObject("rootInfo")
            val infos = root.optJSONArray("moveInfos")
            val moveInfos = mutableListOf<KataMoveInfo>()
            if (infos != null) {
                for (i in 0 until infos.length()) {
                    val info = infos.getJSONObject(i)
                    val pvArr = info.optJSONArray("pv")
                    val pv = mutableListOf<String>()
                    if (pvArr != null) {
                        for (j in 0 until pvArr.length()) pv.add(pvArr.getString(j))
                    }
                    moveInfos.add(KataMoveInfo(
                        move = info.optString("move", ""),
                        visits = info.optInt("visits", 0),
                        winRate = info.optDouble("winrate", 0.5),
                        scoreLead = info.optDouble("scoreLead", 0.0),
                        order = info.optInt("order", i),
                        pv = pv,
                    ))
                }
            }
            KataAnalysisResult(
                moveInfos = moveInfos,
                rootWinRate = rootInfo?.optDouble("winrate", 0.5) ?: 0.5,
                rootScoreLead = rootInfo?.optDouble("scoreLead", 0.0) ?: 0.0,
                totalVisits = rootInfo?.optInt("visits", 0) ?: 0,
                isDuringSearch = root.optBoolean("isDuringSearch", true),
            )
        } catch (e: Exception) {
            logger.e { "parse analysis failed: ${e.message}" }
            KataAnalysisResult(
                moveInfos = emptyList(), rootWinRate = 0.5,
                rootScoreLead = 0.0, totalVisits = 0, isDuringSearch = true,
            )
        }
    }

    companion object {
        init {
            System.loadLibrary("katago-bridge")
        }
    }
}
