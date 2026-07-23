package com.tmaster.ui.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tmaster.game.BoardState
import com.tmaster.ui.board.GoBoard
import com.tmaster.ui.board.BoardOverlay
import com.tmaster.ui.board.CandidateMark
import com.tmaster.ui.theme.BoardTheme
import com.tmaster.ui.theme.ProblemRed
import com.tmaster.ui.theme.GoodGreen

@Composable
fun AnalysisScreen() {
    var currentMove by remember { mutableIntStateOf(0) }
    var isAnalyzing by remember { mutableStateOf(true) } // 默认自动分析
    val state = remember { BoardState.empty(19, 6.5) }

    val demoCandidates = remember {
        listOf(
            CandidateMark(com.tmaster.game.Coord(3, 3), 1, 0.52, 0.5, 1234),
            CandidateMark(com.tmaster.game.Coord(3, 9), 2, 0.48, -0.3, 1100),
            CandidateMark(com.tmaster.game.Coord(9, 3), 3, 0.45, -1.2, 900),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { currentMove = (currentMove - 1).coerceAtLeast(0) }) {
                Text("◀")
            }
            Text("第 $currentMove 手  B方", style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = { currentMove++ }) {
                Text("▶")
            }
            TextButton(onClick = { isAnalyzing = !isAnalyzing }) {
                Text(if (isAnalyzing) "暂停" else "继续")
            }
        }

        // 问题手指示
        Text(
            text = "⚠ 问题手  胜率 -12.3%",
            color = ProblemRed,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        // 棋盘 + 分析叠加
        GoBoard(
            state = state,
            boardTheme = BoardTheme.CLASSIC,
            candidates = demoCandidates,
            overlay = BoardOverlay(candidates = demoCandidates),
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        // 迷你胜率条
        LinearProgressIndicator(
            progress = { 0.65f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = com.tmaster.ui.theme.WinRateBar,
        )

        // 问题手列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            items(3) { i ->
                val losses = listOf("5%", "12%", "1.8%")
                val cats = listOf("方向错误", "过分", "缓着")
                val sevs = listOf("⬤⬤", "⬤⬤⬤", "⬤")
                Text(
                    text = "#${i * 30 + 7} $sevs ${cats[i]}  -${losses[i]}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(onClick = { /* open teacher */ }) { Text("🤖 问AI老师") }
            OutlinedButton(onClick = { /* export SGF */ }) { Text("导出SGF") }
        }
    }
}
