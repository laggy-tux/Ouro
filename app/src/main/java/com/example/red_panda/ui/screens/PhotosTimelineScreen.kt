package com.example.red_panda.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.graphics.luminance
import com.example.red_panda.data.local.LocalPhotoItem
import com.example.red_panda.data.local.MediaStoreHelper
import com.example.red_panda.data.local.PhotoAccessState
import com.example.red_panda.data.paging.TimelineItem
import com.example.red_panda.ui.components.AddToEventDialog
import com.example.red_panda.ui.components.FloatingPillNavigationBarHeight
import com.example.red_panda.ui.components.SuggestedEventDialog
import com.example.red_panda.ui.model.SuggestedEvent
import com.example.red_panda.ui.theme.HeartRed
import com.example.red_panda.ui.theme.glassCard
import com.example.red_panda.ui.viewmodel.PhotoFilter
import com.example.red_panda.ui.viewmodel.PhotosTimelineViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosTimelineScreen(
    viewModel: PhotosTimelineViewModel,
    onSettingsClick: () -> Unit,
    onViewerVisibilityChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val timelineItems = timelineItemsPaging(viewModel)
    val loadedTimelineItems = timelineItems.itemSnapshotList.items
    val gridState = rememberLazyGridState()
    val taggedUris by viewModel.taggedPhotoUris.collectAsState()
    val favoriteUris by viewModel.favoritePhotoUris.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val suggestedEvents by viewModel.suggestedEvents.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSuggestedEvents()
    }

    var photoAccessState by remember {
        mutableStateOf(MediaStoreHelper.getPhotoAccessState(context))
    }

    var selectedPhotoIndex by rememberSaveable { mutableIntStateOf(-1) }
    var showAddToEventDialog by remember { mutableStateOf(false) }
    var defaultEventDate by remember { mutableStateOf<String?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var suggestedEventToCreate by remember { mutableStateOf<SuggestedEvent?>(null) }

    // Predictive back gestures
    BackHandler(enabled = selectedPhotoIndex >= 0) {
        selectedPhotoIndex = -1
    }
    BackHandler(enabled = selectedPhotoIndex < 0 && viewModel.isSelectionMode) {
        viewModel.exitSelectionMode()
    }
    BackHandler(enabled = selectedPhotoIndex < 0 && !viewModel.isSelectionMode && viewModel.activeFilter != PhotoFilter.ALL) {
        viewModel.setFilter(PhotoFilter.ALL)
    }
    BackHandler(enabled = suggestedEventToCreate != null) {
        suggestedEventToCreate = null
    }

    LaunchedEffect(showAddToEventDialog, viewModel.selectedPhotoUris) {
        if (showAddToEventDialog && viewModel.selectedPhotoUris.isNotEmpty()) {
            val selectedUris = viewModel.selectedPhotoUris.toList()
            defaultEventDate = withContext(Dispatchers.IO) {
                MediaStoreHelper.getDefaultEventDate(context, selectedUris)
            }
        } else {
            defaultEventDate = null
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                photoAccessState = MediaStoreHelper.getPhotoAccessState(context)
                timelineItems.refresh()
                viewModel.loadSuggestedEvents()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SideEffect {
        onViewerVisibilityChanged(selectedPhotoIndex >= 0)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        photoAccessState = MediaStoreHelper.getPhotoAccessState(context)
    }

    // System Photo Picker launcher
    var showAddToEventDialogForPicked by remember { mutableStateOf<List<String>?>(null) }
    val systemPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                MediaStoreHelper.persistMediaReadPermission(context, uri)
            }
            val uriStrings = uris.map { it.toString() }
            viewModel.selectAllPhotos(uriStrings)
        }
    }

    if (showAddToEventDialog || showAddToEventDialogForPicked != null) {
        val photosToAdd = if (showAddToEventDialogForPicked != null) {
            showAddToEventDialogForPicked!!.size
        } else {
            viewModel.selectedPhotoUris.size
        }

        AddToEventDialog(
            selectedPhotoCount = photosToAdd,
            albums = albums,
            defaultEventDate = defaultEventDate,
            onDismiss = {
                showAddToEventDialog = false
                showAddToEventDialogForPicked = null
            },
            onAddToExistingEvent = { eventId ->
                if (showAddToEventDialogForPicked != null) {
                    val uris = showAddToEventDialogForPicked!!
                    viewModel.selectAllPhotos(uris)
                    showAddToEventDialogForPicked = null
                }
                viewModel.addSelectedToEvent(eventId) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Added photos to event")
                    }
                }
                showAddToEventDialog = false
            },
            onCreateNewEvent = { categoryId, title, date ->
                if (showAddToEventDialogForPicked != null) {
                    val uris = showAddToEventDialogForPicked!!
                    viewModel.selectAllPhotos(uris)
                    showAddToEventDialogForPicked = null
                }
                viewModel.createEventAndAddSelected(categoryId, title, date) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Event '$title' created with photos!")
                    }
                }
                showAddToEventDialog = false
            }
        )
    }

    suggestedEventToCreate?.let { suggested ->
        SuggestedEventDialog(
            suggested = suggested,
            albums = albums,
            onDismiss = { suggestedEventToCreate = null },
            onConfirm = { title, categoryId, albumName, newAlbumTitle ->
                viewModel.createEventFromSuggestedCard(
                    suggested = suggested,
                    categoryId = categoryId,
                    title = title,
                    newCategoryTitle = newAlbumTitle
                ) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Event '$title' created in '$albumName'!")
                    }
                }
                suggestedEventToCreate = null
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
            TopAppBar(
                title = {
                    if (viewModel.isSelectionMode) {
                        Text(
                            text = "${viewModel.selectedPhotoUris.size} Selected",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Photos",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Active Filter Chip Indicator
                            if (viewModel.activeFilter != PhotoFilter.ALL) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.clickable { viewModel.setFilter(PhotoFilter.ALL) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (viewModel.activeFilter) {
                                                PhotoFilter.UNTAGGED_ONLY -> "Untagged"
                                                PhotoFilter.FAVORITES_ONLY -> "Favorites"
                                                else -> ""
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear Filter",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (viewModel.isSelectionMode) {
                        IconButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Exit Selection Mode")
                        }
                    }
                },
                actions = {
                    if (viewModel.isSelectionMode) {
                        TextButton(
                            onClick = {
                                val allUris = mutableListOf<String>()
                                for (i in 0 until timelineItems.itemCount) {
                                    val item = timelineItems[i]
                                    if (item is TimelineItem.Photo) {
                                        allUris.add(item.photo.contentUri)
                                    }
                                }
                                viewModel.selectAllPhotos(allUris)
                            }
                        ) {
                            Text("Select All", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options"
                                    )
                                    if (viewModel.activeFilter != PhotoFilter.ALL) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .align(Alignment.TopEnd)
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Select Photos") },
                                    leadingIcon = {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.toggleSelectionMode()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Favorites Only") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Favorite, contentDescription = null, tint = HeartRed)
                                    },
                                    trailingIcon = {
                                        if (viewModel.activeFilter == PhotoFilter.FAVORITES_ONLY) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.toggleFilterFavoritesOnly()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Untagged Only") },
                                    leadingIcon = {
                                        Icon(Icons.Default.FilterList, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        if (viewModel.activeFilter == PhotoFilter.UNTAGGED_ONLY) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.toggleFilterUntaggedOnly()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import via Photo Picker") },
                                    leadingIcon = {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        systemPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.toggleColumnCount() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (viewModel.columnCount == 3) Icons.Default.GridOn else Icons.Default.GridView,
                            contentDescription = "Toggle Grid Columns"
                        )
                    }
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 112.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Permission Card when access is Partial or Denied
                if (photoAccessState == PhotoAccessState.PARTIAL) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Limited Photo Access",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Only selected photos and videos are visible.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            TextButton(
                                onClick = {
                                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                        arrayOf(
                                            Manifest.permission.READ_MEDIA_IMAGES,
                                            Manifest.permission.READ_MEDIA_VIDEO,
                                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                                        )
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        arrayOf(
                                            Manifest.permission.READ_MEDIA_IMAGES,
                                            Manifest.permission.READ_MEDIA_VIDEO
                                        )
                                    } else {
                                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                    permissionLauncher.launch(permissions)
                                }
                            ) {
                                Text("Select More")
                            }
                        }
                    }
                } else if (photoAccessState == PhotoAccessState.DENIED) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Photo Access Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ouro needs permission to read device photos and videos for your timeline.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val permissions = when {
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                                            Manifest.permission.READ_MEDIA_IMAGES,
                                            Manifest.permission.READ_MEDIA_VIDEO,
                                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                                        )
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                                            Manifest.permission.READ_MEDIA_IMAGES,
                                            Manifest.permission.READ_MEDIA_VIDEO
                                        )
                                        else -> arrayOf(
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                        )
                                    }
                                    permissionLauncher.launch(permissions)
                                }
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }

                // Check Paging State: Error State with Retry
                val refreshState = timelineItems.loadState.refresh
                if (refreshState is LoadState.Error) {
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
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Unable to Load Photos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = refreshState.error.localizedMessage ?: "An error occurred while accessing the gallery.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { timelineItems.retry() },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                } else if (refreshState is LoadState.Loading && timelineItems.itemCount == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                } else if (timelineItems.itemCount == 0 && refreshState is LoadState.NotLoading) {
                    // Empty State based on active filter
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
                            val emptyIcon = when (viewModel.activeFilter) {
                                PhotoFilter.FAVORITES_ONLY -> Icons.Default.FavoriteBorder
                                PhotoFilter.UNTAGGED_ONLY -> Icons.Default.BookmarkBorder
                                else -> Icons.Default.PhotoLibrary
                            }

                            val emptyTitle = when (viewModel.activeFilter) {
                                PhotoFilter.FAVORITES_ONLY -> "No Favorite Photos Yet"
                                PhotoFilter.UNTAGGED_ONLY -> "All Photos Are Tagged"
                                else -> "No Photos Found"
                            }

                            val emptySubtitle = when (viewModel.activeFilter) {
                                PhotoFilter.FAVORITES_ONLY -> "Tap the heart icon when viewing any photo to save it to your favorites."
                                PhotoFilter.UNTAGGED_ONLY -> "All photos in your library have been organized into events."
                                else -> "Photos taken on this device or saved in storage will appear here chronologically."
                            }

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = emptyIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = emptyTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = emptySubtitle,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (viewModel.activeFilter != PhotoFilter.ALL) {
                                OutlinedButton(
                                    onClick = { viewModel.setFilter(PhotoFilter.ALL) },
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("View All Photos")
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { timelineItems.refresh() },
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Refresh")
                                    }
                                    Button(
                                        onClick = {
                                            systemPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                            )
                                        },
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Import")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Timeline Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(viewModel.columnCount),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                FloatingPillNavigationBarHeight + 36.dp
                        )
                    ) {
                        if (
                            suggestedEvents.isNotEmpty() &&
                            !viewModel.isSelectionMode &&
                            photoAccessState != PhotoAccessState.DENIED
                        ) {
                            val firstSuggested = suggestedEvents.first()
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Suggested Event • ${firstSuggested.dateString}",
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            IconButton(
                                                onClick = {
                                                    viewModel.dismissSuggestedEvent(firstSuggested.id)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Dismiss suggested event",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Create event from these ${firstSuggested.photoCount} untagged photos?",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            firstSuggested.sampleUris.take(4).forEach { uri ->
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(uri)
                                                        .size(150, 150)
                                                        .build(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                suggestedEventToCreate = firstSuggested
                                            },
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Create Event")
                                        }
                                    }
                                }
                            }
                        }

                        items(
                            count = timelineItems.itemCount,
                            key = { index ->
                                when (val item = timelineItems[index]) {
                                    is TimelineItem.Header -> "header_${item.title}"
                                    is TimelineItem.Photo -> "photo_${item.photo.id}"
                                    null -> "placeholder_$index"
                                }
                            },
                            span = { index ->
                                when (timelineItems[index]) {
                                    is TimelineItem.Header -> GridItemSpan(maxLineSpan)
                                    else -> GridItemSpan(1)
                                }
                            }
                        ) { index ->
                            when (val item = timelineItems[index]) {
                                is TimelineItem.Header -> {
                                    val headerPhotoUris = remember(index, loadedTimelineItems) {
                                        val uris = mutableListOf<String>()
                                        for (i in (index + 1) until loadedTimelineItems.size) {
                                            when (val subItem = loadedTimelineItems[i]) {
                                                is TimelineItem.Header -> break
                                                is TimelineItem.Photo -> uris.add(subItem.photo.contentUri)
                                            }
                                        }
                                        uris
                                    }

                                    val isAllSelectedInGroup = remember(headerPhotoUris, viewModel.selectedPhotoUris) {
                                        headerPhotoUris.isNotEmpty() && headerPhotoUris.all { viewModel.selectedPhotoUris.contains(it) }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.title.uppercase(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                letterSpacing = 1.2.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (viewModel.isSelectionMode) {
                                            TextButton(
                                                onClick = {
                                                    viewModel.toggleDateGroupSelection(headerPhotoUris)
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isAllSelectedInGroup) "Deselect" else "Select All",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                                is TimelineItem.Photo -> {
                                    val photoUri = item.photo.contentUri
                                    val isSelected = viewModel.selectedPhotoUris.contains(photoUri)
                                    val isTagged = taggedUris.contains(photoUri)
                                    val isFav = favoriteUris.contains(photoUri)

                                    PhotoGridTile(
                                        photo = item.photo,
                                        isSelected = isSelected,
                                        isSelectionMode = viewModel.isSelectionMode,
                                        isTagged = isTagged,
                                        isFavorite = isFav,
                                        onClick = {
                                            if (viewModel.isSelectionMode) {
                                                viewModel.togglePhotoSelection(photoUri)
                                            } else {
                                                selectedPhotoIndex = index
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.togglePhotoSelection(photoUri)
                                        }
                                    )
                                }
                                null -> {
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .padding(1.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

            // Floating Bottom Action Bar when items are selected
            AnimatedVisibility(
                visible = viewModel.selectedPhotoUris.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 112.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .glassCard(isDarkTheme = isDarkTheme, shape = RoundedCornerShape(28.dp), isElevated = true)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${viewModel.selectedPhotoUris.size} selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showAddToEventDialog = true },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add to Event", fontSize = 13.sp)
                        }

                        IconButton(
                            onClick = { viewModel.batchFavoriteSelected(true) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorite selected",
                                tint = HeartRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Selection",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

        }
    }

    // Draw above the Scaffold so the full-screen viewer also covers the Photos app bar.
    if (selectedPhotoIndex >= 0) {
        val photoUris = remember(loadedTimelineItems) {
            loadedTimelineItems
                .filterIsInstance<TimelineItem.Photo>()
                .map { it.photo.contentUri }
        }
        val videoUris = remember(loadedTimelineItems) {
            loadedTimelineItems
                .filterIsInstance<TimelineItem.Photo>()
                .filter { it.photo.isVideo }
                .mapTo(mutableSetOf()) { it.photo.contentUri }
        }

        val photoIndex = remember(selectedPhotoIndex, loadedTimelineItems) {
            loadedTimelineItems
                .take(selectedPhotoIndex)
                .count { it is TimelineItem.Photo }
                .coerceIn(0, (photoUris.size - 1).coerceAtLeast(0))
        }

        if (photoUris.isNotEmpty()) {
            FullScreenPhotoViewer(
                photoUris = photoUris,
                initialIndex = photoIndex,
                favoriteUris = favoriteUris,
                videoUris = videoUris,
                onDismiss = { selectedPhotoIndex = -1 },
                onToggleFavorite = { page ->
                    val uri = photoUris.getOrNull(page)
                    if (uri != null) {
                        viewModel.togglePhotoFavorite(uri)
                    }
                }
            )
        }
    }
    }
}

@Composable
private fun timelineItemsPaging(viewModel: PhotosTimelineViewModel) =
    viewModel.timelineItems.collectAsLazyPagingItems()

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridTile(
    photo: LocalPhotoItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isTagged: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        GalleryMediaThumbnail(
            uri = photo.contentUri,
            isVideo = photo.isVideo,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "${if (photo.isVideo) "Video" else "Photo"} from ${photo.dateString}"
        )

        if (photo.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Dim overlay if selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }

        // Top-Right Checkmark Selection Indicator
        if (isSelectionMode || isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Not Selected",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Badges: Tagged & Favorite indicators in top-left
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTagged) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Already tagged in an event",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite photo",
                        tint = HeartRed,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
