package com.example.red_panda

import android.app.Application
import com.example.red_panda.data.local.AppDatabase
import com.example.red_panda.data.repository.GalleryRepositoryImpl

class RedPandaApplication : Application() {
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
    }
}
