package com.tmaster.game

/**
 * 将 [BoardState] 序列化为 SGF 字符串。
 */
object SgfWriter {

    fun write(state: BoardState): String {
        val sb = StringBuilder("(;GM[1]SZ[${state.boardSize}]KM[${state.komi}]")
        if (state.blackPlayer != null) sb.append("PB[${state.blackPlayer}]")
        if (state.whitePlayer != null) sb.append("PW[${state.whitePlayer}]")
        sb.append("\n")

        state.moveHistory.forEach { move ->
            val color = if (move.player == StoneColor.BLACK) "B" else "W"
            sb.append(";$color[${move.coord.toSgf(state.boardSize)}]\n")
        }
        sb.append(")")
        return sb.toString()
    }

    /** 精简导出 — 仅主线，无注释。 */
    fun writeCompact(state: BoardState): String {
        val sb = StringBuilder("(;GM[1]SZ[${state.boardSize}]KM[${state.komi}]")
        state.moveHistory.forEach { move ->
            val color = if (move.player == StoneColor.BLACK) "B" else "W"
            sb.append(";$color[${move.coord.toSgf(state.boardSize)}]")
        }
        sb.append(")")
        return sb.toString()
    }
}
