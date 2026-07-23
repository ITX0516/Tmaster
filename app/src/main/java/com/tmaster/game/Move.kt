package com.tmaster.game

data class Move(
    val player: StoneColor,
    val coord: Coord,
    val moveNumber: Int, // 0-based
    val captures: Set<Coord> = emptySet(),
)
