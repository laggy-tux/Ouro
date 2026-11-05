package com.example.red_panda.data.local

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class PhotoAccessState {
    FULL,
    PARTIAL,
    DENIED
}

data class LocalPhotoItem(
    val id: Long,
    val contentUri: String,
    val takenAt: Long,
    val dateString: String,
    val width: Int = 0,
    val height: Int = 0,
    val isVideo: Boolean = false,
    val folderId: String = "",
    val folderName: String = "Other",
)

data class MediaFolder(
    val id: String,
    val name: String
)

data class SuggestedEventItem(
    val id: String,
    val dateString: String,
    val dateMillis: Long,
    val photos: List<LocalPhotoItem>
)

object MediaStoreHelper {
    fun getMediaStoreFolders(context: Context): List<MediaFolder> {
        val folders = linkedMapOf<String, MediaFolder>()
        listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to
                arrayOf(MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME),
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to
                arrayOf(MediaStore.Video.Media.BUCKET_ID, MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        ).forEach { (collection, projection) ->
            val cursor = context.contentResolver.query(collection, projection, null, null, null)
                ?: error("MediaStore returned no cursor while loading media folders.")
            cursor.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                while (it.moveToNext()) {
                    val id = it.getString(idColumn).orEmpty()
                    val name = it.getString(nameColumn)?.takeIf(String::isNotBlank) ?: "Other"
                    folders.putIfAbsent(id, MediaFolder(id, name))
                }
            }
        }
        return folders.values.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    fun fetchImageUrisForDate(context: Context, date: java.time.LocalDate): List<String> {
        val zone = ZoneId.systemDefault()
        val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val selection =
            "(${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} < ?) " +
                "OR ((${MediaStore.Images.Media.DATE_TAKEN} IS NULL OR ${MediaStore.Images.Media.DATE_TAKEN} <= 0) " +
                "AND ${MediaStore.Images.Media.DATE_ADDED} >= ? AND ${MediaStore.Images.Media.DATE_ADDED} < ?)"
        val selectionArgs = arrayOf(
            startMillis.toString(),
            endMillis.toString(),
            (startMillis / 1000L).toString(),
            (endMillis / 1000L).toString()
        )
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            selection,
            selectionArgs,
            null
        ) ?: error("MediaStore returned no cursor while selecting images by day.")

