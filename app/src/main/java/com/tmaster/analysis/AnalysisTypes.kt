package com.tmaster.analysis

import com.tmaster.game.Coord

/**
 * 分析结果 — 一手棋的完整评估。
 */
data class AnalysisResult(
    val moveNumber: Int,
    val candidates: List<MoveCandidate>,
    val rootWinRate: Double,
    val rootScoreLead: Double,
    val totalVisits: Int,
    val depth: Int,
)

data class MoveCandidate(
    val coord: Coord,
    val winRate: Double,
    val scoreLead: Double,
    val visits: Int,
    val order: Int,
    val principalVariation: List<Coord> = emptyList(),
)

/**
 * 问题手严重程度。
 */
enum class ProblemSeverity {
    MINOR,     // < 2% 胜率损失
    MODERATE,  // 2–5%
    MAJOR,     // 5–15%
    CRITICAL,  // > 15%
}

/**
 * 问题手分类。
 */
enum class ProblemCategory {
    DIRECTION,       // 方向错误
    LIFE_AND_DEATH,  // 死活失误
    JOSEKI,          // 定式偏离
    ENDGAME,         // 官子失误
    SHAPE,           // 棋形不佳
    TENUKI,          // 脱先不当
    SENTE_GOTE,      // 先后手判断
    OVERPLAY,        // 过分
    PASSIVE,         // 缓着
    OTHER,
}

/**
 * 一手问题棋。
 */
data class ProblemMove(
    val moveNumber: Int,
    val actualCoord: Coord,
    val bestCoord: Coord,
    val winRateLoss: Double,
    val scoreLoss: Double,
    val severity: ProblemSeverity,
    val category: ProblemCategory,
)

/**
 * 一手好棋。
 */
data class GoodMove(
    val moveNumber: Int,
    val coord: Coord,
    val description: String,
)

/**
 * 全谱分析汇总。
 */
data class GameAnalysis(
    val totalMoves: Int,
    val moveAnalyses: List<AnalysisResult>,
    val problemMoves: List<ProblemMove>,
    val goodMoves: List<GoodMove>,
    val averageWinRateLoss: Double,
    val aiMatchRate: Double,
    val blackAvgWinRate: Double,
    val whiteAvgWinRate: Double,
)
