package com.example.red_panda

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.example.red_panda.data.local.MediaStoreHelper
import com.example.red_panda.data.local.PhotoAccessState
import com.example.red_panda.data.local.AppDatabase
import com.example.red_panda.data.repository.GalleryRepositoryImpl
import com.example.red_panda.data.thumbnail.MediaThumbnailFetcher
import com.example.red_panda.data.thumbnail.MediaThumbnailKeyer
import com.example.red_panda.data.thumbnail.ThumbnailRepository
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RedPandaApplication : Application(), ImageLoaderFactory {
    lateinit var thumbnailRepository: ThumbnailRepository
        private set
    private val maintenanceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val maintenanceHandler = Handler(Looper.getMainLooper())
    private var pendingPhotoCleanup: Runnable? = null
    private var mediaStoreObserver: ContentObserver? = null

    lateinit var repository: GalleryRepositoryImpl
        private set

    override fun onCreate() {
        super.onCreate()
        thumbnailRepository = ThumbnailRepository(this)
        val database = AppDatabase.getInstance(this)
        repository = GalleryRepositoryImpl(
            context = this,
            categoryDao = database.categoryDao(),
            eventDao = database.eventDao(),
            photoDao = database.photoDao()
        )

        scheduleMissingPhotoCleanup(0)
        maintenanceScope.launch {
            try {
                repository.backfillPhotoCaptureDates()
            } catch (exception: Exception) {
                Log.e(TAG, "Unable to backfill photo capture dates.", exception)
            }
        }
        mediaStoreObserver = object : ContentObserver(maintenanceHandler) {
            override fun onChange(selfChange: Boolean) {
                thumbnailRepository.onMediaStoreChanged()
                scheduleMissingPhotoCleanup()
            }
        }.also { observer ->
            contentResolver.registerContentObserver(
                MediaStore.Files.getContentUri("external"),
                true,
                observer
            )
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.20)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(cacheDir, "coil_images"))
                .maxSizeBytes(100L * 1024L * 1024L)
                .build()
        }
        .components {
            add(MediaThumbnailKeyer())
            add(MediaThumbnailFetcher.Factory(this@RedPandaApplication, thumbnailRepository))
        }
        .build()

    private fun scheduleMissingPhotoCleanup(delayMillis: Long = 750) {
        if (MediaStoreHelper.getPhotoAccessState(this) != PhotoAccessState.FULL) return

        pendingPhotoCleanup?.let(maintenanceHandler::removeCallbacks)
        pendingPhotoCleanup = Runnable {
            maintenanceScope.launch {
                try {
                    repository.cleanupMissingPhotos(this@RedPandaApplication)
                } catch (exception: Exception) {
                    Log.e(TAG, "Unable to reconcile missing photo references.", exception)
                }
            }
        }.also { maintenanceHandler.postDelayed(it, delayMillis) }
    }

    private companion object {
        const val TAG = "RedPandaApplication"
    }
}
