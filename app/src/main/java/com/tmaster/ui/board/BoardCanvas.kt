package com.tmaster.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.tmaster.game.BoardState
import com.tmaster.game.Coord
import com.tmaster.game.StoneColor
import com.tmaster.ui.theme.BoardTheme

/**
 * 围棋棋盘渲染组件 —— Canvas 绘制。
 *
 * 支持:
 * - 棋盘网格 + 星位 + 坐标
 * - 棋子 + 最后一手标记
 * - 候选点圆圈 (带序号) — 参照 BadukAI 风格
 * - 问题手/好手颜色标记 — 参照 KaTrain 风格
 * - 落子手势 (点击坐标映射)
 */
@Composable
fun GoBoard(
    state: BoardState,
    boardTheme: BoardTheme = BoardTheme.CLASSIC,
    candidates: List<BoardOverlay.CandidateMark> = emptyList(),
    overlay: BoardOverlay = BoardOverlay(),
    onTap: (Coord) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(state) {
                detectTapGestures { offset ->
                    val coord = pixelToCoord(
                        offset.x, offset.y,
                        size.width, state.boardSize,
                    )
                    onTap(coord)
                }
            },
    ) {
        val cellSize = size.width / state.boardSize
        val boardWidth = cellSize * (state.boardSize - 1)
        val offsetX = (size.width - boardWidth) / 2f
        val offsetY = (size.height - boardWidth) / 2f

        drawBackground(boardTheme, size)
        drawGrid(state.boardSize, cellSize, offsetX, offsetY, boardTheme)
        drawStarPoints(state.boardSize, cellSize, offsetX, offsetY, boardTheme)
        drawStones(state, cellSize, offsetX, offsetY, boardTheme)
        drawLastMoveMarker(state, cellSize, offsetX, offsetY)
        drawCandidates(candidates, cellSize, offsetX, offsetY)
        drawOverlay(overlay, cellSize, offsetX, offsetY, state.boardSize)
    }
}

private fun DrawScope.drawBackground(theme: BoardTheme, size: androidx.compose.ui.geometry.Size) {
    drawRect(color = theme.backgroundColor, size = size)
}

private fun DrawScope.drawGrid(
    boardSize: Int, cellSize: Float, ox: Float, oy: Float, theme: BoardTheme,
) {
    for (i in 0 until boardSize) {
        val x = ox + i * cellSize
        val y = oy + i * cellSize
        drawLine(theme.lineColor, Offset(x, oy), Offset(x, oy + (boardSize - 1) * cellSize), 1.5f)
        drawLine(theme.lineColor, Offset(ox, y), Offset(ox + (boardSize - 1) * cellSize, y), 1.5f)
    }
}

private fun DrawScope.drawStarPoints(
    boardSize: Int, cellSize: Float, ox: Float, oy: Float, theme: BoardTheme,
) {
    val starPositions = when (boardSize) {
        19 -> listOf(3 to 3, 3 to 9, 3 to 15, 9 to 3, 9 to 9, 9 to 15, 15 to 3, 15 to 9, 15 to 15)
        else -> emptyList()
    }
    for ((x, y) in starPositions) {
        val cx = ox + x * cellSize
        val cy = oy + y * cellSize
        drawCircle(theme.lineColor, 3.5f, Offset(cx, cy))
    }
}

private fun DrawScope.drawStones(
    state: BoardState, cellSize: Float, ox: Float, oy: Float, theme: BoardTheme,
) {
    val radius = cellSize * 0.44f
    for (y in 0 until state.boardSize) {
        for (x in 0 until state.boardSize) {
            val stone = state.stoneAt(Coord(x, y)) ?: continue
            val cx = ox + x * cellSize
            val cy = oy + y * cellSize
            val color = if (stone == StoneColor.BLACK) theme.blackStoneColor else theme.whiteStoneColor
            drawCircle(color, radius, Offset(cx, cy))
            // 白棋描边
            if (stone == StoneColor.WHITE) {
                drawCircle(theme.lineColor, radius, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            }
        }
    }
}

private fun DrawScope.drawLastMoveMarker(
    state: BoardState, cellSize: Float, ox: Float, oy: Float,
) {
    val last = state.lastMove ?: return
    if (last.coord.isPass) return
    val cx = ox + last.coord.x * cellSize
    val cy = oy + last.coord.y * cellSize
    val markerColor = if (last.player == StoneColor.BLACK)
        android.graphics.Color.WHITE else android.graphics.Color.BLACK
    drawCircle(
        androidx.compose.ui.graphics.Color(markerColor),
        cellSize * 0.12f,
        Offset(cx, cy),
    )
}

private fun DrawScope.drawCandidates(
    candidates: List<BoardOverlay.CandidateMark>, cellSize: Float, ox: Float, oy: Float,
) {
    val paint = android.graphics.Paint().apply {
        textSize = cellSize * 0.3f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }

    for (mark in candidates) {
        val cx = ox + mark.coord.x * cellSize
        val cy = oy + mark.coord.y * cellSize
        val radius = cellSize * 0.38f
        val color = when {
            mark.isProblem -> androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.6f)
            mark.isGood -> androidx.compose.ui.graphics.Color(0xFF43A047).copy(alpha = 0.6f)
            else -> androidx.compose.ui.graphics.Color(0xFF2196F3).copy(alpha = 0.5f)
        }
        drawCircle(color, radius, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))

        // 序号
        if (mark.order > 0) {
            paint.color = if (mark.isProblem) android.graphics.Color.RED
                else android.graphics.Color.BLUE
            drawContext.canvas.nativeCanvas.drawText(
                "${mark.order}",
                cx, cy + cellSize * 0.1f, paint,
            )
        }
    }
}

private fun DrawScope.drawOverlay(
    overlay: BoardOverlay, cellSize: Float, ox: Float, oy: Float, boardSize: Int,
) {
    // 坐标标签
    val coordPaint = android.graphics.Paint().apply {
        textSize = cellSize * 0.22f
        isAntiAlias = true
        color = 0xFF888888.toInt()
        textAlign = android.graphics.Paint.Align.CENTER
    }
    for (i in 0 until boardSize) {
        val x = ox + i * cellSize
        drawContext.canvas.nativeCanvas.drawText(
            ('A' + i).toString(), x, oy - cellSize * 0.5f, coordPaint,
        )
        val y = oy + i * cellSize + cellSize * 0.12f
        drawContext.canvas.nativeCanvas.drawText(
            "${boardSize - i}", ox - cellSize * 0.7f, y, coordPaint,
        )
    }
}

/** 屏幕像素坐标 → 棋盘坐标。 */
private fun pixelToCoord(px: Float, py: Float, boardPixelSize: Float, boardSize: Int): Coord {
    val cellSize = boardPixelSize / boardSize
    val boardWidth = cellSize * (boardSize - 1)
    val ox = (boardPixelSize - boardWidth) / 2f
    val oy = ox
    val x = ((px - ox + cellSize / 2) / cellSize).toInt()
    val y = ((py - oy + cellSize / 2) / cellSize).toInt()
    if (x in 0 until boardSize && y in 0 until boardSize) return Coord(x, y)
    return Coord.PASS
}
