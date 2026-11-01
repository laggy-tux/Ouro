package com.example.red_panda.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.graphics.luminance
import com.example.red_panda.data.local.entity.PhotoRefEntity
import com.example.red_panda.ui.components.CategoryDialog
import com.example.red_panda.ui.components.ConfirmDeleteDialog
import com.example.red_panda.ui.components.EventDialog
import com.example.red_panda.ui.components.popupButtonColors
import com.example.red_panda.ui.model.EventMock
import com.example.red_panda.ui.theme.glassCard
import com.example.red_panda.ui.viewmodel.DetailViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
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

    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var showCoverEditDialog by remember { mutableStateOf(false) }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }

    var showCreateEventDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<EventMock?>(null) }
    var eventToDelete by remember { mutableStateOf<EventMock?>(null) }

    // Predictive back gestures for open dialogs or active filters
    BackHandler(enabled = showCreateEventDialog || eventToEdit != null || eventToDelete != null || showCoverEditDialog || showDeleteCategoryDialog || showEditCategoryDialog) {
        showCreateEventDialog = false
        eventToEdit = null
        eventToDelete = null
        showCoverEditDialog = false
        showDeleteCategoryDialog = false
        showEditCategoryDialog = false
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
            message = "Are you sure you want to delete '${event.title}'? Attached photos will remain on your device.",
            onDismiss = { eventToDelete = null },
            onConfirm = {
                viewModel.deleteEvent(event.id)
                eventToDelete = null
            }
        )
    }

    val totalPhotoCount = remember(events) { events.sumOf { it.totalPhotos } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hero Header
            item {
                HeroHeader(
                    albumId = category.id.toString(),
                    title = category.name,
                    photoCount = totalPhotoCount,
                    coverColor = category.coverColor,
                    coverPhotoUri = coverPhotoUri,
                    isFavorite = category.isFavorite,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onBackClick = onBackClick,
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onChangeCoverClick = { showCoverEditDialog = true },
                    onEditCategory = { showEditCategoryDialog = true },
                    onDeleteCategory = { showDeleteCategoryDialog = true },
                    onCreateEvent = { showCreateEventDialog = true }
                )
            }

            // Filter Chips Section
            item {
                FilterChipsBar(
                    placeFilter = selectedPlaceFilter,
                    placeOptions = placeOptions,
                    onPlaceSelected = { selectedPlaceFilter = it },
                    peopleFilter = selectedPeopleFilter,
                    peopleOptions = peopleOptions,
                    onPeopleSelected = { selectedPeopleFilter = it }
                )
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
                                    "Create an event to group photos by date, location, and memories.",
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
                    EventCardRow(
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HeroHeader(
    albumId: String,
    title: String,
    photoCount: Int,
    coverColor: Long,
    coverPhotoUri: String? = null,
    isFavorite: Boolean,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onChangeCoverClick: () -> Unit,
    onEditCategory: () -> Unit,
    onDeleteCategory: () -> Unit,
    onCreateEvent: () -> Unit
) {
    val headerHeight = 280.dp
    var showMenu by remember { mutableStateOf(false) }

    val baseModifier = Modifier
        .fillMaxWidth()
        .height(headerHeight)

    val transitionModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            baseModifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "album-cover-$albumId"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        baseModifier
    }

    Box(
        modifier = transitionModifier
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Color(coverColor))
    ) {
        if (!coverPhotoUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverPhotoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
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
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Top Navigation Bar (Back + Favorite + Menu + Add Event)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { onToggleFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF4B4B) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Add Event Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { onCreateEvent() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Event",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Overflow Menu
                Box {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
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
        }

        // Title and Count (Always white over dark gradient)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val titleModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "album-title-$albumId"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            } else {
                Modifier
            }

            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = titleModifier
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$photoCount PHOTOS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 1.5.sp
            )
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
                // Option 1: Use First Photo
                OutlinedButton(
                    onClick = {
                        onUseFirstPhoto()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
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

                // Option 2: Pick from Device Gallery
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

@Composable
fun FilterChipsBar(
    placeFilter: String,
    placeOptions: List<String>,
    onPlaceSelected: (String) -> Unit,
    peopleFilter: String,
    peopleOptions: List<String>,
    onPeopleSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DropdownFilterChip(
            selectedOption = placeFilter,
            options = placeOptions,
            onOptionSelected = onPlaceSelected
        )
        DropdownFilterChip(
            selectedOption = peopleFilter,
            options = peopleOptions,
            onOptionSelected = onPeopleSelected
        )
    }
}

@Composable
fun DropdownFilterChip(
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(20.dp),
            color = if (selectedOption != "Places" && selectedOption != "People") {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selectedOption != "Places" && selectedOption != "People") {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = selectedOption,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun EventCardRow(
    event: EventMock,
    onEventClick: () -> Unit,
    onEditEvent: () -> Unit,
    onDeleteEvent: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .glassCard(isDarkTheme = isDarkTheme, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onEventClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Event Header Row (Title, Date, Overflow Menu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${event.date} • ${event.location}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Event Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Event") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEditEvent()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Event", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDeleteEvent()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Collage Thumbnails Row
            CollagePhotoGrid(
                photoUris = event.photoUris,
                thumbnailColors = event.thumbnailColors,
                totalPhotos = event.totalPhotos
            )
        }
    }
}

@Composable
fun CollagePhotoGrid(
    photoUris: List<String>,
    thumbnailColors: List<Long>,
    totalPhotos: Int
) {
    val displayUris = photoUris.take(5)
    val remainingCount = (totalPhotos - 5).coerceAtLeast(0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Large Thumbnail
        Box(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (displayUris.isEmpty()) MaterialTheme.colorScheme.surfaceVariant
                    else Color(thumbnailColors.getOrElse(0) { 0xFF2C2C30 })
                ),
            contentAlignment = Alignment.Center
        ) {
            if (displayUris.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(displayUris[0])
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "No photo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 4 Smaller Thumbnails Grid
        Column(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThumbnailBox(
                    uri = displayUris.getOrNull(1),
                    color = thumbnailColors.getOrElse(1) { 0xFF2C2C30 },
                    modifier = Modifier.weight(1f)
                )
                ThumbnailBox(
                    uri = displayUris.getOrNull(2),
                    color = thumbnailColors.getOrElse(2) { 0xFF2C2C30 },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThumbnailBox(
                    uri = displayUris.getOrNull(3),
                    color = thumbnailColors.getOrElse(3) { 0xFF2C2C30 },
                    modifier = Modifier.weight(1f)
                )
                ThumbnailBox(
                    uri = displayUris.getOrNull(4),
                    color = thumbnailColors.getOrElse(4) { 0xFF2C2C30 },
                    remainingCount = remainingCount,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ThumbnailBox(
    uri: String?,
    color: Long,
    modifier: Modifier = Modifier,
    remainingCount: Int = 0
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (uri.isNullOrBlank()) MaterialTheme.colorScheme.surfaceVariant
                else Color(color)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!uri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "No photo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        if (remainingCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$remainingCount",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
