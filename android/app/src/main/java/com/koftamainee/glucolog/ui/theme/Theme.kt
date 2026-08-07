package com.koftamainee.glucolog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GlucologGreen,
    onPrimary = Color.White,
    primaryContainer = AccentBg,
    onPrimaryContainer = AccentText,
    secondary = LightTextSec,
    onSecondary = Color.White,
    secondaryContainer = LightBgTer,
    onSecondaryContainer = LightText,
    tertiary = ChartBasal,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightText,
    surface = LightBg,
    onSurface = LightText,
    surfaceVariant = LightBgSec,
    onSurfaceVariant = LightTextSec,
    outline = Color(0x33000000),
    outlineVariant = Color(0x38000000),
    error = Color(0xFFB3261E),
)

private val DarkColorScheme = darkColorScheme(
    primary = GlucologGreen,
    onPrimary = Color(0xFF003D2B),
    primaryContainer = GlucologGreenDark,
    onPrimaryContainer = AccentBg,
    secondary = DarkTextSec,
    onSecondary = Color(0xFF1A1A18),
    secondaryContainer = DarkBgTer,
    onSecondaryContainer = DarkText,
    tertiary = ChartBasal,
    onTertiary = Color.White,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkBg,
    onSurface = DarkText,
    surfaceVariant = DarkBgSec,
    onSurfaceVariant = DarkTextSec,
    outline = Color(0x1AFFFFFF),
    outlineVariant = Color(0x33FFFFFF),
    error = Color(0xFFF2B8B5),
)

@Composable
fun GlucologTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
