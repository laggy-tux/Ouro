package com.example.red_panda.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.metrics.performance.PerformanceMetricsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.red_panda.RedPandaApplication
import com.example.red_panda.ui.components.ConfirmDeleteDialog
import com.example.red_panda.ui.components.MoveEventDialog
import com.example.red_panda.data.local.MediaStoreHelper
import com.example.red_panda.ui.viewmodel.EventDetailViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

private const val PREFETCH_SCROLL_ITEM_COUNT = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onBackClick: () -> Unit
) {
    val eventState by viewModel.event.collectAsState()
    val event = eventState
    val allAlbums by viewModel.allAlbums.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.coverFallbackEvents.collect {
            snackbarHostState.showSnackbar("Cover photo removed. A new cover was selected.")
        }
    }

    var showViewer by remember { mutableStateOf(false) }
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var photoToRemove by remember { mutableStateOf<Long?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri -> MediaStoreHelper.persistMediaReadPermission(context, uri) }
            val uriStrings = uris.map { it.toString() }
            viewModel.addPhotos(uriStrings)
        }
    }

    if (showDeleteDialog && event != null) {
        ConfirmDeleteDialog(
            title = "Delete Event",
            message = "Are you sure you want to delete '${event.title}'? This removes the event from Ouro. Your original photos in the device gallery will NOT be deleted.",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteEvent(onDeleted = onBackClick)
                showDeleteDialog = false
            }
        )
    }

    if (showMoveDialog && event != null) {
        val currentCategory = allAlbums.find { album -> album.events.any { it.id == event.id } }
        MoveEventDialog(
            eventTitle = event.title,
            currentCategoryId = currentCategory?.id?.toLongOrNull(),
            albums = allAlbums,
            onDismiss = { showMoveDialog = false },
            onConfirmMove = { targetCategoryId ->
                showMoveDialog = false
                viewModel.moveEvent(targetCategoryId) {
                    onBackClick()
                }
            }
        )
    }

    photoToRemove?.let { photoId ->
        ConfirmDeleteDialog(
            title = "Remove Media from Event",
            message = "Remove this photo or video from '${event?.title.orEmpty()}'? The original file will remain in your device gallery.",
            onDismiss = { photoToRemove = null },
            onConfirm = {
                viewModel.removePhoto(photoId)
                photoToRemove = null
                showViewer = false
            }
        )
    }

    if (event == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val app = context.applicationContext as RedPandaApplication
    val gridState = rememberLazyGridState()
    val view = LocalView.current
    val metricsStateHolder = remember(view) {
        PerformanceMetricsState.getHolderForHierarchy(view)
    }

    LaunchedEffect(event.id, event.photoUris) {
        app.thumbnailRepository.prefetchAlbum(event.photoUris.firstOrNull(), event.photoUris)
    }

    LaunchedEffect(gridState, event.photoUris) {
        var previousFirstVisibleIndex = gridState.firstVisibleItemIndex
        snapshotFlow {
            Triple(
                gridState.isScrollInProgress,
                gridState.firstVisibleItemIndex,
                gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            )
        }.distinctUntilChanged().collectLatest { (isScrolling, firstVisible, lastVisible) ->
            if (isScrolling) {
                metricsStateHolder.state?.putState("event_grid", "scrolling")
                if (kotlin.math.abs(firstVisible - previousFirstVisibleIndex) >= 6) {
                    val upcoming = if (firstVisible >= previousFirstVisibleIndex) {
                        val start = (lastVisible ?: firstVisible) + 1
                        event.photoUris.drop(start).take(PREFETCH_SCROLL_ITEM_COUNT)
                    } else {
                        event.photoUris.subList(
                            (firstVisible - PREFETCH_SCROLL_ITEM_COUNT).coerceAtLeast(0),
                            firstVisible.coerceAtMost(event.photoUris.size)
                        )
                    }
                    app.thumbnailRepository.prefetch(upcoming)
                    previousFirstVisibleIndex = firstVisible
                }
            } else {
                metricsStateHolder.state?.removeState("event_grid")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = event.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (event.date.isNotBlank() || event.location.isNotBlank()) {
                                Text(
                                    text = listOf(event.date, event.location).filter { it.isNotBlank() }.joinToString(" • "),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add Photos or Videos") },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Move to Album") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showMoveDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Event", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Photos or Videos")
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            BackHandler(enabled = showViewer) {
                showViewer = false
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val photos = event.photoUris
                if (photos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Media Yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap below to import photos or videos from your device gallery.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Photos or Videos")
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        itemsIndexed(
                            items = event.photoRefs,
                            key = { _, photo -> photo.id },
                            contentType = { _, _ -> "event_media" }
                        ) { index, photo ->
                            val photoUri = photo.contentUri
                            val isVideo = remember(photoUri) {
                                MediaStoreHelper.isVideoUri(context, photoUri)
                            }
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF2C2C30))
                                    .clickable {
                                        selectedPhotoIndex = index
                                        showViewer = true
                                    }
                            ) {
                                GalleryMediaThumbnail(
                                    uri = photoUri,
                                    isVideo = isVideo,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    contentDescription = "${if (isVideo) "Video" else "Photo"} ${index + 1}"
                                )
                                if (isVideo) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play video",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showViewer && event.photoUris.isNotEmpty()) {
            FullScreenPhotoViewer(
                photoUris = event.photoUris,
                initialIndex = selectedPhotoIndex,
                favoriteUris = event.photoRefs.filter { it.isFavorite }.mapTo(mutableSetOf()) { it.contentUri },
                onDismiss = { showViewer = false },
                onToggleFavorite = { page ->
                    event.photoRefs.getOrNull(page)?.let { photo ->
                        viewModel.togglePhotoFavorite(photo.id)
                    }
                },
                onRemovePhoto = { page ->
                    if (page in event.photoRefs.indices) {
                        val photo = event.photoRefs[page]
                        photoToRemove = photo.id
                    }
                }
            )
        }
    }
}
