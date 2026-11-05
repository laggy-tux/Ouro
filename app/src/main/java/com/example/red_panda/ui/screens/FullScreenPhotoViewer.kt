package com.example.red_panda.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.red_panda.ui.components.LiquidGlassVideoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun FullScreenPhotoViewer(
    photoUris: List<String>,
    initialIndex: Int = 0,
    isFavorite: Boolean = false,
    favoriteUris: Set<String> = emptySet(),
    videoUris: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onToggleFavorite: (Int) -> Unit = {},
    onRemovePhoto: (Int) -> Unit = {}
) {
    if (photoUris.isEmpty()) {
        onDismiss()
        return
    }

    // Predictive Back Gesture handling
    BackHandler(onBack = onDismiss)

    val startIndex = initialIndex.coerceIn(0, photoUris.size - 1)
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { photoUris.size })
    var showControls by remember { mutableStateOf(true) }
    var showVideoActions by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val currentUri = photoUris.getOrNull(pagerState.currentPage) ?: ""
    var currentDateLabel by remember(currentUri) { mutableStateOf("Loading date…") }
    LaunchedEffect(currentUri) {
        currentDateLabel = withContext(Dispatchers.IO) {
            loadMediaDateLabel(context, currentUri)
        }
    }

    val isCurrentFavorite = if (favoriteUris.isNotEmpty()) {
        favoriteUris.contains(currentUri)
    } else {
        isFavorite
    }
    val isCurrentVideo = remember(currentUri, videoUris) {
        currentUri in videoUris ||
            runCatching {
                context.contentResolver.getType(Uri.parse(currentUri))?.startsWith("video/") == true
            }.getOrDefault(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF090B10), Color.Black, Color(0xFF10131C))
                )
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = false,
            userScrollEnabled = true
        ) { page ->
            val context = LocalContext.current
            val mediaUri = photoUris[page]
            val isVideo = remember(mediaUri, videoUris) {
                mediaUri in videoUris ||
                    runCatching {
                        context.contentResolver.getType(Uri.parse(mediaUri))?.startsWith("video/") == true
                    }.getOrDefault(false)
            }
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val isActiveVideo = pagerState.currentPage == page

            LaunchedEffect(pagerState.currentPage) {
                scale = 1f
                offset = Offset.Zero
            }

            if (isVideo) {
                LiquidGlassVideoPlayer(
                    videoUri = mediaUri,
                    isActive = isActiveVideo,
                    showParentControls = showControls,
                    onToggleParentControls = { showControls = !showControls },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 4f)
                    if (scale > 1f) {
                        offset += offsetChange
                    } else {
                        offset = Offset.Zero
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showControls = !showControls },
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                        .transformable(
                            state = transformableState,
                            canPan = { scale > 1f }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(mediaUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Photo ${page + 1} of ${photoUris.size}",
                        alignment = Alignment.Center,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
            }
        }

        // Controls Overlay (Top Bar)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .viewerGlass(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close viewer",
                        tint = Color.White
                    )
                }

                Text(
                    text = currentDateLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(204.dp)
                        .viewerGlass(RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )

                if (isCurrentVideo) {
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        IconButton(
                            onClick = { showVideoActions = true },
                            modifier = Modifier
                                .size(48.dp)
                                .viewerGlass(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Video actions",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showVideoActions,
                            onDismissRequest = { showVideoActions = false },
                            offset = DpOffset(x = 0.dp, y = 12.dp),
                            modifier = Modifier
                                .width(184.dp)
                                .viewerGlass(RoundedCornerShape(20.dp))
                                .padding(vertical = 6.dp),
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            VideoActionMenuItem(
                                label = "Share",
                                icon = Icons.Default.Share,
                                onClick = {
                                    showVideoActions = false
                                    shareMedia(context, currentUri)
                                }
                            )
                            VideoActionMenuItem(
                                label = "Delete",
                                icon = Icons.Default.Delete,
                                onClick = {
                                    showVideoActions = false
                                    onRemovePhoto(pagerState.currentPage)
                                }
                            )
                            VideoActionMenuItem(
                                label = if (isCurrentFavorite) "Remove Bookmark" else "Bookmark",
                                icon = if (isCurrentFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                onClick = {
                                    showVideoActions = false
                                    onToggleFavorite(pagerState.currentPage)
                                }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            // Video actions are available from the player's overflow menu.
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
        ) {
            if (!isCurrentVideo) {
                Row(
                    modifier = Modifier
                        .viewerGlass(RoundedCornerShape(30.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViewerAction(
                        label = "Share",
                        icon = Icons.Default.Share,
                        onClick = { shareMedia(context, currentUri) }
                    )
                    ViewerAction(
                        label = "Delete",
                        icon = Icons.Default.Delete,
                        onClick = { onRemovePhoto(pagerState.currentPage) }
                    )
                    ViewerAction(
                        label = "Bookmark",
                        icon = if (isCurrentFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        tint = if (isCurrentFavorite) Color(0xFFFFB45C) else Color.White,
                        onClick = { onToggleFavorite(pagerState.currentPage) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoActionMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ViewerAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun Modifier.viewerGlass(shape: Shape): Modifier =
    shadow(
        elevation = 18.dp,
        shape = shape,
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.55f),
        spotColor = Color.Black.copy(alpha = 0.55f)
    )
        .clip(shape)
        .background(Color(0xB310121D))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.38f),
                    Color.White.copy(alpha = 0.14f),
                    Color.White.copy(alpha = 0.06f)
                )
            ),
            shape = shape
        )

private fun loadMediaDateLabel(context: Context, uriString: String): String {
    val dateMillis = runCatching {
        val uri = Uri.parse(uriString)
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_ADDED),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                val takenColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                val addedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val takenAt = if (takenColumn >= 0) cursor.getLong(takenColumn) else 0L
                val addedAt = if (addedColumn >= 0) cursor.getLong(addedColumn) * 1000L else 0L
                takenAt.takeIf { it > 0L } ?: addedAt.takeIf { it > 0L }
            }
        }
    }.getOrNull()

    return dateMillis?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) }
        ?: "Date unavailable"
}

private fun shareMedia(context: Context, uriString: String) {
    val uri = Uri.parse(uriString)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = context.contentResolver.getType(uri) ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(shareIntent, "Share media"))
    }.onFailure { exception ->
        Toast.makeText(
            context,
            exception.localizedMessage ?: "Unable to share this media.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
