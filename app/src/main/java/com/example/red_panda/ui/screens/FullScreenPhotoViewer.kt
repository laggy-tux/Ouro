package com.example.red_panda.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.red_panda.ui.theme.HeartRed

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

    val currentUri = photoUris.getOrNull(pagerState.currentPage) ?: ""
    val isCurrentFavorite = if (favoriteUris.isNotEmpty()) {
        favoriteUris.contains(currentUri)
    } else {
        isFavorite
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                AndroidView(
                    factory = { viewContext ->
                        VideoView(viewContext).apply {
                            tag = mediaUri
                            val controller = MediaController(viewContext)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setVideoURI(Uri.parse(mediaUri))
                            setOnPreparedListener { player ->
                                if (isActiveVideo) player.start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { videoView ->
                        if (videoView.tag != mediaUri) {
                            videoView.tag = mediaUri
                            videoView.setVideoURI(Uri.parse(mediaUri))
                            videoView.setOnPreparedListener { player ->
                                if (isActiveVideo) player.start()
                            }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close viewer",
                        tint = Color.White
                    )
                }

                Text(
                    text = "${pagerState.currentPage + 1} / ${photoUris.size}",
                    color = Color.White,
                    fontSize = 16.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onToggleFavorite(pagerState.currentPage) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isCurrentFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isCurrentFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isCurrentFavorite) HeartRed else Color.White
                        )
                    }

                    IconButton(
                        onClick = { onRemovePhoto(pagerState.currentPage) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove media",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
