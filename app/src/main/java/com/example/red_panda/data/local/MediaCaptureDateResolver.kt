package com.example.red_panda.data.local

import android.content.ContentResolver
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.system.Os
import android.util.Log
import java.io.IOException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

/** Resolves the best available capture timestamp without using import or album dates. */
data class MediaCaptureMetadata(
    val mediaStoreId: Long?,
    val takenAt: Long
)

internal fun selectCaptureTime(
    dateTaken: Long?,
    readEmbeddedCaptureTime: () -> Long?,
    mediaStoreModified: Long?,
    readFileModified: () -> Long?
): Long = dateTaken?.takeIf { it > 0L }
    ?: readEmbeddedCaptureTime()?.takeIf { it > 0L }
    ?: mediaStoreModified?.takeIf { it > 0L }
    ?: readFileModified()?.takeIf { it > 0L }
    ?: 0L

class MediaCaptureDateResolver(private val contentResolver: ContentResolver) {
    fun resolve(uriString: String): MediaCaptureMetadata {
        val uri = Uri.parse(uriString)
        val mediaStoreMetadata = queryMediaStore(uri)
        val isVideo = try {
            contentResolver.getType(uri)?.startsWith("video/") == true
        } catch (exception: SecurityException) {
            Log.w(TAG, "Unable to read media type while resolving capture date.", exception)
            false
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "Unable to determine media type while resolving capture date.", exception)
            false
        }
        val captureDate = selectCaptureTime(
            dateTaken = mediaStoreMetadata.dateTaken,
            readEmbeddedCaptureTime = {
                if (isVideo) readVideoCreationDate(uri) else readExifDate(uri)
            },
            mediaStoreModified = mediaStoreMetadata.dateModified,
            readFileModified = { readFileModifiedDate(uri) }
        )

        return MediaCaptureMetadata(
            mediaStoreId = mediaStoreMetadata.id ?: uri.lastPathSegment?.toLongOrNull(),
            takenAt = captureDate
        )
    }

    private fun queryMediaStore(uri: Uri): MediaStoreMetadata {
        return try {
            contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DATE_TAKEN,
                    MediaStore.MediaColumns.DATE_MODIFIED
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return MediaStoreMetadata()
                val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val takenColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                val modifiedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                MediaStoreMetadata(
                    id = if (idColumn >= 0) cursor.getLong(idColumn) else null,
                    dateTaken = if (takenColumn >= 0) cursor.getLong(takenColumn).takeIf { it > 0L } else null,
                    dateModified = if (modifiedColumn >= 0) {
                        cursor.getLong(modifiedColumn).takeIf { it > 0L }?.times(1_000L)
                    } else {
                        null
                    }
                )
            } ?: MediaStoreMetadata()
        } catch (exception: SecurityException) {
            Log.w(TAG, "Unable to query MediaStore capture metadata.", exception)
            MediaStoreMetadata()
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "The media provider does not expose standard capture metadata.", exception)
            MediaStoreMetadata()
        }
    }

    private fun readExifDate(uri: Uri): Long? {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val date = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: return null
                val offset = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)
                parseExifDate(date, offset)
            }
        } catch (exception: IOException) {
            Log.w(TAG, "Unable to read EXIF capture date.", exception)
            null
        } catch (exception: RuntimeException) {
            Log.w(TAG, "Unable to parse EXIF capture metadata.", exception)
            null
        }
    }

    private fun readVideoCreationDate(uri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                retriever.setDataSource(descriptor.fileDescriptor)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)?.let(::parseVideoDate)
            }
        } catch (exception: IOException) {
            Log.w(TAG, "Unable to read video container creation date.", exception)
            null
        } catch (exception: RuntimeException) {
            Log.w(TAG, "Unable to parse video container metadata.", exception)
            null
        } finally {
            try {
                retriever.release()
            } catch (exception: IOException) {
                Log.w(TAG, "Unable to release media metadata retriever.", exception)
            }
        }
    }

    private fun readFileModifiedDate(uri: Uri): Long? {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                Os.fstat(descriptor.fileDescriptor).st_mtime * 1_000L
            }?.takeIf { it > 0L }
        } catch (exception: IOException) {
            Log.w(TAG, "Unable to read media file modification time.", exception)
            null
        } catch (exception: RuntimeException) {
            Log.w(TAG, "Unable to stat the selected media file.", exception)
            null
        }
    }

    private fun parseExifDate(value: String, offset: String?): Long? {
        val formatter = SimpleDateFormat(EXIF_DATE_PATTERN, Locale.US).apply {
            isLenient = false
            if (!offset.isNullOrBlank()) {
                val timeZoneId = if (offset.startsWith("GMT")) offset else "GMT$offset"
                timeZone = TimeZone.getTimeZone(timeZoneId)
            }
        }
        val position = ParsePosition(0)
        val parsed = formatter.parse(value, position)
        return parsed?.time?.takeIf { position.index == value.length }
    }

    private fun parseVideoDate(value: String): Long? {
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
            ?: VIDEO_DATE_FORMATS.firstNotNullOfOrNull { formatter ->
                runCatching {
                    java.time.LocalDateTime.parse(value, formatter)
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli()
                }.getOrNull()
            }
    }

    private data class MediaStoreMetadata(
        val id: Long? = null,
        val dateTaken: Long? = null,
        val dateModified: Long? = null
    )

    private companion object {
        const val TAG = "MediaCaptureDateResolver"
        const val EXIF_DATE_PATTERN = "yyyy:MM:dd HH:mm:ss"
        val VIDEO_DATE_FORMATS = listOf(
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.US),
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
        )
    }
}
