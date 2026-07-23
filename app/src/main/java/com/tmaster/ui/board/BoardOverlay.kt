package com.tmaster.ui.board

import com.tmaster.game.Coord

/**
 * 棋盘上的叠加标记 — 候选点、问题手、好手。
 */
data class BoardOverlay(
    /** 候选点标记列表。 */
    val candidates: List<CandidateMark> = emptyList(),
    /** 是否显示坐标。 */
    val showCoordinates: Boolean = true,
)

data class CandidateMark(
    val coord: Coord,
    val order: Int,          // 1 = AI首选, 2 = 第二选...
    val winRate: Double,
    val scoreLead: Double,
    val visits: Int,
    val isProblem: Boolean = false,  // 实战手是问题手 → 红色
    val isGood: Boolean = false,     // 实战手是好手 → 绿色
)
