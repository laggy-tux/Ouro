package com.example.red_panda.data.thumbnail

import android.net.Uri

/** A stable, size-aware request for an app-managed thumbnail. */
data class MediaThumbnailRequest(
    val uri: String,
    val mediaId: String,
    val dateModified: Long,
    val widthPx: Int,
    val heightPx: Int,
    val isVideo: Boolean
) {
    val cacheKey: String
        get() = "${if (isVideo) "video" else "image"}_${mediaId}_${dateModified}_${widthPx}x$heightPx"

    companion object {
        fun fallbackMediaId(uri: String): String {
            val parsed = Uri.parse(uri)
            return parsed.lastPathSegment?.substringAfterLast('/')?.toLongOrNull()?.toString()
                ?: uri.hashCode().toUInt().toString(16)
        }
    }
}
