package com.example.red_panda.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.glassCard(
    isDarkTheme: Boolean,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    isElevated: Boolean = false,
    elevation: Dp = if (isElevated) 12.dp else 4.dp
): Modifier {
    val backgroundColor = if (isDarkTheme) {
        if (isElevated) Color(0xFF1E2130).copy(alpha = 0.78f)
        else Color(0xFF161822).copy(alpha = 0.65f)
    } else {
        if (isElevated) Color(0xFFFFFFFF).copy(alpha = 0.90f)
        else Color(0xFFFFFFFF).copy(alpha = 0.75f)
    }

    val borderBrush = if (isDarkTheme) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFE2E8F0).copy(alpha = 0.80f),
                Color(0xFFCBD5E1).copy(alpha = 0.40f)
            )
        )
    }

    val shadowColor = if (isDarkTheme) Color.Black.copy(alpha = 0.45f) else Color(0xFF0F172A).copy(alpha = 0.08f)

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .clip(shape)
        .background(backgroundColor)
        .border(
            width = 1.dp,
            brush = borderBrush,
            shape = shape
        )
}

@Composable
fun Modifier.glassNavbar(
    isDarkTheme: Boolean,
    shape: RoundedCornerShape = RoundedCornerShape(32.dp)
): Modifier {
    val backgroundColor = if (isDarkTheme) {
        Color(0xFF161822).copy(alpha = 0.75f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.82f)
    }

    val borderBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.12f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 1.0f),
                Color(0xFFE2E8F0).copy(alpha = 0.80f)
            )
        )
    }

    val shadowColor = if (isDarkTheme) Color.Black.copy(alpha = 0.50f) else Color(0xFF0F172A).copy(alpha = 0.10f)

    return this
        .shadow(
            elevation = 16.dp,
            shape = shape,
            clip = false,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .clip(shape)
        .background(backgroundColor)
        .border(
            width = 1.dp,
            brush = borderBrush,
            shape = shape
        )
}
