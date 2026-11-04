package com.example.red_panda.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import com.example.red_panda.data.local.entity.PhotoRefEntity
import com.example.red_panda.ui.components.CategoryDialog
import com.example.red_panda.ui.components.ConfirmDeleteDialog
import com.example.red_panda.ui.components.EventDialog
import com.example.red_panda.ui.components.popupButtonColors
import com.example.red_panda.ui.model.EventMock
import com.example.red_panda.ui.viewmodel.DetailViewModel

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBackClick: () -> Unit,
    onEventClick: (String) -> Unit = {}
) {
    val categoryState by viewModel.category.collectAsState()
    val coverPhotoUri by viewModel.coverPhotoUri.collectAsState()
    val events by viewModel.events.collectAsState()
    val categoryPhotos by viewModel.categoryPhotos.collectAsState()

    val category = categoryState
    if (category == null) {
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

    var selectedPlaceFilter by remember { mutableStateOf("Places") }
    var selectedPeopleFilter by remember { mutableStateOf("People") }
    var showSlideshow by remember { mutableStateOf(false) }

    val placeOptions = remember(events) {
        listOf("Places") + events.map { it.placeTag }.distinct().filter { it.isNotBlank() && it != "All Places" }
    }
    val peopleOptions = remember(events) {
        listOf("People") + events.map { it.personTag }.distinct().filter { it.isNotBlank() && it != "All People" }
    }

    val filteredEvents = remember(events, selectedPlaceFilter, selectedPeopleFilter) {
        events.filter { event ->
            val matchesPlace = (selectedPlaceFilter == "Places") || (event.placeTag == selectedPlaceFilter)
            val matchesPeople = (selectedPeopleFilter == "People") || (event.personTag == selectedPeopleFilter)
            matchesPlace && matchesPeople
        }
    }

    val allPhotoUris = remember(events, categoryPhotos) {
        val eventUris = events.flatMap { it.photoUris }
        if (eventUris.isNotEmpty()) eventUris else categoryPhotos.map { it.contentUri }
    }

    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var showCoverEditDialog by remember { mutableStateOf(false) }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }

    var showCreateEventDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<EventMock?>(null) }
    var eventToDelete by remember { mutableStateOf<EventMock?>(null) }

    // Predictive back gestures for open dialogs or active filters
    BackHandler(enabled = showCreateEventDialog || eventToEdit != null || eventToDelete != null || showCoverEditDialog || showDeleteCategoryDialog || showEditCategoryDialog || showSlideshow) {
        if (showSlideshow) {
            showSlideshow = false
        } else {
            showCreateEventDialog = false
            eventToEdit = null
            eventToDelete = null
            showCoverEditDialog = false
            showDeleteCategoryDialog = false
            showEditCategoryDialog = false
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setCategoryCoverFromUri(uri.toString())
        }
    }

    if (showCoverEditDialog) {
        CoverEditDialog(
            categoryPhotos = categoryPhotos,
            onDismiss = { showCoverEditDialog = false },
            onUseFirstPhoto = { viewModel.resetCategoryCover() },
            onSelectPhoto = { photoId -> viewModel.updateCategoryCover(photoId) },
            onPickFromDevice = {
                showCoverEditDialog = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }

    if (showEditCategoryDialog) {
        CategoryDialog(
            initialName = category.name,
            initialSection = category.section,
            initialColor = category.coverColor,
            isEditMode = true,
            onDismiss = { showEditCategoryDialog = false },
            onConfirm = { name, section, coverColor ->
                viewModel.updateCategory(name, section, coverColor)
                showEditCategoryDialog = false
            }
        )
    }

    if (showDeleteCategoryDialog) {
        ConfirmDeleteDialog(
            title = "Delete Album",
            message = "Are you sure you want to delete '${category.name}' and all its events? This removes the album from Ouro. Your original photos in the device gallery will NOT be deleted.",
            onDismiss = { showDeleteCategoryDialog = false },
            onConfirm = {
                viewModel.deleteCategory(onDeleted = onBackClick)
                showDeleteCategoryDialog = false
            }
        )
    }

    if (showCreateEventDialog) {
        EventDialog(
            isEditMode = false,
            onDismiss = { showCreateEventDialog = false },
            onConfirm = { title, date, location, note, placeTag, personTag ->
                viewModel.createEvent(title, date, location, note, placeTag, personTag)
                showCreateEventDialog = false
            }
        )
    }

    eventToEdit?.let { event ->
        EventDialog(
            initialTitle = event.title,
            initialDate = event.date,
            initialLocation = event.location,
            initialNote = event.note,
            initialPlaceTag = event.placeTag,
            initialPersonTag = event.personTag,
            isEditMode = true,
            onDismiss = { eventToEdit = null },
            onConfirm = { title, date, location, note, placeTag, personTag ->
                viewModel.updateEvent(event.id, title, date, location, note, placeTag, personTag)
                eventToEdit = null
            }
        )
    }

    eventToDelete?.let { event ->
        ConfirmDeleteDialog(
            title = "Delete Event",
            message = "Are you sure you want to delete '${event.title}'? Attached media will remain on your device.",
            onDismiss = { eventToDelete = null },
            onConfirm = {
                viewModel.deleteEvent(event.id)
                eventToDelete = null
            }
        )
    }

    val totalPhotoCount = remember(events) { events.sumOf { it.totalPhotos } }

    if (showSlideshow) {
        AlbumSlideshowViewer(
            photoUris = allPhotoUris,
            onDismiss = { showSlideshow = false }
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Hero Cover Header
                item {
                    HeroHeader(
                        title = category.name,
                        photoCount = totalPhotoCount,
                        coverColor = category.coverColor,
                        coverPhotoUri = coverPhotoUri,
                        isFavorite = category.isFavorite,
                        onBackClick = onBackClick,
                        onStartSlideshow = { showSlideshow = true },
                        onToggleFavorite = { viewModel.toggleFavorite() },
                        onChangeCoverClick = { showCoverEditDialog = true },
                        onEditCategory = { showEditCategoryDialog = true },
                        onDeleteCategory = { showDeleteCategoryDialog = true },
                        onCreateEvent = { showCreateEventDialog = true }
                    )
                }

                // Floating Filter Card displayed below the cover image (compact size, truly floating)
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingFilterCard(
                            placeFilter = selectedPlaceFilter,
                            placeOptions = placeOptions,
                            onPlaceSelected = { selectedPlaceFilter = it },
                            peopleFilter = selectedPeopleFilter,
                            peopleOptions = peopleOptions,
                            onPeopleSelected = { selectedPeopleFilter = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Events List or Empty State
                if (filteredEvents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp, horizontal = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (selectedPlaceFilter != "Places" || selectedPeopleFilter != "People")
                                        "No Matching Events"
                                    else
                                        "No Events in this Album Yet",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (selectedPlaceFilter != "Places" || selectedPeopleFilter != "People")
                                        "Try clearing the active place or people filters."
                                    else
                                        "Create an event to group photos and videos by date, location, and memories.",
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (selectedPlaceFilter != "Places" || selectedPeopleFilter != "People") {
                                    OutlinedButton(
                                        onClick = {
                                            selectedPlaceFilter = "Places"
                                            selectedPeopleFilter = "People"
                                        },
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("Clear Filters")
                                    }
                                } else {
                                    Button(
                                        onClick = { showCreateEventDialog = true },
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Create Event")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredEvents, key = { it.id }) { event ->
                        EventSectionRow(
                            event = event,
                            onEventClick = { onEventClick(event.id) },
                            onEditEvent = { eventToEdit = event },
                            onDeleteEvent = { eventToDelete = event }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroHeader(
    title: String,
    photoCount: Int,
    coverColor: Long,
    coverPhotoUri: String? = null,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onStartSlideshow: () -> Unit,
    onToggleFavorite: () -> Unit,
    onChangeCoverClick: () -> Unit,
    onEditCategory: () -> Unit,
    onDeleteCategory: () -> Unit,
    onCreateEvent: () -> Unit
) {
    val context = LocalContext.current
    val coverIsVideo = remember(coverPhotoUri) {
        coverPhotoUri?.let {
            context.contentResolver.getType(Uri.parse(it))?.startsWith("video/") == true
        } ?: false
    }
    val headerHeight = 280.dp
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Color(coverColor))
    ) {
        if (!coverPhotoUri.isNullOrBlank()) {
            GalleryMediaThumbnail(
                uri = coverPhotoUri,
                isVideo = coverIsVideo,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dark gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        // Top Navigation Bar (Back + Menu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Unfavorite Album" else "Favorite Album") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) Color(0xFFFF4B4B) else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add Event") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onCreateEvent()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Change Cover Photo") },
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onChangeCoverClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Album") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEditCategory()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Album", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDeleteCategory()
                        }
                    )
                }
            }
        }

        // Title and Count
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$photoCount Items",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }

        // Bottom-Right Circular Slideshow Play Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onStartSlideshow() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Slideshow",
                tint = Color(0xFF0F121C),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun FloatingFilterCard(
    placeFilter: String,
    placeOptions: List<String>,
    onPlaceSelected: (String) -> Unit,
    peopleFilter: String,
    peopleOptions: List<String>,
    onPeopleSelected: (String) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var placeExpanded by remember { mutableStateOf(false) }
    var peopleExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isDark) Color(0xFF161822).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color.White.copy(alpha = 0.22f) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .widthIn(max = 340.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Places Button (Left Side)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { placeExpanded = true },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val isActive = placeFilter != "Places"
                    Text(
                        text = placeFilter,
                        fontSize = 14.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Place",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = placeExpanded,
                    onDismissRequest = { placeExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    placeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    fontWeight = if (option == placeFilter) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onPlaceSelected(option)
                                placeExpanded = false
                            }
                        )
                    }
                }
            }

            // Divider in center
            VerticalDivider(
                modifier = Modifier
                    .height(18.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            // People Button (Right Side)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { peopleExpanded = true },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val isActive = peopleFilter != "People"
                    Text(
                        text = peopleFilter,
                        fontSize = 14.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select People",
                        tint = if (isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = peopleExpanded,
                    onDismissRequest = { peopleExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    peopleOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    fontWeight = if (option == peopleFilter) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onPeopleSelected(option)
                                peopleExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventSectionRow(
    event: EventMock,
    onEventClick: () -> Unit,
    onEditEvent: () -> Unit,
    onDeleteEvent: () -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Event Header Row (No card box background!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onEventClick,
                    onLongClick = { showContextMenu = true }
                )
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitleText = buildString {
                    append(event.date)
                    if (event.location.isNotBlank()) {
                        append(", ")
                        append(event.location)
                    } else if (event.placeTag.isNotBlank() && event.placeTag != "All Places") {
                        append(", ")
                        append(event.placeTag)
                    }
                }
                Text(
                    text = subtitleText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open Event",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                DropdownMenuItem(
                    text = { Text("Edit Event") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        onEditEvent()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete Event", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showContextMenu = false
                        onDeleteEvent()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Event Photo Mosaic Grid (1 large tile on left, 2x2 small tiles on right)
        EventPhotoMosaic(
            photoUris = event.photoUris,
            thumbnailColors = event.thumbnailColors,
            totalPhotos = event.totalPhotos,
            onMosaicClick = onEventClick
        )
    }
}

@Composable
fun EventPhotoMosaic(
    photoUris: List<String>,
    thumbnailColors: List<Long>,
    totalPhotos: Int,
    onMosaicClick: () -> Unit
) {
    val displayUris = photoUris.take(5)
    val remainingCount = (totalPhotos - 5).coerceAtLeast(0)
    val mosaicHeight = 160.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(mosaicHeight)
            .clickable(onClick = onMosaicClick),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (displayUris.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                )
            }
        } else if (displayUris.size == 1) {
            MosaicTile(
                uri = displayUris[0],
                color = thumbnailColors.getOrElse(0) { 0xFF2C2C30 },
                cornerRadius = 16.dp,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Large tile on left + 2x2 grid on right
            MosaicTile(
                uri = displayUris[0],
                color = thumbnailColors.getOrElse(0) { 0xFF2C2C30 },
                cornerRadius = 16.dp,
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top Row (2 small tiles)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MosaicTile(
                        uri = displayUris.getOrNull(1),
                        color = thumbnailColors.getOrElse(1) { 0xFF2C2C30 },
                        cornerRadius = 12.dp,
                        modifier = Modifier.weight(1f)
                    )
                    MosaicTile(
                        uri = displayUris.getOrNull(2),
                        color = thumbnailColors.getOrElse(2) { 0xFF2C2C30 },
                        cornerRadius = 12.dp,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Bottom Row (2 small tiles)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MosaicTile(
                        uri = displayUris.getOrNull(3),
                        color = thumbnailColors.getOrElse(3) { 0xFF2C2C30 },
                        cornerRadius = 12.dp,
                        modifier = Modifier.weight(1f)
                    )
                    MosaicTile(
                        uri = displayUris.getOrNull(4),
                        color = thumbnailColors.getOrElse(4) { 0xFF2C2C30 },
                        cornerRadius = 12.dp,
                        remainingCount = remainingCount,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun MosaicTile(
    uri: String?,
    color: Long,
    cornerRadius: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    remainingCount: Int = 0
) {
    val context = LocalContext.current
    val isVideo = remember(uri) {
        uri?.let {
            context.contentResolver.getType(Uri.parse(it))?.startsWith("video/") == true
        } ?: false
    }
    val isPlaceholder = uri.isNullOrBlank()
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (isPlaceholder) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else Color(color)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!uri.isNullOrBlank()) {
            GalleryMediaThumbnail(
                uri = uri,
                isVideo = isVideo,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isVideo) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            )
        }

        if (remainingCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$remainingCount",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AlbumSlideshowViewer(
    photoUris: List<String>,
    onDismiss: () -> Unit
) {
    if (photoUris.isEmpty()) {
        onDismiss()
        return
    }

    var isPlaying by remember { mutableStateOf(true) }
    val pagerState = rememberPagerState(pageCount = { photoUris.size })
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    LaunchedEffect(isPlaying, photoUris, pagerState.currentPage) {
        val currentUri = photoUris[pagerState.currentPage]
        val isVideo = context.contentResolver.getType(Uri.parse(currentUri))?.startsWith("video/") == true
        if (isPlaying && !isVideo) {
            kotlinx.coroutines.delay(3000L)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % photoUris.size)
        }
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val mediaUri = photoUris[page]
            val isVideo = remember(mediaUri) {
                context.contentResolver.getType(Uri.parse(mediaUri))?.startsWith("video/") == true
            }
            val isActiveVideo = pagerState.currentPage == page
            if (isVideo) {
                AndroidView(
                    factory = { viewContext ->
                        VideoView(viewContext).apply {
                            tag = mediaUri
                            val controller = MediaController(viewContext)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setVideoURI(Uri.parse(mediaUri))
                            setOnPreparedListener { if (isActiveVideo) it.start() }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { videoView ->
                        if (videoView.tag != mediaUri) {
                            videoView.tag = mediaUri
                            videoView.setVideoURI(Uri.parse(mediaUri))
                            videoView.setOnPreparedListener { if (isActiveVideo) it.start() }
                        }
                        if (isActiveVideo && !videoView.isPlaying) {
                            videoView.start()
                        } else if (!isActiveVideo && videoView.isPlaying) {
                            videoView.pause()
                        }
                    },
                    onRelease = { it.stopPlayback() }
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(mediaUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Photo ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isPlaying = !isPlaying }
                )
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${pagerState.currentPage + 1} / ${photoUris.size}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Slideshow",
                    tint = Color.White
                )
            }
        }

        // Bottom control bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                coroutineScope.launch {
                    val previousPage = if (pagerState.currentPage > 0) {
                        pagerState.currentPage - 1
                    } else {
                        photoUris.lastIndex
                    }
                    pagerState.animateScrollToPage(previousPage)
                }
            }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White)
            }

            IconButton(onClick = { isPlaying = !isPlaying }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White
                )
            }

            IconButton(onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % photoUris.size)
                }
            }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
            }
        }
    }
}

@Composable
fun CoverEditDialog(
    categoryPhotos: List<PhotoRefEntity>,
    onDismiss: () -> Unit,
    onUseFirstPhoto: () -> Unit,
    onSelectPhoto: (Long) -> Unit,
    onPickFromDevice: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Change Cover Photo",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        onUseFirstPhoto()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use First Photo (Default)")
                }

                Button(
                    onClick = {
                        onPickFromDevice()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = popupButtonColors()
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick from Device Gallery")
                }

                if (categoryPhotos.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    Text(
                        text = "Or choose from album photos:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(categoryPhotos) { photo ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectPhoto(photo.id)
                                        onDismiss()
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(photo.contentUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    )
}
