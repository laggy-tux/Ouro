package com.example.red_panda.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.red_panda.RedPandaApplication
import com.example.red_panda.data.local.AppDatabase
import com.example.red_panda.data.repository.GalleryRepositoryImpl
import com.example.red_panda.data.repository.SettingsRepository
import com.example.red_panda.data.repository.SettingsRepositoryImpl
import com.example.red_panda.ui.screens.DetailScreen
import com.example.red_panda.ui.screens.EventDetailScreen
import com.example.red_panda.ui.screens.MainShellScreen
import com.example.red_panda.ui.screens.SettingsScreen
import com.example.red_panda.ui.viewmodel.DetailViewModel
import com.example.red_panda.ui.viewmodel.DetailViewModelFactory
import com.example.red_panda.ui.viewmodel.EventDetailViewModel
import com.example.red_panda.ui.viewmodel.EventDetailViewModelFactory
import com.example.red_panda.ui.viewmodel.HomeViewModel
import com.example.red_panda.ui.viewmodel.HomeViewModelFactory
import com.example.red_panda.ui.viewmodel.PhotosTimelineViewModel
import com.example.red_panda.ui.viewmodel.PhotosTimelineViewModelFactory
import com.example.red_panda.ui.viewmodel.SettingsViewModel
import com.example.red_panda.ui.viewmodel.SettingsViewModelFactory

sealed class Screen(val route: String) {
    object MainShell : Screen("main_shell")
    object Home : Screen("home")
    object Settings : Screen("settings")
    object Detail : Screen("detail/{albumId}") {
        fun createRoute(albumId: String) = "detail/$albumId"
    }
    object EventDetail : Screen("event/{eventId}") {
        fun createRoute(eventId: String) = "event/$eventId"
    }
}

private const val NAVIGATION_TRANSITION_MILLIS = 280
private val forwardEnterTransition =
    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(NAVIGATION_TRANSITION_MILLIS)) +
        fadeIn(animationSpec = tween(NAVIGATION_TRANSITION_MILLIS), initialAlpha = 0.98f)
private val forwardExitTransition =
    slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(NAVIGATION_TRANSITION_MILLIS)) +
        fadeOut(animationSpec = tween(NAVIGATION_TRANSITION_MILLIS), targetAlpha = 0.98f)
private val popEnterTransition =
    slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(NAVIGATION_TRANSITION_MILLIS)) +
        fadeIn(animationSpec = tween(NAVIGATION_TRANSITION_MILLIS), initialAlpha = 0.98f)
private val popExitTransition =
    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(NAVIGATION_TRANSITION_MILLIS)) +
        fadeOut(animationSpec = tween(NAVIGATION_TRANSITION_MILLIS), targetAlpha = 0.98f)

@Composable
fun NavGraph(
    settingsRepository: SettingsRepository? = null
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as? RedPandaApplication

    val repository = app?.repository ?: remember(context) {
        val database = AppDatabase.getInstance(context)
        GalleryRepositoryImpl(
            context = context.applicationContext,
            categoryDao = database.categoryDao(),
            eventDao = database.eventDao(),
            photoDao = database.photoDao()
        )
    }

    val actualSettingsRepository = settingsRepository ?: remember(context) {
        SettingsRepositoryImpl(context.applicationContext)
    }

    NavHost(
        navController = navController,
        startDestination = Screen.MainShell.route,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        composable(
            route = Screen.MainShell.route,
            enterTransition = { forwardEnterTransition },
            exitTransition = { forwardExitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(repository, actualSettingsRepository)
                )
                val photosTimelineViewModel: PhotosTimelineViewModel = viewModel(
                    factory = PhotosTimelineViewModelFactory(context, repository)
                )
                MainShellScreen(
                    homeViewModel = homeViewModel,
                    photosTimelineViewModel = photosTimelineViewModel,
                    onAlbumClick = { albumId ->
                        navController.navigate(Screen.Detail.createRoute(albumId))
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
        }
        composable(
            route = Screen.Home.route,
            enterTransition = { forwardEnterTransition },
            exitTransition = { forwardExitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(repository, actualSettingsRepository)
                )
                val photosTimelineViewModel: PhotosTimelineViewModel = viewModel(
                    factory = PhotosTimelineViewModelFactory(context, repository)
                )
                MainShellScreen(
                    homeViewModel = homeViewModel,
                    photosTimelineViewModel = photosTimelineViewModel,
                    onAlbumClick = { albumId ->
                        navController.navigate(Screen.Detail.createRoute(albumId))
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
        }
        composable(
            route = Screen.Settings.route,
            enterTransition = { forwardEnterTransition },
            exitTransition = { forwardExitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition }
        ) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(actualSettingsRepository, repository)
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
        }
        composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
                enterTransition = { forwardEnterTransition },
                exitTransition = { forwardExitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) { backStackEntry ->
                val albumIdString = backStackEntry.arguments?.getString("albumId") ?: "1"
                val categoryId = albumIdString.toLongOrNull() ?: 1L

                val detailViewModel: DetailViewModel = viewModel(
                    key = "detail_$categoryId",
                    factory = DetailViewModelFactory(repository, categoryId)
                )

                DetailScreen(
                    viewModel = detailViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onEventClick = { eventId ->
                        navController.navigate(Screen.EventDetail.createRoute(eventId))
                    }
                )
        }
        composable(
                route = Screen.EventDetail.route,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
                enterTransition = { forwardEnterTransition },
                exitTransition = { forwardExitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) { backStackEntry ->
                val eventIdString = backStackEntry.arguments?.getString("eventId") ?: "1"
                val eventId = eventIdString.toLongOrNull() ?: 1L

                val eventViewModel: EventDetailViewModel = viewModel(
                    key = "event_$eventId",
                    factory = EventDetailViewModelFactory(repository, eventId)
                )

                EventDetailScreen(
                    viewModel = eventViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
        }
    }
}
