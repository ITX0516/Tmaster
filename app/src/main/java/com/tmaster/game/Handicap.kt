package com.tmaster.game

/**
 * 让子棋布局 — 标准让子位置（按中国规则，从最多 9 子排列）。
 */
object Handicap {

    private val PLACEMENTS_9 = listOf(
        // 9 子标准摆法 (从大到小)
        listOf(2, 6, 10, 14, 18),
    )

    /** 返回 n 个让子的坐标列表 (0～9)。n=0 表示分先。 */
    fun placements(boardSize: Int, n: Int): List<Coord> {
        if (n < 2 || n > 9) return emptyList()
        val last = boardSize - 1
        val star = if (boardSize == 19) 3 else (boardSize / 2 - 2).coerceAtLeast(2)
        val center = boardSize / 2

        val points = mutableListOf(
            Coord(star, star), Coord(last - star, last - star),
            Coord(last - star, star), Coord(star, last - star),
            Coord(center, star), Coord(center, last - star),
            Coord(star, center), Coord(last - star, center),
            Coord(center, center),
        )
        // 9子: 全部, 8子: 去掉天元, 7子: 再去掉一个角, etc.
        return when (n) {
            9 -> points
            8 -> points.dropLast(1)
            7 -> points.dropLast(2)
            6 -> points.take(6)
            5 -> points.take(5)
            4 -> points.take(4)
            3 -> points.take(3)
            2 -> points.take(2)
            else -> emptyList()
        }
    }

    /** 创建让子初始棋盘。 */
    fun setup(boardSize: Int, n: Int, komi: Double = 0.5): BoardState {
        if (n < 2) return BoardState.empty(boardSize, komi)
        val state = BoardState.empty(boardSize, komi)
        val grid = arrayOfNulls<StoneColor>(boardSize * boardSize)
        for (c in placements(boardSize, n)) {
            grid[c.y * boardSize + c.x] = StoneColor.BLACK
        }
        return state.copyWithGrid(grid, currentPlayer = StoneColor.WHITE)
    }
}
