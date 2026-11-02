package com.example.red_panda.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.red_panda.ui.theme.glassNavbar

enum class NavigationTab {
    PHOTOS,
    ALBUMS
}

@Composable
fun FloatingPillNavigationBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = modifier
            .wrapContentSize(Alignment.BottomCenter, unbounded = true)
            .navigationBarsPadding()
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Frosted Glass Floating Capsule Container matching iOS 26 glass theme
        Box(
            modifier = Modifier
                .glassNavbar(isDarkTheme = isDarkTheme)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSegmentedNavItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    isSelected = currentTab == NavigationTab.PHOTOS,
                    isDarkTheme = isDarkTheme,
                    onClick = { onTabSelected(NavigationTab.PHOTOS) }
                )

                Spacer(modifier = Modifier.width(6.dp))

                GlassSegmentedNavItem(
                    label = "Albums",
                    icon = Icons.Default.PhotoAlbum,
                    isSelected = currentTab == NavigationTab.ALBUMS,
                    isDarkTheme = isDarkTheme,
                    onClick = { onTabSelected(NavigationTab.ALBUMS) }
                )
            }
        }
    }
}

@Composable
private fun GlassSegmentedNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    // Both Home and Albums share the same dark glass active pill style with crisp white outline stroke
    val activeBackgroundColor = if (isDarkTheme) {
        Color(0xFF1E212E).copy(alpha = 0.85f)
    } else {
        Color(0xFF0F121C).copy(alpha = 0.90f)
    }

    val activeContentColor = Color.White

    val inactiveContentColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.55f)
    } else {
        Color(0xFF64748B)
    }

    val activeBorderColor = if (isSelected) {
        if (isDarkTheme) Color.White.copy(alpha = 0.85f) else Color(0xFF0F121C)
    } else {
        Color.Transparent
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) activeBackgroundColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) activeContentColor else inactiveContentColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabContent"
    )

    val borderColor by animateColorAsState(
        targetValue = activeBorderColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabBorder"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected && borderColor != Color.Transparent) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClickLabel = label,
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}
