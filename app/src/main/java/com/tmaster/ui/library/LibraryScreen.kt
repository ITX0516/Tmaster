package com.tmaster.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen() {
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索栏
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索棋手、日期、来源...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        )

        // 筛选标签
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = false, onClick = {}, label = { Text("全部") })
            FilterChip(selected = false, onClick = {}, label = { Text("本地") })
            FilterChip(selected = false, onClick = {}, label = { Text("野狐") })
            FilterChip(selected = false, onClick = {}, label = { Text("OGS") })
        }

        // 导入按钮
        OutlinedButton(
            onClick = { /* SGF 文件选择器 */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) { Text("📂 导入 SGF 棋谱") }

        // 棋谱列表
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(5) { i ->
                GameCard(
                    black = "棋手A",
                    white = "棋手B",
                    result = "B+R",
                    date = "2024-0${i + 1}-15",
                    aiMatch = "${60 + i * 5}%",
                )
            }
        }
    }
}

@Composable
fun GameCard(black: String, white: String, result: String, date: String, aiMatch: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$black vs $white", style = MaterialTheme.typography.titleSmall)
                Text(result, style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(date, style = MaterialTheme.typography.bodySmall)
                Text("AI 重合: $aiMatch", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
