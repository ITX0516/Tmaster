package com.tmaster.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    var placementMethod by remember { mutableIntStateOf(0) } // 0=单击, 1=双击, 2=确认
    var engineType by remember { mutableIntStateOf(0) }      // 0=本地, 1=远程
    var llmProvider by remember { mutableIntStateOf(0) }
    var boardThemeIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)

        // ── 落子方式 ──
        Text("落子方式", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("单击", "双击", "确认按钮").forEachIndexed { idx, label ->
                FilterChip(
                    selected = placementMethod == idx,
                    onClick = { placementMethod = idx },
                    label = { Text(label) },
                )
            }
        }

        Divider()

        // ── 引擎设置 ──
        Text("AI 引擎", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("本地 KataGo", "远程引擎").forEachIndexed { idx, label ->
                FilterChip(
                    selected = engineType == idx,
                    onClick = { engineType = idx },
                    label = { Text(label) },
                )
            }
        }

        if (engineType == 1) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("远程主机地址 (host:port)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("认证令牌") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Divider()

        // ── LLM Provider ──
        Text("AI 老师", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("OpenAI", "Anthropic", "DeepSeek", "通义千问").forEachIndexed { idx, label ->
                FilterChip(
                    selected = llmProvider == idx,
                    onClick = { llmProvider = idx },
                    label = { Text(label) },
                )
            }
        }

        Divider()

        // ── 棋盘皮肤 ──
        Text("棋盘皮肤", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("经典木纹", "深色木纹").forEachIndexed { idx, label ->
                FilterChip(
                    selected = boardThemeIndex == idx,
                    onClick = { boardThemeIndex = idx },
                    label = { Text(label) },
                )
            }
        }

        Divider()

        // ── 关于 ──
        Text("关于", style = MaterialTheme.typography.titleSmall)
        Text("Tmaster v0.1.0", style = MaterialTheme.typography.bodyMedium)
        Text("KataGo + LLM 围棋 AI 教练", style = MaterialTheme.typography.bodySmall)
    }
}
