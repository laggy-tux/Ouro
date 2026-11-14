package com.example.red_panda.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.metrics.performance.PerformanceMetricsState
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import com.example.red_panda.ui.components.FloatingPillNavigationBar
import com.example.red_panda.ui.components.NavigationTab
import com.example.red_panda.ui.viewmodel.HomeViewModel
import com.example.red_panda.ui.viewmodel.PhotosTimelineViewModel

private const val TAB_SWITCH_METRIC_WINDOW_MS = 150L

@Composable
fun MainShellScreen(
    homeViewModel: HomeViewModel,
    photosTimelineViewModel: PhotosTimelineViewModel,
    onAlbumClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(NavigationTab.PHOTOS) }
    var isPhotoViewerOpen by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current
    val metricsStateHolder = remember(view) {
        PerformanceMetricsState.getHolderForHierarchy(view)
    }
    var previousTab by remember { mutableStateOf(currentTab) }

    LaunchedEffect(currentTab, metricsStateHolder) {
        val state = metricsStateHolder.state
        state?.putState("main_tab", currentTab.name)
        if (previousTab != currentTab) {
            state?.putState("tab_switch", "${previousTab.name}->${currentTab.name}")
            previousTab = currentTab
            delay(TAB_SWITCH_METRIC_WINDOW_MS)
            state?.removeState("tab_switch")
        }
    }

    // Predictive back gesture: when on Albums tab, pressing back returns to Photos tab before exit
    BackHandler(enabled = currentTab != NavigationTab.PHOTOS) {
        currentTab = NavigationTab.PHOTOS
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(100)) togetherWith
                    fadeOut(animationSpec = tween(100))
            },
            label = "mainTab"
        ) { tab ->
            when (tab) {
                NavigationTab.PHOTOS -> PhotosTimelineScreen(
                    viewModel = photosTimelineViewModel,
                    onViewerVisibilityChanged = { isPhotoViewerOpen = it }
                )
                NavigationTab.ALBUMS -> HomeScreen(
                    viewModel = homeViewModel,
                    onAlbumClick = onAlbumClick,
                    onSettingsClick = onSettingsClick
                )
            }
        }

        if (!isPhotoViewerOpen) {
            FloatingPillNavigationBar(
                currentTab = currentTab,
                onTabSelected = { selectedTab -> currentTab = selectedTab },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
