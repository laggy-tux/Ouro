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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import com.example.red_panda.ui.components.FloatingPillNavigationBar
import com.example.red_panda.ui.components.NavigationTab
import com.example.red_panda.ui.viewmodel.HomeViewModel
import com.example.red_panda.ui.viewmodel.PhotosTimelineViewModel

@Composable
fun MainShellScreen(
    homeViewModel: HomeViewModel,
    photosTimelineViewModel: PhotosTimelineViewModel,
    onAlbumClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(NavigationTab.PHOTOS) }
    var isPhotoViewerOpen by rememberSaveable { mutableStateOf(false) }

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
                fadeIn(animationSpec = tween(130)) togetherWith
                    fadeOut(animationSpec = tween(130))
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
