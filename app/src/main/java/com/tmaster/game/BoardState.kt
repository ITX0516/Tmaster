package com.tmaster.game

import com.tmaster.log.ModuleLogger

/**
 * 不可变的棋盘状态 —— 所有修改操作返回新 [BoardState]。
 *
 * grid 是 1D 数组，索引 = y * boardSize + x。null = 空交叉点。
 */
class BoardState private constructor(
    val boardSize: Int,
    val currentPlayer: StoneColor,
    private val grid: Array<StoneColor?>,
    val moveHistory: List<Move>,
    private val previousHashes: Set<Int>,
    val komi: Double = 6.5,
    val blackPlayer: String? = null,
    val whitePlayer: String? = null,
    val result: String? = null,
) {
    private val logger = ModuleLogger("Board")

    fun stoneAt(c: Coord): StoneColor? {
        if (c.isPass || c.x !in 0..<boardSize || c.y !in 0..<boardSize) return null
        return grid[c.y * boardSize + c.x]
    }

    val moveCount: Int get() = moveHistory.size
    val lastMove: Move? get() = moveHistory.lastOrNull()

    // ── 走子验证 ─────────────────────────────────────────────

    fun isValidMove(c: Coord): Boolean {
        if (c.isPass) return true
        if (c.x !in 0..<boardSize || c.y !in 0..<boardSize) return false
        if (stoneAt(c) != null) return false
        if (isSuicide(c)) return false
        if (violatesKo(c)) return false
        return true
    }

    private fun isSuicide(c: Coord): Boolean {
        val test = grid.clone()
        test[c.y * boardSize + c.x] = currentPlayer
        val enemy = currentPlayer.opposite()
        for (n in c.neighbours(boardSize)) {
            if (test[n.y * boardSize + n.x] == enemy && countLiberties(test, n) == 0)
                return false // captures enemy → not suicide
        }
        return countLiberties(test, c) == 0
    }

    private fun violatesKo(c: Coord): Boolean {
        if (moveHistory.isEmpty()) return false
        return previousHashes.contains(hypotheticalHash(c))
    }

    // ── 落子 ─────────────────────────────────────────────────

    fun play(c: Coord): BoardState? {
        if (!isValidMove(c)) return null
        if (c.isPass) return passInternal()
        return playInternal(c)
    }

    fun pass(): BoardState = passInternal()
    fun resign(): BoardState = copy(
        result = if (currentPlayer == StoneColor.BLACK) "W+R" else "B+R"
    )

    private fun playInternal(c: Coord): BoardState {
        val newGrid = grid.clone()
        newGrid[c.y * boardSize + c.x] = currentPlayer
        val captures = mutableSetOf<Coord>()
        val enemy = currentPlayer.opposite()
        for (n in c.neighbours(boardSize)) {
            if (newGrid[n.y * boardSize + n.x] == enemy && countLiberties(newGrid, n) == 0) {
                removeGroup(newGrid, n)
                collectPositions(grid, enemy, n, captures)
            }
        }
        val newMove = Move(currentPlayer, c, moveHistory.size, captures)
        return BoardState(
            boardSize, currentPlayer.opposite(), newGrid,
            moveHistory + newMove, previousHashes + hashGrid(grid),
            komi, blackPlayer, whitePlayer, result,
        )
    }

    private fun passInternal(): BoardState {
        val newMove = Move(currentPlayer, Coord.PASS, moveHistory.size)
        return BoardState(
            boardSize, currentPlayer.opposite(), grid.clone(),
            moveHistory + newMove, previousHashes + hashGrid(grid),
            komi, blackPlayer, whitePlayer, result,
        )
    }

    // ── 捕获 ─────────────────────────────────────────────────

    fun capturesAt(c: Coord): Set<Coord> {
        if (c.isPass) return emptySet()
        val test = grid.clone()
        test[c.y * boardSize + c.x] = currentPlayer
        val result = mutableSetOf<Coord>()
        val enemy = currentPlayer.opposite()
        for (n in c.neighbours(boardSize)) {
            if (test[n.y * boardSize + n.x] == enemy && countLiberties(test, n) == 0) {
                collectPositions(test, enemy, n, result)
            }
        }
        return result
    }

    // ── 气 ───────────────────────────────────────────────────

    private fun countLiberties(g: Array<StoneColor?>, start: Coord): Int {
        val color = g[start.y * boardSize + start.x] ?: return 0
        val visited = mutableSetOf<Int>()
        val stack = mutableListOf(start)
        var liberties = 0
        while (stack.isNotEmpty()) {
            val c = stack.removeAt(stack.lastIndex)
            val idx = c.y * boardSize + c.x
            if (!visited.add(idx)) continue
            for (n in c.neighbours(boardSize)) {
                val ni = n.y * boardSize + n.x
                when (g[ni]) {
                    null -> liberties++
                    color -> if (ni !in visited) stack.add(n)
                    else -> {} // other color, not traversed
                }
            }
        }
        return liberties
    }

    private fun removeGroup(g: Array<StoneColor?>, start: Coord) {
        val color = g[start.y * boardSize + start.x] ?: return
        val stack = mutableListOf(start)
        while (stack.isNotEmpty()) {
            val c = stack.removeAt(stack.lastIndex)
            val idx = c.y * boardSize + c.x
            if (g[idx] != color) continue
            g[idx] = null
            c.neighbours(boardSize).forEach { n ->
                if (g[n.y * boardSize + n.x] == color) stack.add(n)
            }
        }
    }

    private fun collectPositions(
        g: Array<StoneColor?>, color: StoneColor, start: Coord, out: MutableSet<Coord>,
    ) {
        val stack = mutableListOf(start)
        while (stack.isNotEmpty()) {
            val c = stack.removeAt(stack.lastIndex)
            if (!out.add(c)) continue
            c.neighbours(boardSize).forEach { n ->
                if (g[n.y * boardSize + n.x] == color && n !in out) stack.add(n)
            }
        }
    }

    // ── 数目 (中国规则) ──────────────────────────────────────

    fun scoreChinese(): Double {
        var black = 0.0
        var white = 0.0
        val visited = mutableSetOf<Int>()
        for (y in 0..<boardSize) {
            for (x in 0..<boardSize) {
                val idx = y * boardSize + x
                if (idx in visited) continue
                val stone = grid[idx]
                if (stone != null) {
                    if (stone == StoneColor.BLACK) black++ else white++
                    visited.add(idx)
                    continue
                }
                // 空点 → 洪水填充判断归属
                val region = mutableListOf<Int>()
                val borders = mutableSetOf<StoneColor>()
                val stack = mutableListOf(idx)
                while (stack.isNotEmpty()) {
                    val ci = stack.removeAt(stack.lastIndex)
                    if (!visited.add(ci)) continue
                    val s = grid[ci]
                    if (s != null) { borders.add(s); continue }
                    region.add(ci)
                    val cx = ci % boardSize
                    val cy = ci / boardSize
                    Coord(cx, cy).neighbours(boardSize).forEach { n ->
                        val ni = n.y * boardSize + n.x
                        if (ni !in visited) stack.add(ni)
                    }
                }
                if (borders.size == 1) {
                    if (borders.first() == StoneColor.BLACK) black += region.size
                    else white += region.size
                }
            }
        }
        return black - white - komi
    }

    // ── 哈希 ─────────────────────────────────────────────────

    private fun hashGrid(g: Array<StoneColor?>): Int {
        var h = 17
        g.forEach { h = h * 31 + when (it) {
            null -> 0; StoneColor.BLACK -> 1; StoneColor.WHITE -> 2
        }}
        return h
    }

    private fun hypotheticalHash(c: Coord): Int {
        val test = grid.clone()
        test[c.y * boardSize + c.x] = currentPlayer
        return hashGrid(test)
    }

    // ── 复制 ─────────────────────────────────────────────────

    private fun copy(
        result: String? = this.result,
    ): BoardState = BoardState(
        boardSize, currentPlayer, grid.clone(),
        moveHistory.toList(), previousHashes.toSet(),
        komi, blackPlayer, whitePlayer, result,
    )

    companion object {
        fun empty(boardSize: Int = 19, komi: Double = 6.5): BoardState = BoardState(
            boardSize = boardSize,
            currentPlayer = StoneColor.BLACK,
            grid = arrayOfNulls(boardSize * boardSize),
            moveHistory = emptyList(),
            previousHashes = emptySet(),
            komi = komi,
        )
    }
}
