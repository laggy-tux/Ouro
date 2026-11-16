package com.example.red_panda.ui.screens

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.red_panda.RedPandaApplication

@Composable
fun GalleryMediaThumbnail(
    uri: String,
    isVideo: Boolean,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    targetWidthPx: Int = DEFAULT_THUMBNAIL_SIZE_PX,
    targetHeightPx: Int = DEFAULT_THUMBNAIL_SIZE_PX
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as RedPandaApplication }
    val widthPx = targetWidthPx.coerceIn(1, MAX_DISPLAY_SIZE_PX)
    val heightPx = targetHeightPx.coerceIn(1, MAX_DISPLAY_SIZE_PX)

    val thumbnailRequest = remember(uri, isVideo, widthPx, heightPx) {
        app.thumbnailRepository.getOrCreateRequest(uri, isVideo, widthPx, heightPx)
    }

    val imageRequest = remember(thumbnailRequest) {
        ImageRequest.Builder(context)
            .data(thumbnailRequest)
            .size(Size(widthPx, heightPx))
            .crossfade(false)
            .build()
    }

    val startedAtMs = remember(uri) { SystemClock.elapsedRealtime() }

    Box(
        modifier = modifier.background(PLACEHOLDER_COLOR)
    ) {
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
                Log.e(THUMBNAIL_TAG, "Thumbnail request failed for $uri.", state.result.throwable)
            }
        )
    }
}

private const val DEFAULT_THUMBNAIL_SIZE_PX = 320
private const val MAX_DISPLAY_SIZE_PX = 1_024
private const val PERFORMANCE_TAG = "OuroPerformance"
private const val THUMBNAIL_TAG = "GalleryMediaThumbnail"
private val PLACEHOLDER_COLOR = Color(0xFF2C2C30)
