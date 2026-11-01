package com.example.red_panda.data.local

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
    val height: Int = 0
)

data class SuggestedEventItem(
    val dateString: String,
    val dateMillis: Long,
    val photos: List<LocalPhotoItem>
)

object MediaStoreHelper {

    fun getPhotoAccessState(context: Context): PhotoAccessState {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                val fullGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
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
        val currentPhotos = fetchAllMediaStorePhotos(context)
        for (photo in currentPhotos) {
            val hash = calculateContentHash(context, photo.contentUri)
            if (hash == targetHash && hash.isNotBlank()) {
                return photo.contentUri
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
                SuggestedEventItem(
                    dateString = dateStr,
                    dateMillis = latestTime,
                    photos = photos
                )
            } else null
        }.sortedByDescending { it.dateMillis }
    }
}
