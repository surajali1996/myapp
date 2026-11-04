package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ProTradingColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color(0xFF001B24),
    primaryContainer = ChartSurfaceElevated,
    onPrimaryContainer = CyanAccent,
    secondary = GoldAccent,
    onSecondary = Color(0xFF221B00),
    tertiary = CandleBullGreenBright,
    background = ChartBackground,
    onBackground = TextPrimary,
    surface = ChartSurface,
    onSurface = TextPrimary,
    surfaceVariant = ChartSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = ChartBorder,
    error = CandleBearRedBright,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Pro charts are best viewed in dark trading room mode
    MaterialTheme(
        colorScheme = ProTradingColorScheme,
        typography = Typography,
        content = content
    )
}
