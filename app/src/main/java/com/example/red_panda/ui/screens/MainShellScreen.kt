package com.example.red_panda.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.red_panda.ui.components.FloatingPillNavigationBar
import com.example.red_panda.ui.components.NavigationTab
import com.example.red_panda.ui.viewmodel.HomeViewModel
import com.example.red_panda.ui.viewmodel.PhotosTimelineViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainShellScreen(
    homeViewModel: HomeViewModel,
    photosTimelineViewModel: PhotosTimelineViewModel,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onAlbumClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(NavigationTab.PHOTOS) }
    var isPhotoViewerOpen by rememberSaveable { mutableStateOf(false) }

    // Predictive back gesture: when on Albums tab, pressing back returns to Photos tab before exit
    BackHandler(enabled = currentTab != NavigationTab.PHOTOS) {
        currentTab = NavigationTab.PHOTOS
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentTab) {
            NavigationTab.PHOTOS -> {
                PhotosTimelineScreen(
                    viewModel = photosTimelineViewModel,
                    onSettingsClick = onSettingsClick,
                    onViewerVisibilityChanged = { isPhotoViewerOpen = it }
                )
            }
            NavigationTab.ALBUMS -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
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
