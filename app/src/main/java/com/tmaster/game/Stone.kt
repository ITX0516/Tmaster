package com.tmaster.game

enum class StoneColor {
    BLACK, WHITE;

    fun opposite(): StoneColor = if (this == BLACK) WHITE else BLACK
}

data class Coord(val x: Int, val y: Int) {
    val isPass: Boolean get() = x < 0

    fun toSgf(boardSize: Int): String {
        if (isPass) return "tt"
        return "${('a' + x)}${('a' + y)}"
    }

    fun neighbours(boardSize: Int): List<Coord> = buildList {
        if (y > 0) add(Coord(x, y - 1))
        if (x < boardSize - 1) add(Coord(x + 1, y))
        if (y < boardSize - 1) add(Coord(x, y + 1))
        if (x > 0) add(Coord(x - 1, y))
    }

    companion object {
        val PASS = Coord(-1, -1)

        fun fromSgf(label: String): Coord {
            if (label.isEmpty() || label == "tt") return PASS
            return Coord(label[0] - 'a', label[1] - 'a')
        }
    }
}
