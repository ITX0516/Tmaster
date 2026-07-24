package com.tmaster.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tmaster.log.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 日志查看界面 — 显示文件日志内容。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(onBack: () -> Unit) {
    var logText by remember { mutableStateOf("加载中...") }
    val scope = rememberCoroutineScope()

    fun loadLog() {
        scope.launch {
            logText = withContext(Dispatchers.IO) {
                try {
                    val file = FileLogger.getLogFile()
                    if (file == null || !file.exists()) {
                        "日志文件不存在"
                    } else {
                        val lines = file.readLines()
                        // 只显示最后 500 行
                        lines.takeLast(500).joinToString("\n")
                    }
                } catch (e: Exception) {
                    "读取日志失败: ${e.message}"
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadLog()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日志") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                },
                actions = {
                    TextButton(onClick = { loadLog() }) {
                        Text("刷新")
                    }
                    TextButton(onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                FileLogger.getLogFile()?.delete()
                            }
                            logText = "日志已清空"
                        }
                    }) {
                        Text("清空")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                text = logText,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}
