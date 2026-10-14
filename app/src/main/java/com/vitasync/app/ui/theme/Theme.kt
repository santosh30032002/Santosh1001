package com.vitasync.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF0B6E4F)
private val GreenLight = Color(0xFF3E9C7A)
private val Accent = Color(0xFFEF8354)

private val LightColors = lightColorScheme(
    primary = Green,
    secondary = GreenLight,
    tertiary = Accent
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    secondary = Green,
    tertiary = Accent
)

@Composable
fun VitaSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
