package com.example.red_panda.data.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class MediaThumbnailKeyer : Keyer<MediaThumbnailRequest> {
    override fun key(data: MediaThumbnailRequest, options: Options): String = data.cacheKey
}

class MediaThumbnailFetcher(
    private val data: MediaThumbnailRequest,
    private val context: Context,
    private val repository: ThumbnailRepository
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val file = repository.getThumbnailFile(data)
        val bitmap = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.HARDWARE
                    inMutable = false
                }
            ) ?: throw IOException("Unable to decode cached thumbnail ${file.name}.")
        }
        return DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    class Factory(
        private val context: Context,
        private val repository: ThumbnailRepository
    ) : Fetcher.Factory<MediaThumbnailRequest> {
        override fun create(data: MediaThumbnailRequest, options: Options, imageLoader: ImageLoader): Fetcher =
            MediaThumbnailFetcher(data, context, repository)
    }
}
