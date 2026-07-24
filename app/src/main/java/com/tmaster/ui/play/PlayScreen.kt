package com.tmaster.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tmaster.engine.EngineManager
import com.tmaster.game.Coord
import com.tmaster.game.StoneColor
import com.tmaster.log.LogCollector
import com.tmaster.ui.board.BoardOverlay
import com.tmaster.ui.board.GoBoard
import com.tmaster.ui.theme.BoardTheme

@Composable
fun PlayScreen(viewModel: PlayViewModel = viewModel()) {
    val boardState by viewModel.boardState.collectAsState()
    val engineState by viewModel.engineState.collectAsState()
    val aiThinking by viewModel.aiThinking.collectAsState()
    val message by viewModel.message.collectAsState()
    val engineError by viewModel.engineError.collectAsState()

    GameLayout(
        boardState = boardState,
        engineState = engineState,
        engineError = engineError,
        aiThinking = aiThinking,
        message = message,
        onTap = viewModel::onUserTap,
        onPass = viewModel::onPass,
        onUndo = viewModel::undo,
        onResign = viewModel::resign,
        onNewGame = viewModel::newGame,
        onRetry = { viewModel.retryEngine() },
    )
}

@Composable
private fun GameLayout(
    boardState: com.tmaster.game.BoardState,
    engineState: EngineManager.State,
    engineError: String?,
    aiThinking: Boolean,
    message: String?,
    onTap: (Coord) -> Unit,
    onPass: () -> Unit,
    onUndo: () -> Unit,
    onResign: () -> Unit,
    onNewGame: () -> Unit,
    onRetry: () -> Unit,
) {
    var showLog by remember { mutableStateOf(true) }
    val logs by LogCollector.lines.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── 错误横幅 (不挡棋盘) ──
        if (engineError != null) {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = engineError.takeLast(200),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRetry) {
                        Text("重试", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // ── 状态栏 ──
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (status, color) = when (engineState) {
                    EngineManager.State.IDLE -> "等待启动" to MaterialTheme.colorScheme.onSurfaceVariant
                    EngineManager.State.INITIALIZING -> "初始化中..." to MaterialTheme.colorScheme.onSurfaceVariant
                    EngineManager.State.READY -> "KataGo 就绪" to com.tmaster.ui.theme.GoodGreen
                    EngineManager.State.ERROR -> "引擎异常" to MaterialTheme.colorScheme.error
                    EngineManager.State.DOWNLOADING -> "解压模型中..." to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(status, style = MaterialTheme.typography.labelSmall, color = color)

                val turn = when {
                    engineState != EngineManager.State.READY -> "等待引擎就绪"
                    aiThinking -> "AI 思考中..."
                    boardState.currentPlayer == StoneColor.BLACK -> "黑方落子"
                    else -> "白方落子"
                }
                Text(turn, style = MaterialTheme.typography.labelSmall)

                TextButton(onClick = { showLog = !showLog }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (showLog) "隐藏日志" else "日志", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // ── 日志面板 ──
        if (showLog) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                LazyColumn(modifier = Modifier.padding(4.dp)) {
                    items(logs.size) { i ->
                        Text(
                            text = logs[logs.size - 1 - i],
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(vertical = 0.5.dp),
                        )
                    }
                }
            }
        }

        // ── 棋盘 ──
        GoBoard(
            state = boardState,
            boardTheme = BoardTheme.CLASSIC,
            overlay = BoardOverlay(),
            onTap = onTap,
            modifier = Modifier.padding(4.dp).weight(1f),
        )

        // ── 底部控制 ──
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${boardState.moveCount} 手", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onUndo, enabled = !aiThinking) { Text("撤销") }
                TextButton(onClick = onPass, enabled = !aiThinking) { Text("Pass") }
                TextButton(onClick = onResign) { Text("认输") }
                TextButton(onClick = onNewGame) { Text("新局") }
            }
        }
    }
}
