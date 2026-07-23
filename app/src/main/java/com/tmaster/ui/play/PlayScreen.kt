package com.tmaster.ui.play

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tmaster.game.BoardState
import com.tmaster.game.Coord
import com.tmaster.game.GameController
import com.tmaster.ui.board.GoBoard
import com.tmaster.ui.board.BoardOverlay
import com.tmaster.ui.theme.BoardTheme
import com.tmaster.ui.theme.WinRateBar

@Composable
fun PlayScreen() {
    val gameController = remember { GameController(BoardState.empty(19, 6.5)) }
    val boardState by gameController.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 对手信息
        Text(
            text = "● 棋风: 均衡  难度: 3d  引擎: 本地 KataGo",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(8.dp),
        )

        // 棋盘
        GoBoard(
            state = boardState,
            boardTheme = BoardTheme.CLASSIC,
            overlay = BoardOverlay(),
            onTap = { coord -> gameController.play(coord) },
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        // 迷你胜率条
        LinearProgressIndicator(
            progress = { 0.65f }, // placeholder
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = WinRateBar,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text("黑 65%", style = MaterialTheme.typography.labelSmall)

        // 控制按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(onClick = { gameController.pass() }) { Text("Pass") }
            Button(onClick = { gameController.resign() }) { Text("认输") }
            OutlinedButton(onClick = { /* undo logic */ }) { Text("撤销") }
        }
    }
}
