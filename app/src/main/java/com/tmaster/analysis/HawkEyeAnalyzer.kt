package com.tmaster.analysis

import com.tmaster.engine.GtpEngine
import com.tmaster.engine.KataAnalysisResult
import com.tmaster.game.BoardState
import com.tmaster.game.Coord
import com.tmaster.game.Move
import com.tmaster.log.ModuleLogger
import kotlinx.coroutines.flow.first

/**
 * 鹰眼分析器 — 全谱分析与问题手/好手检测。
 *
 * 用法:
 * ```kotlin
 * val hawkEye = HawkEyeAnalyzer(engine)
 * val summary = hawkEye.analyzeGame(boardState)
 * ```
 */
class HawkEyeAnalyzer(
    private val engine: GtpEngine,
    private val config: HawkEyeConfig = HawkEyeConfig(),
) {
    private val logger = ModuleLogger("HawkEye")

    /** 分析完整对局 — 逐手调用引擎，返回汇总。 */
    suspend fun analyzeGame(initialState: BoardState): GameAnalysis {
        logger.i { "starting full game analysis (${initialState.moveCount} moves)" }

        val analyses = mutableListOf<AnalysisResult>()
        val problems = mutableListOf<ProblemMove>()
        val goods = mutableListOf<GoodMove>()
        var totalLoss = 0.0
        var aiMatches = 0

        var state = BoardState.empty(initialState.boardSize, initialState.komi)

        for (i in 0..initialState.moveCount) {
            // 导航到第 i 手
            state = navigateTo(initialState, i)

            val result = engine.analyze(state).first() // 等待最终结果
            val parsed = convertResult(i, result)
            analyses.add(parsed)

            // 检测实战手 vs AI 推荐
            if (i < initialState.moveCount) {
                val actual = initialState.moveHistory[i]
                if (!actual.coord.isPass) {
                    // 问题手
                    detectProblem(actual, parsed)?.let { problems.add(it) }
                    // 好手
                    detectGood(actual, parsed)?.let { goods.add(it) }
                    // AI 重合
                    if (parsed.candidates.isNotEmpty() &&
                        parsed.candidates.first().coord == actual.coord
                    ) aiMatches++
                }
            }

            if (i % 20 == 0) logger.d { "analyzed move $i/${initialState.moveCount}" }
        }

        problems.forEach { totalLoss += it.winRateLoss }

        return GameAnalysis(
            totalMoves = initialState.moveCount,
            moveAnalyses = analyses,
            problemMoves = problems,
            goodMoves = goods,
            averageWinRateLoss = if (problems.isEmpty()) 0.0 else totalLoss / problems.size,
            aiMatchRate = if (initialState.moveCount == 0) 0.0
                else aiMatches.toDouble() / initialState.moveCount,
            blackAvgWinRate = 0.5, // TODO: 从分析结果计算
            whiteAvgWinRate = 0.5,
        )
    }

    /** 检测单步问题手。 */
    fun detectProblem(actual: Move, analysis: AnalysisResult): ProblemMove? {
        if (analysis.candidates.isEmpty()) return null
        val best = analysis.candidates.first()
        if (best.coord == actual.coord) return null // 下对了

        val actualCandidate = analysis.candidates.find { it.coord == actual.coord }
        val winRateLoss = if (actualCandidate != null) {
            best.winRate - actualCandidate.winRate
        } else {
            0.15 // 不在候选列表 = 大损失
        }
        val scoreLoss = if (actualCandidate != null) {
            best.scoreLead - actualCandidate.scoreLead
        } else {
            10.0
        }
        val severity = classifySeverity(winRateLoss)
        if (severity == ProblemSeverity.MINOR && !config.reportMinor) return null

        return ProblemMove(
            moveNumber = actual.moveNumber,
            actualCoord = actual.coord,
            bestCoord = best.coord,
            winRateLoss = winRateLoss,
            scoreLoss = scoreLoss,
            severity = severity,
            category = guessCategory(actual.coord, best.coord, winRateLoss),
        )
    }

    /** 检测好手 — 实战手匹配 AI 首选 + 胜率提升明显。 */
    fun detectGood(actual: Move, analysis: AnalysisResult): GoodMove? {
        if (analysis.candidates.isEmpty()) return null
        val best = analysis.candidates.first()
        if (best.coord != actual.coord) return null

        // AI 首选且该步显著优于第二名
        val second = analysis.candidates.getOrNull(1) ?: return null
        val margin = best.winRate - second.winRate
        if (margin < config.goodMoveThreshold) return null

        return GoodMove(
            moveNumber = actual.moveNumber,
            coord = actual.coord,
            description = when {
                margin > 0.10 -> "妙手！胜率大幅领先"
                margin > 0.05 -> "好手，精准判断"
                else -> "正解"
            },
        )
    }

    private fun classifySeverity(loss: Double): ProblemSeverity = when {
        loss < 0.02 -> ProblemSeverity.MINOR
        loss < 0.05 -> ProblemSeverity.MODERATE
        loss < 0.15 -> ProblemSeverity.MAJOR
        else -> ProblemSeverity.CRITICAL
    }

    private fun guessCategory(
        actual: Coord, best: Coord, loss: Double,
    ): ProblemCategory {
        val dx = Math.abs(actual.x - best.x)
        val dy = Math.abs(actual.y - best.y)
        return when {
            dx + dy <= 1 -> ProblemCategory.SHAPE
            dx + dy <= 3 -> ProblemCategory.DIRECTION
            loss > 0.15 -> ProblemCategory.LIFE_AND_DEATH
            else -> ProblemCategory.OTHER
        }
    }

    private fun convertResult(moveNum: Int, raw: KataAnalysisResult): AnalysisResult {
        val candidates = raw.moveInfos.mapIndexed { idx, info ->
            MoveCandidate(
                coord = Coord.fromSgf(info.move),
                winRate = info.winRate,
                scoreLead = info.scoreLead,
                visits = info.visits,
                order = idx + 1,
                principalVariation = info.pv.map { Coord.fromSgf(it) },
            )
        }
        return AnalysisResult(
            moveNumber = moveNum,
            candidates = candidates,
            rootWinRate = raw.rootWinRate,
            rootScoreLead = raw.rootScoreLead,
            totalVisits = raw.totalVisits,
            depth = 0,
        )
    }

    private fun navigateTo(initial: BoardState, target: Int): BoardState {
        var state = BoardState.empty(initial.boardSize, initial.komi)
        for (i in 0 until target.coerceAtMost(initial.moveCount)) {
            val m = initial.moveHistory[i]
            state = if (m.coord.isPass) state.pass()
            else state.play(m.coord) ?: state.pass()
        }
        return state
    }
}

data class HawkEyeConfig(
    val topCandidates: Int = 5,
    val reportMinor: Boolean = false,
    val goodMoveThreshold: Double = 0.03,
)
