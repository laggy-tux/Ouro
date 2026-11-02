package com.example.red_panda.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.red_panda.data.repository.ThemeSetting

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = BackgroundDark,
    primaryContainer = Color(0xFF1E2130),
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = BackgroundDark,
    secondaryContainer = Color(0xFF26293A),
    onSecondaryContainer = Color.White,
    background = BackgroundDark,
    onBackground = PrimaryTextDark,
    surface = BackgroundDark,
    onSurface = PrimaryTextDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = SecondaryTextDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F121C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF0F121C),
    secondary = Color(0xFF0F121C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F121C),
    background = BackgroundLight,
    onBackground = PrimaryTextLight,
    surface = SurfaceLight,
    onSurface = PrimaryTextLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = SecondaryTextLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

@Composable
fun RedpandaTheme(
    themeSetting: ThemeSetting = ThemeSetting.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSetting) {
        ThemeSetting.SYSTEM -> isSystemInDarkTheme()
        ThemeSetting.DARK -> true
        ThemeSetting.LIGHT -> false
    }
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
