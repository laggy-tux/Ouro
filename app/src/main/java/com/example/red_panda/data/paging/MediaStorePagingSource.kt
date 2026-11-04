package com.example.red_panda.data.paging

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.red_panda.data.local.LocalPhotoItem
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MediaStorePagingSource(
    private val context: Context,
    private val taggedPhotoUris: Set<String> = emptySet(),
    private val filterUntaggedOnly: Boolean = false,
    private val favoritePhotoUris: Set<String> = emptySet(),
    private val filterFavoritesOnly: Boolean = false,
    private val pageSize: Int = 60
) : PagingSource<Int, LocalPhotoItem>() {

    private var contentObserver: ContentObserver? = null

    init {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                invalidate()
            }
        }
        contentObserver = observer
        context.contentResolver.registerContentObserver(
            MediaStore.Files.getContentUri("external"),
            true,
            observer
        )

        registerInvalidatedCallback {
            contentObserver?.let {
                context.contentResolver.unregisterContentObserver(it)
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, LocalPhotoItem>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition) ?: return null
        return anchorPage.prevKey?.plus(pageSize) ?: 0
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LocalPhotoItem> {
        val offset = params.key ?: 0
        val loadSize = params.loadSize

        try {
            val queryCount = offset + loadSize
            val images = queryMedia(
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                queryCount = queryCount,
                isVideo = false
            )
            val videos = queryMedia(
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                queryCount = queryCount,
                isVideo = true
            )
            val media = (images + videos)
                .sortedWith(compareByDescending<LocalPhotoItem> { it.takenAt }.thenByDescending { it.id })
            val photos = media.drop(offset).take(loadSize)

            val filteredPhotos = when {
                filterFavoritesOnly -> photos.filter { it.contentUri in favoritePhotoUris }
                filterUntaggedOnly && taggedPhotoUris.isNotEmpty() -> photos.filter { it.contentUri !in taggedPhotoUris }
                else -> photos
            }

            val nextKey = if (media.size < queryCount) null else offset + photos.size
            val prevKey = if (offset == 0) null else (offset - pageSize).coerceAtLeast(0)

            return LoadResult.Page(
                data = filteredPhotos,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

    private fun queryMedia(
        collection: android.net.Uri,
        queryCount: Int,
        isVideo: Boolean
    ): List<LocalPhotoItem> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
        )
        val queryArgs = Bundle().apply {
            putInt(ContentResolver.QUERY_ARG_LIMIT, queryCount)
            putInt(ContentResolver.QUERY_ARG_OFFSET, 0)
            putString(
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "CASE WHEN ${MediaStore.MediaColumns.DATE_TAKEN} > 0 " +
                    "THEN ${MediaStore.MediaColumns.DATE_TAKEN} " +
                    "ELSE ${MediaStore.MediaColumns.DATE_ADDED} * 1000 END DESC, " +
                    "${MediaStore.MediaColumns._ID} DESC"
            )
        }
        val cursor = context.contentResolver.query(collection, projection, queryArgs, null)
            ?: error("MediaStore returned no cursor while loading ${if (isVideo) "videos" else "images"}.")

        return cursor.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateTakenColumn = c.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            val dateAddedColumn = c.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val widthColumn = c.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightColumn = c.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            buildList {
                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val dateTaken = (if (dateTakenColumn >= 0) c.getLong(dateTakenColumn) else 0L)
                        .takeIf { it > 0L }
                        ?: ((if (dateAddedColumn >= 0) c.getLong(dateAddedColumn) else 0L) * 1000L)
                    val takenAt = dateTaken.takeIf { it > 0L } ?: System.currentTimeMillis()
                    val uri = ContentUris.withAppendedId(collection, id).toString()
                    add(
                        LocalPhotoItem(
                            id = id,
                            contentUri = uri,
                            takenAt = takenAt,
                            dateString = formatDate(takenAt),
                            width = if (widthColumn >= 0) c.getInt(widthColumn) else 0,
                            height = if (heightColumn >= 0) c.getInt(heightColumn) else 0,
                            isVideo = isVideo
                        )
                    )
                }
            }
        }
    }

    companion object {
        fun formatDate(dateTaken: Long): String {
            return try {
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
                Instant.ofEpochMilli(dateTaken)
                    .atZone(ZoneId.systemDefault())
                    .format(formatter)
            } catch (_: Exception) {
                "Unknown Date"
            }
        }

        fun formatDateHeader(dateTaken: Long): String {
            return try {
                val zoneId = ZoneId.systemDefault()
                val photoDate = Instant.ofEpochMilli(dateTaken).atZone(zoneId).toLocalDate()
                val today = LocalDate.now(zoneId)
                val yesterday = today.minusDays(1)

                when (photoDate) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> {
                        val pattern = if (photoDate.year == today.year) "MMM d" else "MMM d, yyyy"
                        photoDate.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
                    }
                }
            } catch (_: Exception) {
                "Unknown Date"
            }
        }
    }
}
