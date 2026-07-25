package com.tmaster.ui.play

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tmaster.game.StoneColor

/**
 * 新对局设置对话框 — 选择棋盘大小、让子、贴目、执黑/白。
 */
@Composable
fun NewGameDialog(
    onDismiss: () -> Unit,
    onConfirm: (config: GameConfig) -> Unit,
) {
    var boardSize by remember { mutableIntStateOf(19) }
    var handicap by remember { mutableIntStateOf(0) }
    var komi by remember { mutableDoubleStateOf(6.5) }
    var userColor by remember { mutableStateOf(StoneColor.BLACK) }
    var aiStrength by remember { mutableStateOf("normal") } // normal/strong/weak

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新对局") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 棋盘大小
                Text("棋盘大小", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(9, 13, 19).forEach { size ->
                        FilterChip(
                            selected = boardSize == size,
                            onClick = { boardSize = size },
                            label = { Text("${size}x$size") },
                        )
                    }
                }

                Divider()

                // 让子
                Text("让子", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 2, 3, 4, 5, 6, 7, 8, 9).forEach { h ->
                        FilterChip(
                            selected = handicap == h,
                            onClick = { handicap = h },
                            label = { Text(if (h == 0) "分先" else "$h 子") },
                        )
                    }
                }

                Divider()

                // 贴目
                Text("贴目", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.0, 3.25, 6.5, 7.5).forEach { k ->
                        FilterChip(
                            selected = komi == k,
                            onClick = { komi = k },
                            label = { Text("$k") },
                        )
                    }
                }

                Divider()

                // 执子
                Text("执子", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        StoneColor.BLACK to "执黑",
                        StoneColor.WHITE to "执白",
                    ).forEach { (color, label) ->
                        FilterChip(
                            selected = userColor == color,
                            onClick = { userColor = color },
                            label = { Text(label) },
                        )
                    }
                }

                Divider()

                // AI 强度
                Text("AI 强度", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "weak" to "初级",
                        "normal" to "中级",
                        "strong" to "高级",
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = aiStrength == key,
                            onClick = { aiStrength = key },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        GameConfig(
                            boardSize = boardSize,
                            handicap = handicap,
                            komi = komi,
                            userColor = userColor,
                            aiStrength = aiStrength,
                        )
                    )
                }
            ) {
                Text("开始对局")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

data class GameConfig(
    val boardSize: Int = 19,
    val handicap: Int = 0,
    val komi: Double = 6.5,
    val userColor: StoneColor = StoneColor.BLACK,
    val aiStrength: String = "normal",
)
