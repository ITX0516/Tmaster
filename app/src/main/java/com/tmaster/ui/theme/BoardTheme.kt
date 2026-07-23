package com.tmaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 棋盘皮肤配置 — 木头色、线色、棋子样式。
 */
data class BoardTheme(
    val id: String,
    val name: String,
    val backgroundColor: Color,
    val lineColor: Color,
    val blackStoneColor: Color,
    val whiteStoneColor: Color,
    val coordinateColor: Color,
) {
    companion object {
        val CLASSIC = BoardTheme(
            id = "classic",
            name = "经典木纹",
            backgroundColor = WoodLight,
            lineColor = BoardLine,
            blackStoneColor = BlackStone,
            whiteStoneColor = WhiteStone,
            coordinateColor = Color(0xFF666666),
        )

        val DARK_WOOD = BoardTheme(
            id = "dark_wood",
            name = "深色木纹",
            backgroundColor = WoodDark,
            lineColor = Color(0xFFFFF8DC),
            blackStoneColor = Color(0xFF0D0D0D),
            whiteStoneColor = Color(0xFFFFFAF0),
            coordinateColor = Color(0xFFAAAAAA),
        )

        val ALL = listOf(CLASSIC, DARK_WOOD)
    }
}
