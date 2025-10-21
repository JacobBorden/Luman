package com.lumen.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = MutedGold,
    secondary = ForestGreen,
    background = Ivory,
    surface = Ivory,
    onSurface = Ink,
    onPrimary = Ivory,
    onSecondary = Ivory
)

private val DarkColorScheme = darkColorScheme(
    primary = MutedGold,
    secondary = ForestGreen,
    background = Ink,
    surface = Ink,
    onSurface = Ivory,
    onPrimary = Ink,
    onSecondary = Ink
)

@Composable
fun LumenTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = LumenTypography,
        shapes = LumenShapes,
        content = content
    )
}
