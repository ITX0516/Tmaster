package com.tmaster.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tmaster.ui.analysis.AnalysisScreen
import com.tmaster.ui.library.LibraryScreen
import com.tmaster.ui.play.PlayScreen
import com.tmaster.ui.teacher.TeacherScreen

enum class Screen(val route: String, val label: String, val icon: ImageVector) {
    PLAY("play", "对弈", Icons.Default.SportsEsports),
    ANALYSIS("analysis", "分析", Icons.Default.Analytics),
    TEACHER("teacher", "AI老师", Icons.Default.Chat),
    LIBRARY("library", "棋谱库", Icons.Default.FolderOpen),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TmasterNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.PLAY.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.PLAY.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.PLAY.route) { PlayScreen() }
            composable(Screen.ANALYSIS.route) { AnalysisScreen() }
            composable(Screen.TEACHER.route) { TeacherScreen() }
            composable(Screen.LIBRARY.route) { LibraryScreen() }
        }
    }
}
