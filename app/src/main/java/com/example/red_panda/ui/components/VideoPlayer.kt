package com.example.red_panda.ui.components

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.red_panda.ui.screens.GalleryMediaThumbnail
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun LiquidGlassVideoPlayer(
    videoUri: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    showParentControls: Boolean = true,
    onToggleParentControls: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragPositionMs by remember { mutableFloatStateOf(0f) }

    var controlsVisible by remember { mutableStateOf(showParentControls) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val videoTitle = remember(videoUri) {
        getVideoTitle(context, videoUri)
    }

    val videoSubtitle = remember(durationMs) {
        if (durationMs > 0L) {
            "Ouro Player • ${formatTime(durationMs)}"
        } else {
            "Ouro Video Player"
        }
    }

    // Sync parent controls state if provided
    LaunchedEffect(showParentControls) {
        controlsVisible = showParentControls
    }

    // Update playback progress periodically
    LaunchedEffect(isPlaying, isActive, isDraggingSlider) {
        while (isActive && isPlaying && !isDraggingSlider) {
            videoViewInstance?.let { view ->
                runCatching {
                    val pos = view.currentPosition.toLong()
                    val dur = view.duration.toLong()
                    if (pos >= 0) currentPositionMs = pos
                    if (dur > 0) durationMs = dur
                }
            }
            delay(250)
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(controlsVisible, isPlaying, lastInteractionTime) {
        if (controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
            onToggleParentControls?.invoke()
        }
    }

    val toggleControls = {
        controlsVisible = !controlsVisible
        lastInteractionTime = System.currentTimeMillis()
        onToggleParentControls?.invoke()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { toggleControls() }
                )
            }
    ) {
        AndroidView(
            factory = { viewContext ->
                VideoView(viewContext).apply {
                    tag = videoUri
                    setVideoURI(Uri.parse(videoUri))
                    setOnPreparedListener { player ->
                        videoViewInstance = this
                        durationMs = player.duration.coerceAtLeast(0).toLong()
                        if (isActive) {
                            player.start()
                            isPlaying = true
                        }
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        currentPositionMs = durationMs
                        controlsVisible = true
                    }
                    setOnErrorListener { _, _, _ ->
                        isPlaying = false
                        true
                    }
                }
            },
            update = { view ->
                videoViewInstance = view
                if (view.tag != videoUri) {
                    view.tag = videoUri
                    view.setVideoURI(Uri.parse(videoUri))
                    view.setOnPreparedListener { player ->
                        durationMs = player.duration.coerceAtLeast(0).toLong()
                        if (isActive) {
                            player.start()
                            isPlaying = true
                        }
                    }
                }
                if (isActive && !view.isPlaying && isPlaying) {
                    view.start()
                } else if (!isActive && view.isPlaying) {
                    view.pause()
                    isPlaying = false
                }
            },
            onRelease = {
                it.stopPlayback()
                videoViewInstance = null
            },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Liquid Glass Control Card (Theme-Agnostic)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            val cardShape = RoundedCornerShape(26.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = cardShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.65f),
                        spotColor = Color.Black.copy(alpha = 0.65f)
                    )
                    .clip(cardShape)
                    .background(Color(0xB310121D)) // Translucent obsidian glass
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = cardShape
                    )
            ) {
                // Background Liquid Wave Decoration
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height

                    val path1 = Path().apply {
                        moveTo(0f, h * 0.4f)
                        cubicTo(w * 0.35f, h * 0.15f, w * 0.65f, h * 0.75f, w, h * 0.35f)
                    }
                    drawPath(
                        path = path1,
                        color = Color.White.copy(alpha = 0.07f),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    val path2 = Path().apply {
                        moveTo(0f, h * 0.65f)
                        cubicTo(w * 0.3f, h * 0.85f, w * 0.7f, h * 0.25f, w, h * 0.6f)
                    }
                    drawPath(
                        path = path2,
                        color = Color.White.copy(alpha = 0.04f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Header Row: Media Thumbnail + Title & Subtitle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        ) {
                            GalleryMediaThumbnail(
                                uri = videoUri,
                                isVideo = true,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = videoTitle,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = videoSubtitle,
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress Track & Timestamps Row
                    val currentPos = if (isDraggingSlider) sliderDragPositionMs.toLong() else currentPositionMs
                    val safeDuration = durationMs.coerceAtLeast(1L)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatTime(currentPos),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Slider(
                            value = currentPos.toFloat().coerceIn(0f, safeDuration.toFloat()),
                            valueRange = 0f..safeDuration.toFloat(),
                            onValueChange = {
                                isDraggingSlider = true
                                sliderDragPositionMs = it
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                videoViewInstance?.seekTo(sliderDragPositionMs.toInt())
                                currentPositionMs = sliderDragPositionMs.toLong()
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.30f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = formatRemainingTime(currentPos, durationMs),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Playback Control Buttons Row
                    Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                    ) {
                    IconButton(
                        onClick = {
                            val newPos = (currentPositionMs - 10000L).coerceAtLeast(0L)
                            videoViewInstance?.seekTo(newPos.toInt())
                            currentPositionMs = newPos
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .clickable {
                                videoViewInstance?.let { view ->
                                    if (view.isPlaying) {
                                        view.pause()
                                        isPlaying = false
                                    } else {
                                        view.start()
                                        isPlaying = true
                                    }
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    IconButton(
                        onClick = {
                            val newPos = (currentPositionMs + 10000L).coerceAtMost(durationMs)
                            videoViewInstance?.seekTo(newPos.toInt())
                            currentPositionMs = newPos
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

private fun formatRemainingTime(currentMs: Long, durationMs: Long): String {
    val remainingMs = (durationMs - currentMs).coerceAtLeast(0L)
    val formatted = formatTime(remainingMs)
    return "-$formatted"
}

private fun getVideoTitle(context: Context, videoUri: String): String {
    val uri = Uri.parse(videoUri)
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, MediaStore.MediaColumns.TITLE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) return name.substringBeforeLast(".")
                    }
                    val titleIndex = cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)
                    if (titleIndex != -1) {
                        val title = cursor.getString(titleIndex)
                        if (!title.isNullOrBlank()) return title
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to Uri path segment
        }
    }
    val rawName = uri.lastPathSegment ?: "Video"
    val cleanName = rawName.substringAfterLast("/").substringBeforeLast(".")
    return if (cleanName.isNotBlank()) cleanName else "Video"
}
