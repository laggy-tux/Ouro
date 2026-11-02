package com.example.red_panda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.red_panda.ui.model.Album
import com.example.red_panda.ui.model.SuggestedEvent

@Composable
fun SuggestedEventDialog(
    suggested: SuggestedEvent,
    albums: List<Album>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, categoryId: Long, albumName: String, newAlbumTitle: String?) -> Unit
) {
    var eventTitle by remember { mutableStateOf("Event on ${suggested.dateString}") }
    var selectedAlbumId by remember { mutableStateOf(albums.firstOrNull()?.id ?: "-1") }
    var createNewAlbum by remember { mutableStateOf(albums.isEmpty()) }
    var newAlbumTitle by remember { mutableStateOf("My Events") }

    val dialogMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.9f
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 480.dp)
                .heightIn(max = dialogMaxHeight)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = dialogMaxHeight)
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Create Suggested Event",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Preview Photos
                    Text(
                        text = "${suggested.photoCount} photos from ${suggested.dateString}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suggested.sampleUris.take(4).forEach { uri ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(uri)
                                    .size(150, 150)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Event Name") },
                        singleLine = true,
                        colors = popupTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Destination Album",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (albums.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            albums.forEach { album ->
                                val isSelected = !createNewAlbum && selectedAlbumId == album.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        createNewAlbum = false
                                        selectedAlbumId = album.id
                                    },
                                    label = { Text(album.title) },
                                    leadingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            FilterChip(
                                selected = createNewAlbum,
                                onClick = { createNewAlbum = true },
                                label = { Text("+ Create New Album") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (createNewAlbum) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newAlbumTitle,
                            onValueChange = { newAlbumTitle = it },
                            label = { Text("New Album Name") },
                            singleLine = true,
                            colors = popupTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = eventTitle.isNotBlank() && (!createNewAlbum || newAlbumTitle.isNotBlank()),
                        onClick = {
                            val catId = selectedAlbumId.toLongOrNull() ?: -1L
                            val chosenAlbumName = if (createNewAlbum) {
                                newAlbumTitle.trim()
                            } else {
                                albums.find { it.id == selectedAlbumId }?.title ?: "Album"
                            }
                            onConfirm(
                                eventTitle.trim(),
                                catId,
                                chosenAlbumName,
                                if (createNewAlbum) newAlbumTitle.trim() else null
                            )
                        },
                        colors = popupButtonColors()
                    ) {
                        Text("Create Event")
                    }
                }
            }
        }
    }
}
