package com.tmaster.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tmaster.data.model.GameRecord

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
) {
    val games by viewModel.games.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<GameRecord?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索栏
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.setQuery(it)
            },
            placeholder = { Text("搜索棋手、结果...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            singleLine = true,
        )

        // 筛选标签
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = searchQuery.isBlank(), onClick = { searchQuery = ""; viewModel.setQuery("") }, label = { Text("全部") })
            FilterChip(selected = false, onClick = { searchQuery = "local"; viewModel.setQuery("local") }, label = { Text("本地") })
            FilterChip(selected = false, onClick = { searchQuery = "import"; viewModel.setQuery("import") }, label = { Text("导入") })
        }

        // 导入按钮 (占位，实际文件选择需要 ActivityResultLauncher)
        OutlinedButton(
            onClick = { /* 文件选择器需在 Activity 层实现 */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) { Text("导入 SGF 棋谱") }

        // 棋谱列表
        if (games.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无棋谱", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(games, key = { it.id }) { game ->
                    GameCard(
                        game = game,
                        onDelete = { showDeleteConfirm = game },
                    )
                }
            }
        }
    }

    // 删除确认
    showDeleteConfirm?.let { game ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除棋谱") },
            text = { Text("确定删除这条棋谱记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGame(game)
                    showDeleteConfirm = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            },
        )
    }
}

@Composable
fun GameCard(game: GameRecord, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${game.blackPlayer} vs ${game.whitePlayer}", style = MaterialTheme.typography.titleSmall)
                    Text(game.result ?: "?", style = MaterialTheme.typography.bodySmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(game.datePlayed ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${game.boardSize}x${game.boardSize}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(20.dp))
            }
        }
    }
}