        return cursor.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            buildList {
                while (it.moveToNext()) {
                    add(
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            it.getLong(idColumn)
                        ).toString()
                    )
                }
            }
        }
    }

    fun persistMediaReadPermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (exception: SecurityException) {
            Log.w(TAG, "The selected media URI did not grant persistent read access: $uri", exception)
        }
    }


    fun getPhotoAccessState(context: Context): PhotoAccessState {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                val fullGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_VIDEO
                    ) == PackageManager.PERMISSION_GRANTED

                val partialGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                ) == PackageManager.PERMISSION_GRANTED

                if (fullGranted) PhotoAccessState.FULL
                else if (partialGranted) PhotoAccessState.PARTIAL
                else PhotoAccessState.DENIED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val fullGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_VIDEO
                    ) == PackageManager.PERMISSION_GRANTED

                if (fullGranted) PhotoAccessState.FULL else PhotoAccessState.DENIED
            }
            else -> {
                val fullGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                if (fullGranted) PhotoAccessState.FULL else PhotoAccessState.DENIED
            }
        }
    }

    fun isUriAccessible(context: Context, uriString: String): Boolean {
        if (uriString.isBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun calculateContentHash(context: Context, uriString: String): String {
        if (uriString.isBlank()) return ""
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val md = MessageDigest.getInstance("MD5")
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0
                while (stream.read(buffer).also { bytesRead = it } != -1 && totalRead < 65536) {
                    md.update(buffer, 0, bytesRead)
                    totalRead += bytesRead
                }
                md.digest().joinToString("") { "%02x".format(it) }
            } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun reResolvePhotoByHash(context: Context, targetHash: String): String? {
        if (targetHash.isBlank()) return null
        val currentMediaUris = fetchAllMediaStorePhotos(context).map { it.contentUri } +
            buildList {
                val cursor = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Video.Media._ID),
                    null,
                    null,
                    null
                )
                requireNotNull(cursor) { "MediaStore returned no cursor while resolving video references." }
                    .use { videoCursor ->
                        val idColumn = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                        while (videoCursor.moveToNext()) {
                            add(
                                ContentUris.withAppendedId(
                                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                    videoCursor.getLong(idColumn)
                                ).toString()
                            )
                        }
                    }
            }
        for (mediaUri in currentMediaUris) {
            val hash = calculateContentHash(context, mediaUri)
            if (hash == targetHash && hash.isNotBlank()) {
                return mediaUri
            }
        }
        return null
    }

    private fun formatDate(dateTaken: Long): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
            Instant.ofEpochMilli(dateTaken)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
        } catch (_: Exception) {
            "Unknown Date"
        }
    }

    fun getDefaultEventDate(context: Context, uriStrings: List<String>): String? {
        val metadataDates = uriStrings.mapNotNull { uriString ->
            val uri = Uri.parse(uriString)
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        MediaStore.Images.Media.DATE_TAKEN,
                        MediaStore.Images.Media.DATE_ADDED
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (!cursor.moveToFirst()) {
                        null
                    } else {
                        val takenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                        val addedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                        val takenAt = if (takenColumn >= 0) cursor.getLong(takenColumn) else 0L
                        val addedAt = if (addedColumn >= 0) cursor.getLong(addedColumn) * 1000L else 0L
                        takenAt.takeIf { it > 0L } to addedAt.takeIf { it > 0L }
                    }
                }
            } catch (_: SecurityException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        val dateMillis = metadataDates.mapNotNull { it.first }.maxOrNull()
            ?: metadataDates.mapNotNull { it.second }.maxOrNull()
            ?: return null
        return formatDate(dateMillis)
    }

    fun fetchAllMediaStorePhotos(context: Context): List<LocalPhotoItem> {
        val photos = mutableListOf<LocalPhotoItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateTakenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    var dateTaken = if (dateTakenColumn >= 0) cursor.getLong(dateTakenColumn) else 0L
                    if (dateTaken <= 0) {
                        val dateAdded = if (dateAddedColumn >= 0) cursor.getLong(dateAddedColumn) else 0L
                        dateTaken = dateAdded * 1000L
                    }
                    if (dateTaken <= 0) {
                        dateTaken = System.currentTimeMillis()
                    }

                    val width = if (widthColumn >= 0) cursor.getInt(widthColumn) else 0
                    val height = if (heightColumn >= 0) cursor.getInt(heightColumn) else 0

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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return photos
    }

    fun getSuggestedEvents(
        context: Context,
        taggedPhotoUris: Set<String>
    ): List<SuggestedEventItem> {
        val allPhotos = fetchAllMediaStorePhotos(context)
        val untagged = allPhotos.filter { it.contentUri !in taggedPhotoUris }

        val groupedByDate = untagged.groupBy { it.dateString }

        return groupedByDate.mapNotNull { (dateStr, photos) ->
            if (photos.size >= 2) {
                val latestTime = photos.maxOfOrNull { it.takenAt } ?: System.currentTimeMillis()
                val dateGroupKey = Instant.ofEpochMilli(latestTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val photoIdsHash = MessageDigest.getInstance("SHA-256")
                    .digest(photos.map { it.id }.sorted().joinToString(",").toByteArray())
                    .joinToString("") { "%02x".format(it) }
                SuggestedEventItem(
                    id = "$dateGroupKey:$photoIdsHash",
                    dateString = dateStr,
                    dateMillis = latestTime,
                    photos = photos
                )
            } else null
        }.sortedByDescending { it.dateMillis }
    }

    private const val TAG = "MediaStoreHelper"
}
