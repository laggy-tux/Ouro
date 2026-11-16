package com.example.red_panda.data.thumbnail

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class ThumbnailRepository(context: Context) {
    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver
    private val thumbnailDirectory = File(appContext.cacheDir, THUMBNAIL_DIRECTORY)
    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val decodeSemaphore = Semaphore(MAX_CONCURRENT_DECODES)
    private val cacheMutationMutex = Mutex()
    private val _mediaStoreRevision = MutableStateFlow(0L)
    val mediaStoreRevision: StateFlow<Long> = _mediaStoreRevision.asStateFlow()
    private val inFlight = ConcurrentHashMap<String, Deferred<File>>()
    private val metadataCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private var activePrefetchJob: Job? = null

    fun onMediaStoreChanged() {
        metadataCache.clear()
        _mediaStoreRevision.value += 1L
    }

    fun getOrCreateRequest(
        uriString: String,
        isVideo: Boolean,
        widthPx: Int = GRID_THUMBNAIL_SIZE_PX,
        heightPx: Int = GRID_THUMBNAIL_SIZE_PX
    ): MediaThumbnailRequest {
        val cached = metadataCache[uriString]
        val mediaId = cached?.first ?: MediaThumbnailRequest.fallbackMediaId(uriString)
        val dateModified = cached?.second ?: 0L
        return MediaThumbnailRequest(
            uri = uriString,
            mediaId = mediaId,
            dateModified = dateModified,
            widthPx = widthPx.coerceIn(1, MAX_THUMBNAIL_DIMENSION_PX),
            heightPx = heightPx.coerceIn(1, MAX_THUMBNAIL_DIMENSION_PX),
            isVideo = isVideo
        )
    }

    suspend fun createRequest(
        uriString: String,
        isVideo: Boolean,
        widthPx: Int,
        heightPx: Int
    ): MediaThumbnailRequest = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        var mediaId = MediaThumbnailRequest.fallbackMediaId(uriString)
        var dateModified = 0L
        try {
            contentResolver.query(
                uri,
                arrayOf(android.provider.MediaStore.MediaColumns._ID, android.provider.MediaStore.MediaColumns.DATE_MODIFIED),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns._ID)
                    val modifiedColumn = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATE_MODIFIED)
                    if (idColumn >= 0) mediaId = cursor.getString(idColumn) ?: mediaId
                    if (modifiedColumn >= 0) dateModified = cursor.getLong(modifiedColumn) * 1_000L
                }
            }
        } catch (exception: SecurityException) {
            Log.w(TAG, "Unable to read thumbnail metadata for the selected media URI.", exception)
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "Unable to query thumbnail metadata for the selected media URI.", exception)
        }

        metadataCache[uriString] = Pair(mediaId, dateModified)

        MediaThumbnailRequest(
            uri = uriString,
            mediaId = mediaId,
            dateModified = dateModified,
            widthPx = widthPx.coerceIn(1, MAX_THUMBNAIL_DIMENSION_PX),
            heightPx = heightPx.coerceIn(1, MAX_THUMBNAIL_DIMENSION_PX),
            isVideo = isVideo
        )
    }

    suspend fun getThumbnailFile(request: MediaThumbnailRequest): File {
        val key = request.cacheKey
        val cachedFile = File(thumbnailDirectory, "$key.webp")
        val deferred = cacheMutationMutex.withLock {
            if (cachedFile.isFile && cachedFile.length() > 0L) {
                null
            } else {
                inFlight.computeIfAbsent(key) {
                    workerScope.async {
                        decodeSemaphore.withPermit {
                            if (cachedFile.isFile && cachedFile.length() > 0L) return@withPermit cachedFile
                            thumbnailDirectory.mkdirs()
                            val bitmap = createBitmap(request)
                            try {
                                val temporaryFile = File(thumbnailDirectory, "$key.tmp")
                                temporaryFile.outputStream().buffered().use { output ->
                                    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        Bitmap.CompressFormat.WEBP_LOSSY
                                    } else {
                                        @Suppress("DEPRECATION")
                                        Bitmap.CompressFormat.WEBP
                                    }
                                    check(bitmap.compress(format, WEBP_QUALITY, output)) {
                                        "Failed to encode a thumbnail for media $key."
                                    }
                                }
                                check(temporaryFile.renameTo(cachedFile)) {
                                    "Failed to persist a thumbnail for media $key."
                                }
                                removeOutdatedSizes(request)
                                cachedFile
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }.also { job ->
                        job.invokeOnCompletion { inFlight.remove(key, job) }
                    }
                }
            }
        }
        return deferred?.await() ?: cachedFile
    }

    fun prefetchAlbum(coverUri: String?, mediaUris: List<String>) {
        workerScope.launch {
            if (coverUri != null) {
                try {
                    prefetchOne(coverUri, COVER_THUMBNAIL_SIZE_PX, COVER_THUMBNAIL_SIZE_PX)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Log.w(TAG, "Unable to prefetch the album cover thumbnail.", exception)
                }
            }
            mediaUris.distinct().take(PREFETCH_INITIAL_ITEM_COUNT).forEachIndexed { index, uriString ->
                launch {
                    try {
                        prefetchOne(uriString, GRID_THUMBNAIL_SIZE_PX, GRID_THUMBNAIL_SIZE_PX)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        Log.w(TAG, "Unable to prefetch initial thumbnail ${index + 1}.", exception)
                    }
                }
            }
        }
    }

    fun prefetch(uriStrings: List<String>, widthPx: Int = GRID_THUMBNAIL_SIZE_PX, heightPx: Int = GRID_THUMBNAIL_SIZE_PX) {
        val distinctUris = uriStrings.distinct()
        if (distinctUris.isEmpty()) return
        activePrefetchJob?.cancel()
        activePrefetchJob = workerScope.launch {
            distinctUris.forEachIndexed { index, uriString ->
                launch {
                    try {
                        prefetchOne(uriString, widthPx, heightPx)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        Log.w(TAG, "Unable to prefetch upcoming thumbnail ${index + 1}.", exception)
                    }
                }
            }
        }
    }

    private suspend fun prefetchOne(uriString: String, widthPx: Int, heightPx: Int) {
        val isVideo = com.example.red_panda.data.local.MediaStoreHelper.isVideoUri(appContext, uriString)
        val request = getOrCreateRequest(uriString, isVideo, widthPx, heightPx)
        val key = request.cacheKey
        val cachedFile = File(thumbnailDirectory, "$key.webp")
        if (cachedFile.isFile && cachedFile.length() > 0L) return
        if (inFlight.containsKey(key)) return
        getThumbnailFile(request)
    }

    suspend fun cacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        thumbnailDirectory.walkTopDown().filter(File::isFile).sumOf(File::length)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheMutationMutex.lock()
        try {
            metadataCache.clear()
            val pendingJobs = inFlight.values.toList()
            pendingJobs.forEach { it.cancel() }
            pendingJobs.forEach { it.cancelAndJoin() }
            if (thumbnailDirectory.exists() && !thumbnailDirectory.deleteRecursively()) {
                error("Unable to clear the app-managed thumbnail cache.")
            }
            check(thumbnailDirectory.mkdirs() || thumbnailDirectory.isDirectory) {
                "Unable to recreate the app-managed thumbnail cache directory."
            }
        } finally {
            cacheMutationMutex.unlock()
        }
    }

    private fun createBitmap(request: MediaThumbnailRequest): Bitmap {
        val uri = Uri.parse(request.uri)
        val requestedSize = Size(request.widthPx, request.heightPx)
        val bitmap = if (request.isVideo) {
            loadPlatformThumbnail(uri, requestedSize)
                ?: decodeVideoFrame(uri, requestedSize)
                ?: error("No video thumbnail could be decoded for ${request.cacheKey}.")
        } else {
            loadPlatformThumbnail(uri, requestedSize)
                ?: decodeSampledImage(contentResolver, uri, requestedSize)
        }
        return scaleToFit(bitmap, request.widthPx, request.heightPx)
    }

    private fun loadPlatformThumbnail(uri: Uri, requestedSize: Size): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            contentResolver.loadThumbnail(uri, requestedSize, null)
        } catch (exception: IOException) {
            Log.w(TAG, "Platform thumbnail loading failed; trying a decoder fallback.", exception)
            null
        } catch (exception: RuntimeException) {
            Log.w(TAG, "Platform thumbnail loading failed; trying a decoder fallback.", exception)
            null
        }
    }

    private fun decodeVideoFrame(uri: Uri, requestedSize: Size): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
            val frameMs = if (durationMs > 0L) {
                minOf(VIDEO_FRAME_TIME_MS, durationMs / 10L)
                    .coerceAtMost((durationMs - 1L).coerceAtLeast(0L))
            } else {
                VIDEO_FRAME_TIME_MS
            }
            val timeUs = frameMs * 1_000L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    requestedSize.width,
                    requestedSize.height
                )
            } else {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.let { scaleToFit(it, requestedSize.width, requestedSize.height) }
            }
        } catch (exception: IOException) {
            Log.w(TAG, "Video frame decoding failed; trying a platform thumbnail fallback.", exception)
            null
        } catch (exception: RuntimeException) {
            Log.w(TAG, "Video frame decoding failed; trying a platform thumbnail fallback.", exception)
            null
        } finally {
            try {
                retriever.release()
            } catch (exception: IOException) {
                Log.w(TAG, "Unable to release the video thumbnail decoder.", exception)
            }
        }
    }

    private fun decodeSampledImage(resolver: ContentResolver, uri: Uri, requestedSize: Size): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor, null, bounds)
        } ?: error("Unable to open image media ${uri.lastPathSegment}.")

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, requestedSize.width, requestedSize.height)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor, null, options)
        } ?: error("Unable to decode image media ${uri.lastPathSegment}.")
    }

    private fun calculateInSampleSize(
        bounds: BitmapFactory.Options,
        requestedWidth: Int,
        requestedHeight: Int
    ): Int {
        var sampleSize = 1
        while (bounds.outHeight / (sampleSize * 2) >= requestedHeight &&
            bounds.outWidth / (sampleSize * 2) >= requestedWidth
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val scale = minOf(
            1f,
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )
        if (scale >= 1f) return bitmap
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun removeOutdatedSizes(request: MediaThumbnailRequest) {
        val mediaPrefix = "${if (request.isVideo) "video" else "image"}_${request.mediaId}_"
        val currentPrefix = "$mediaPrefix${request.dateModified}_"
        thumbnailDirectory.listFiles()?.forEach { file ->
            if (file.name.startsWith(mediaPrefix) &&
                file.name.endsWith(".webp") &&
                !file.name.startsWith(currentPrefix)
            ) {
                file.delete()
            }
        }
    }

    private companion object {
        const val TAG = "ThumbnailRepository"
        const val THUMBNAIL_DIRECTORY = "thumbs"
        const val MAX_CONCURRENT_DECODES = 4
        const val MAX_THUMBNAIL_DIMENSION_PX = 1_024
        const val GRID_THUMBNAIL_SIZE_PX = 256
        const val COVER_THUMBNAIL_SIZE_PX = 768
        const val PREFETCH_INITIAL_ITEM_COUNT = 60
        const val VIDEO_FRAME_TIME_MS = 1_000L
        const val WEBP_QUALITY = 70
    }
}
