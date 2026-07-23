package com.tmaster.ui.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun TeacherScreen() {
    val messages = remember { mutableStateListOf<TeacherMessage>() }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 初始问候
    LaunchedEffect(Unit) {
        messages.add(TeacherMessage(
            role = "assistant",
            content = "你好！我是你的围棋 AI 老师。\n\n" +
                "我可以帮你:\n" +
                "- 分析你的对局，找出问题手\n" +
                "- 讲解定式、死活、布局思路\n" +
                "- 根据你的弱点制定练习计划\n\n" +
                "上传一盘棋谱，或者直接问我问题吧！",
            cards = emptyList(),
        ))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 学生画像卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("学生: 棋手", style = MaterialTheme.typography.titleSmall)
                    Text("水平: 3d", style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("最近弱点:", style = MaterialTheme.typography.bodySmall)
                    Text("布局方向 · 劫争", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // 对话列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages.size) { idx ->
                val msg = messages[idx]
                TeacherBubble(msg)
            }
        }

        // 输入栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("追问...") },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        messages.add(TeacherMessage("user", inputText))
                        inputText = ""
                        // TODO: 调用 ConversationManager.chat()
                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                },
                modifier = Modifier.padding(start = 8.dp),
            ) { Text("发送") }
        }
    }
}

data class TeacherMessage(
    val role: String, // "user" | "assistant"
    val content: String,
    val cards: List<String> = emptyList(),
)

@Composable
fun TeacherBubble(msg: TeacherMessage) {
    val isUser = msg.role == "user"
    Card(
        modifier = Modifier.fillMaxWidth(if (isUser) 0.75f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(msg.content, style = MaterialTheme.typography.bodyMedium)
            if (msg.cards.isNotEmpty()) {
                Text("\n📎 相关知识卡:", style = MaterialTheme.typography.labelSmall)
                msg.cards.forEach { card ->
                    Text("  [$card]", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
