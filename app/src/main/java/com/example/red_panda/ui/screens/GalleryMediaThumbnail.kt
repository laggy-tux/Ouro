package com.example.red_panda.ui.screens

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
fun GalleryMediaThumbnail(
    uri: String,
    isVideo: Boolean,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    if (!isVideo) {
        AsyncImage(
            model = uri,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    val frame = produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(uri))
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (exception: IOException) {
                Log.w(TAG, "Unable to load video thumbnail for $uri", exception)
                null
            } catch (exception: RuntimeException) {
                Log.w(TAG, "Unable to load video thumbnail for $uri", exception)
                null
            } finally {
                try {
                    retriever.release()
                } catch (exception: IOException) {
                    Log.w(TAG, "Unable to release video thumbnail decoder for $uri", exception)
                }
            }
        }
    }.value

    if (frame == null) {
        Box(modifier = modifier.background(Color(0xFF2C2C30)))
    } else {
        Image(
            bitmap = frame.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier.fillMaxSize()
        )
    }
}

private const val TAG = "GalleryMediaThumbnail"
