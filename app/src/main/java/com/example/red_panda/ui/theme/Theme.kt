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
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = Color.White,
    secondary = AccentAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF261C10),
    onSecondaryContainer = AccentAmber,
    background = BackgroundDark,
    onBackground = PrimaryTextDark,
    surface = SurfaceDark,
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
    secondary = AccentAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF2E2),
    onSecondaryContainer = Color(0xFF7A3600),
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
