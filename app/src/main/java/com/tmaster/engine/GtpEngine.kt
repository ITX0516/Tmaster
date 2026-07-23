package com.tmaster.engine

import com.tmaster.game.BoardState
import com.tmaster.game.Coord
import com.tmaster.game.StoneColor
import kotlinx.coroutines.flow.Flow

/**
 * AI 引擎抽象 — 本地和远程共用一个接口。
 * 通过 GTP 协议与 KataGo 通信。
 */
interface GtpEngine {

    /** 引擎唯一标识。 */
    val id: String

    /** 引擎名称 (e.g. "KataGo-Local", "KataGo-Remote-智星云")。 */
    val name: String

    /** 是否就绪可接受指令。 */
    val isReady: Boolean

    /**
     * 初始化引擎 — 启动进程/建立连接、设置棋盘大小、加载权重。
     * @throws TmasterException.EngineNotFound, .ModelNotLoaded
     */
    suspend fun initialize(boardSize: Int, komi: Double)

    /**
     * 分析当前位置 — 返回候选选点流（支持中间结果推送，类 Lizzie 体验）。
     * 流中每次 emit 都是更深的搜索结果，UI 层逐步更新显示。
     */
    fun analyze(state: BoardState): Flow<KataAnalysisResult>

    /**
     * 生成一手棋 — 对弈模式使用。
     * @param temperature 0.0=确定性最强, 1.0=随机性最强
     */
    suspend fun generateMove(state: BoardState, temperature: Double = 0.0): Coord

    /** 同步走子 — 告诉引擎对方下了什么。 */
    suspend fun playMove(state: BoardState)

    /** 停止当前分析。 */
    suspend fun stopAnalysis()

    /** 释放资源。 */
    suspend fun dispose()
}

/**
 * 从 KataGo 分析 JSON 解析后的结构化结果。
 */
data class KataAnalysisResult(
    val moveInfos: List<KataMoveInfo>,
    val rootWinRate: Double,     // 0.0–1.0
    val rootScoreLead: Double,
    val totalVisits: Int,
    val isDuringSearch: Boolean, // 是否仍在搜索中
)

data class KataMoveInfo(
    val move: String,          // SGF 坐标
    val visits: Int,
    val winRate: Double,
    val scoreLead: Double,
    val order: Int,
    val pv: List<String>,      // 主变化序列 (SGF 坐标)
)
