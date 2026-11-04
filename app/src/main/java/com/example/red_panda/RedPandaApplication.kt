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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RedPandaApplication : Application() {
    private val maintenanceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val maintenanceHandler = Handler(Looper.getMainLooper())
    private var pendingPhotoCleanup: Runnable? = null
    private var mediaStoreObserver: ContentObserver? = null

    lateinit var repository: GalleryRepositoryImpl
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        repository = GalleryRepositoryImpl(
            context = this,
            categoryDao = database.categoryDao(),
            eventDao = database.eventDao(),
            photoDao = database.photoDao()
        )

        scheduleMissingPhotoCleanup(0)
        mediaStoreObserver = object : ContentObserver(maintenanceHandler) {
            override fun onChange(selfChange: Boolean) {
                scheduleMissingPhotoCleanup()
            }
        }.also { observer ->
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        }
    }

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
