package com.tmaster.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tmaster.engine.EngineManager
import com.tmaster.game.Coord
import com.tmaster.game.StoneColor
import com.tmaster.ui.board.BoardOverlay
import com.tmaster.ui.board.GoBoard
import com.tmaster.ui.theme.BoardTheme
import com.tmaster.ui.theme.WinRateBar

@Composable
fun PlayScreen(viewModel: PlayViewModel = viewModel()) {
    val boardState by viewModel.boardState.collectAsState()
    val aiThinking by viewModel.aiThinking.collectAsState()
    val message by viewModel.message.collectAsState()
    val engineState by viewModel.engineState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 状态栏
        StatusBar(
            boardState = boardState,
            aiThinking = aiThinking,
            engineState = engineState,
            message = message,
        )

        // 棋盘
        GoBoard(
            state = boardState,
            boardTheme = BoardTheme.CLASSIC,
            overlay = BoardOverlay(),
            onTap = { viewModel.onUserTap(it) },
            modifier = Modifier.padding(horizontal = 6.dp),
        )

        // 控制按钮
        ControlBar(
            aiThinking = aiThinking,
            currentPlayer = boardState.currentPlayer,
            aiColor = viewModel.aiColor,
            onPass = { viewModel.onPass() },
            onUndo = { viewModel.undo() },
            onResign = { viewModel.resign() },
            onNewGame = { viewModel.newGame() },
        )
    }
}

@Composable
private fun StatusBar(
    boardState: com.tmaster.game.BoardState,
    aiThinking: Boolean,
    engineState: EngineManager.State,
    message: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 引擎状态
            val statusText = when (engineState) {
                EngineManager.State.IDLE -> "引擎未启动"
                EngineManager.State.DOWNLOADING -> "下载模型中..."
                EngineManager.State.INITIALIZING -> "引擎初始化..."
                EngineManager.State.READY -> "KataGo 就绪"
                EngineManager.State.ERROR -> "引擎异常"
            }
            val statusColor = when (engineState) {
                EngineManager.State.READY -> com.tmaster.ui.theme.GoodGreen
                EngineManager.State.ERROR -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = statusText,
                color = statusColor,
                style = MaterialTheme.typography.labelSmall,
            )

            // 当前落子方
            if (engineState == EngineManager.State.READY) {
                val playerText = when {
                    aiThinking -> "AI 思考中..."
                    boardState.currentPlayer == StoneColor.BLACK -> "黑方"
                    else -> "白方"
                }
                Text(playerText, style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    // 消息提示
    if (message != null) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ControlBar(
    aiThinking: Boolean,
    currentPlayer: StoneColor,
    aiColor: StoneColor,
    onPass: () -> Unit,
    onUndo: () -> Unit,
    onResign: () -> Unit,
    onNewGame: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Button(
            onClick = onPass,
            enabled = !aiThinking && currentPlayer != aiColor,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) { Text("Pass") }

        OutlinedButton(
            onClick = onUndo,
            enabled = !aiThinking,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) { Text("撤销") }

        OutlinedButton(
            onClick = onResign,
            enabled = !aiThinking,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) { Text("认输") }

        OutlinedButton(
            onClick = onNewGame,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) { Text("新局") }
    }
}
