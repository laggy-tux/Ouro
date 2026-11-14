package com.example.red_panda.ui.screens

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.red_panda.RedPandaApplication
import com.example.red_panda.data.thumbnail.MediaThumbnailRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GalleryMediaThumbnail(
    uri: String,
    isVideo: Boolean,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as RedPandaApplication }
    val density = LocalDensity.current
    val mediaStoreRevision by app.thumbnailRepository.mediaStoreRevision.collectAsState()

    BoxWithConstraints(modifier = modifier) {
        val widthPx = remember(maxWidth, density) {
            if (maxWidth.value.isFinite()) with(density) { maxWidth.roundToPx() }.coerceIn(1, MAX_DISPLAY_SIZE_PX)
            else DEFAULT_THUMBNAIL_SIZE_PX
        }
        val heightPx = remember(maxHeight, density) {
            if (maxHeight.value.isFinite()) with(density) { maxHeight.roundToPx() }.coerceIn(1, MAX_DISPLAY_SIZE_PX)
            else DEFAULT_THUMBNAIL_SIZE_PX
        }
        val startedAtMs = remember(uri, widthPx, heightPx) { SystemClock.elapsedRealtime() }
        val thumbnailRequest by produceState<MediaThumbnailRequest?>(
            initialValue = null,
            uri,
            isVideo,
            widthPx,
            heightPx,
            mediaStoreRevision
        ) {
            value = withContext(Dispatchers.IO) {
                app.thumbnailRepository.createRequest(uri, isVideo, widthPx, heightPx)
            }
        }

        if (thumbnailRequest == null) {
            Box(modifier = Modifier.fillMaxSize().background(PLACEHOLDER_COLOR))
        } else {
            val imageRequest = remember(thumbnailRequest) {
                ImageRequest.Builder(context)
                    .data(thumbnailRequest)
                    .size(Size(widthPx, heightPx))
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { state ->
                    if (isVideo) {
                        val elapsedMs = SystemClock.elapsedRealtime() - startedAtMs
                        Log.i(
                            PERFORMANCE_TAG,
                            "video_thumbnail elapsedMs=$elapsedMs source=${state.result.dataSource}"
                        )
                    }
                },
                onError = { state ->
                    Log.e(THUMBNAIL_TAG, "Thumbnail request failed.", state.result.throwable)
                }
            )
        }
    }
}

private const val DEFAULT_THUMBNAIL_SIZE_PX = 256
private const val MAX_DISPLAY_SIZE_PX = 1_024
private const val PERFORMANCE_TAG = "OuroPerformance"
private const val THUMBNAIL_TAG = "GalleryMediaThumbnail"
private val PLACEHOLDER_COLOR = Color(0xFF2C2C30)
