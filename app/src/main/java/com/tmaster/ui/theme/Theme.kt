package com.tmaster.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val WoodLight = Color(0xFFDEB887)
val WoodDark = Color(0xFF8B6914)
val BoardLine = Color(0xFF333333)
val BlackStone = Color(0xFF1A1A1A)
val WhiteStone = Color(0xFFF5F5F0)
val CandidateBlue = Color(0xFF2196F3)
val ProblemRed = Color(0xFFE53935)
val GoodGreen = Color(0xFF43A047)
val WinRateBar = Color(0xFF5C6BC0)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5C6BC0),
    secondary = Color(0xFF43A047),
    surface = Color(0xFFFAFAFA),
    background = Color(0xFFF0EDE5),
    error = ProblemRed,
)

@Composable
fun TmasterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
