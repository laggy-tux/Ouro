package com.example.red_panda.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.graphics.luminance
import com.example.red_panda.data.local.MediaStoreHelper
import com.example.red_panda.data.local.PhotoAccessState
import com.example.red_panda.ui.components.CategoryDialog
import com.example.red_panda.ui.components.ConfirmDeleteDialog
import com.example.red_panda.ui.components.SuggestedEventDialog
import com.example.red_panda.ui.model.Album
import com.example.red_panda.ui.model.SuggestedEvent
import com.example.red_panda.ui.theme.HeartRed
import com.example.red_panda.ui.theme.glassCard
import com.example.red_panda.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onAlbumClick: (String) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val albums by viewModel.albums.collectAsState()
    val suggestedEvents by viewModel.suggestedEvents.collectAsState()
    val context = LocalContext.current

    val permissionsToRequest = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES
        )
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    var photoAccessState by remember {
        mutableStateOf(MediaStoreHelper.getPhotoAccessState(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                photoAccessState = MediaStoreHelper.getPhotoAccessState(context)
                if (photoAccessState != PhotoAccessState.DENIED) {
                    viewModel.loadSuggestedEvents(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        photoAccessState = MediaStoreHelper.getPhotoAccessState(context)
        if (photoAccessState != PhotoAccessState.DENIED) {
            viewModel.loadSuggestedEvents(context)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var isGridTwoColumn by remember { mutableStateOf(true) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var albumToEdit by remember { mutableStateOf<Album?>(null) }
    var albumToDelete by remember { mutableStateOf<Album?>(null) }
    var suggestedEventToCreate by remember { mutableStateOf<SuggestedEvent?>(null) }

    // Predictive back gestures for open dialogs
    BackHandler(enabled = showCreateDialog || albumToEdit != null || albumToDelete != null || suggestedEventToCreate != null) {
        showCreateDialog = false
        albumToEdit = null
        albumToDelete = null
        suggestedEventToCreate = null
    }

    if (showCreateDialog) {
        CategoryDialog(
            isEditMode = false,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, section, coverColor ->
                viewModel.createCategory(name, section, coverColor)
                showCreateDialog = false
            }
        )
    }

    albumToEdit?.let { album ->
        CategoryDialog(
            initialName = album.title,
            initialSection = album.section,
            initialColor = album.coverColor,
            isEditMode = true,
            onDismiss = { albumToEdit = null },
            onConfirm = { name, section, coverColor ->
                viewModel.updateCategory(album.id, name, section, coverColor)
                albumToEdit = null
            }
        )
    }

    albumToDelete?.let { album ->
        ConfirmDeleteDialog(
            title = "Delete Album",
            message = "Are you sure you want to delete '${album.title}' and all its events? This removes the album from Ouro. Your original photos in the device gallery will NOT be deleted.",
            onDismiss = { albumToDelete = null },
            onConfirm = {
                viewModel.deleteCategory(album.id)
                albumToDelete = null
            }
        )
    }

    suggestedEventToCreate?.let { suggested ->
        SuggestedEventDialog(
            suggested = suggested,
            albums = albums,
            onDismiss = { suggestedEventToCreate = null },
            onConfirm = { title, categoryId, albumName, newAlbumTitle ->
                viewModel.createEventFromSuggested(
                    context = context,
                    categoryId = categoryId,
                    title = title,
                    date = suggested.dateString,
                    photoUris = suggested.photoUris,
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 112.dp)
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Albums",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = { isGridTwoColumn = !isGridTwoColumn }) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Toggle Grid",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Album",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        val columns = if (isGridTwoColumn) 2 else 1

        if (albums.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 90.dp
                    )
                    .padding(horizontal = 32.dp),
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
                            imageVector = Icons.Default.CollectionsBookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Albums Yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Create categories like Vacations, Family, or Projects to organize your memories into dated events.",
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create First Album")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 90.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (photoAccessState) {
                    PhotoAccessState.PARTIAL -> {
                        item(span = { GridItemSpan(columns) }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Limited Photo Access",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ouro has access to selected photos only. Some photos or suggested events may not appear.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { permissionLauncher.launch(permissionsToRequest) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                        ) {
                                            Text("Select More")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.fromParts("package", context.packageName, null)
                                                }
                                                context.startActivity(intent)
                                            }
                                        ) {
                                            Text("App Settings")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    PhotoAccessState.DENIED -> {
                        item(span = { GridItemSpan(columns) }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Photo Access Required",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Grant permission to organize device photos and suggest events.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = { permissionLauncher.launch(permissionsToRequest) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Grant")
                                    }
                                }
                            }
                        }
                    }
                    PhotoAccessState.FULL -> { /* Full access granted */ }
                }

                if (photoAccessState != PhotoAccessState.DENIED && suggestedEvents.isNotEmpty()) {
                    item(span = { GridItemSpan(columns) }) {
                        SuggestedEventsSection(
                            suggestedEvents = suggestedEvents,
                            onCreateSuggestedEvent = { suggested ->
                                suggestedEventToCreate = suggested
                            }
                        )
                    }
                }

                val sections = albums.groupBy { it.section }

                sections.forEach { (sectionName, sectionAlbums) ->
                    item(span = { GridItemSpan(columns) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = sectionName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "See All",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(sectionAlbums, key = { it.id }) { album ->
                        AlbumCard(
                            album = album,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = { onAlbumClick(album.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(album.id) },
                            onEdit = { albumToEdit = album },
                            onDelete = { albumToDelete = album }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumCard(
    album: Album,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardHeight = 210.dp
    var showMenu by remember { mutableStateOf(false) }

    val baseModifier = Modifier
        .fillMaxWidth()
        .height(cardHeight)

    val transitionModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            baseModifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "album-cover-${album.id}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        baseModifier
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val hasCover = !album.coverPhotoUri.isNullOrBlank()

    Box(
        modifier = transitionModifier
            .glassCard(isDarkTheme = isDarkTheme, shape = RoundedCornerShape(22.dp))
            .background(
                if (hasCover) Color(album.coverColor).copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        if (hasCover) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.coverPhotoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = album.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (hasCover) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            ),
                            startY = 80f
                        )
                    )
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CollectionsBookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasCover) Color.Black.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (album.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (album.isFavorite) HeartRed
                    else if (hasCover) Color.White
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasCover) Color.Black.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = if (hasCover) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Album") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Album") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = album.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (hasCover) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${album.photoCount} Photos",
                fontSize = 12.sp,
                color = if (hasCover) Color.White.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SuggestedEventsSection(
    suggestedEvents: List<SuggestedEvent>,
    onCreateSuggestedEvent: (SuggestedEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Suggested Events",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Grouped from untagged photos in your gallery by creation date.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(suggestedEvents) { suggested ->
                    SuggestedEventCard(
                        suggested = suggested,
                        onCreateClick = { onCreateSuggestedEvent(suggested) }
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestedEventCard(
    suggested: SuggestedEvent,
    onCreateClick: () -> Unit
) {
    Card(
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = suggested.dateString,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${suggested.photoCount} photos",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                suggested.sampleUris.take(3).forEach { uri ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onCreateClick,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Event", fontSize = 12.sp)
            }
        }
    }
}
