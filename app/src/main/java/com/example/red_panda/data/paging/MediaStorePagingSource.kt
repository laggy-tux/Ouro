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
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
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

        val photos = mutableListOf<LocalPhotoItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        try {
            val queryArgs = Bundle().apply {
                putInt(ContentResolver.QUERY_ARG_LIMIT, loadSize)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                putString(
                    ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                    "CASE WHEN ${MediaStore.Images.Media.DATE_TAKEN} > 0 " +
                        "THEN ${MediaStore.Images.Media.DATE_TAKEN} " +
                        "ELSE ${MediaStore.Images.Media.DATE_ADDED} * 1000 END DESC, " +
                        "${MediaStore.Images.Media._ID} DESC"
                )
            }
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                queryArgs,
                null
            )

            requireNotNull(cursor) { "MediaStore returned no cursor while loading photos." }.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateTakenColumn = c.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedColumn = c.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val widthColumn = c.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightColumn = c.getColumnIndex(MediaStore.Images.Media.HEIGHT)

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    var dateTaken = if (dateTakenColumn >= 0) c.getLong(dateTakenColumn) else 0L
                    if (dateTaken <= 0) {
                        val dateAdded = if (dateAddedColumn >= 0) c.getLong(dateAddedColumn) else 0L
                        dateTaken = dateAdded * 1000L
                    }
                    if (dateTaken <= 0) {
                        dateTaken = System.currentTimeMillis()
                    }

                    val width = if (widthColumn >= 0) c.getInt(widthColumn) else 0
                    val height = if (heightColumn >= 0) c.getInt(heightColumn) else 0

                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val dateString = formatDate(dateTaken)

                    photos.add(
                        LocalPhotoItem(
                            id = id,
                            contentUri = uri,
                            takenAt = dateTaken,
                            dateString = dateString,
                            width = width,
                            height = height
                        )
                    )
                }
            }

            val filteredPhotos = when {
                filterFavoritesOnly -> photos.filter { it.contentUri in favoritePhotoUris }
                filterUntaggedOnly && taggedPhotoUris.isNotEmpty() -> photos.filter { it.contentUri !in taggedPhotoUris }
                else -> photos
            }

            val nextKey = if (photos.size < loadSize) null else offset + photos.size
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
                    else -> photoDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault()))
                }
            } catch (_: Exception) {
                "Unknown Date"
            }
        }
    }
}
