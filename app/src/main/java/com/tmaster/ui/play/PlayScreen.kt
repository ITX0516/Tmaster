package com.tmaster.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tmaster.engine.EngineManager
import com.tmaster.game.Coord
import com.tmaster.game.StoneColor
import com.tmaster.log.LogCollector
import com.tmaster.ui.board.BoardOverlay
import com.tmaster.ui.board.GoBoard
import com.tmaster.ui.theme.BoardTheme
import com.tmaster.ui.theme.GoodGreen

@Composable
fun PlayScreen(viewModel: PlayViewModel = viewModel()) {
    val boardState by viewModel.boardState.collectAsState()
    val engineState by viewModel.engineState.collectAsState()
    val aiThinking by viewModel.aiThinking.collectAsState()
    val message by viewModel.message.collectAsState()
    val engineError by viewModel.engineError.collectAsState()
    var showLog by remember { mutableStateOf(false) }
    val logs by LogCollector.lines.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 顶部状态栏 — 始终可见
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (status, color) = when (engineState) {
                    EngineManager.State.IDLE -> "等待启动" to MaterialTheme.colorScheme.onSurfaceVariant
                    EngineManager.State.INITIALIZING -> "初始化中..." to MaterialTheme.colorScheme.onSurfaceVariant
                    EngineManager.State.READY -> "KataGo 就绪" to GoodGreen
                    EngineManager.State.ERROR -> "引擎异常" to MaterialTheme.colorScheme.error
                    EngineManager.State.DOWNLOADING -> "下载模型中..." to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(status, style = MaterialTheme.typography.labelSmall, color = color)
                Text(if (aiThinking) "AI 思考中..." else "", style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = { showLog = !showLog }) {
                    Text(if (showLog) "隐藏日志" else "日志", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // 错误条 — 不挡界面
        if (engineError != null) {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = engineError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        maxLines = 3,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.retryEngine() }) { Text("重试") }
                        TextButton(onClick = { showLog = !showLog }) { Text("查看日志") }
                    }
                }
            }
        } else if (message != null) {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Text(
                    text = message ?: "",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // 日志面板 — 可选中复制
        if (showLog) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                SelectionContainer {
                    LazyColumn(modifier = Modifier.padding(4.dp)) {
                        items(logs.size) { i ->
                            Text(
                                text = logs[logs.size - 1 - i],
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(vertical = 1.dp),
                            )
                        }
                    }
                }
            }
        }

        // 棋盘
        GoBoard(
            state = boardState,
            boardTheme = BoardTheme.CLASSIC,
            overlay = BoardOverlay(),
            onTap = viewModel::onUserTap,
            modifier = Modifier.padding(4.dp).weight(1f),
        )

        // 底部控制
        BottomBar(
            boardState = boardState,
            aiThinking = aiThinking,
            engineReady = engineState == EngineManager.State.READY,
            onPass = viewModel::onPass,
            onUndo = viewModel::undo,
            onResign = viewModel::resign,
            onNewGame = viewModel::newGame,
        )
    }
}

@Composable
private fun BottomBar(
    boardState: com.tmaster.game.BoardState,
    aiThinking: Boolean,
    engineReady: Boolean,
    onPass: () -> Unit,
    onUndo: () -> Unit,
    onResign: () -> Unit,
    onNewGame: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${boardState.moveCount} 手", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onUndo, enabled = !aiThinking && boardState.moveCount > 0) { Text("撤销") }
            TextButton(onClick = onPass, enabled = !aiThinking) { Text("Pass") }
            TextButton(onClick = onResign, enabled = !aiThinking) { Text("认输") }
            TextButton(onClick = onNewGame) { Text("新局") }
        }
    }
}
