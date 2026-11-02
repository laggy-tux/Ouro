package com.example.red_panda.data.repository

import com.example.red_panda.data.local.entity.CategoryEntity
import com.example.red_panda.data.local.entity.PhotoRefEntity
import com.example.red_panda.ui.model.Album
import com.example.red_panda.ui.model.EventMock
import com.example.red_panda.ui.model.SuggestedEvent
import kotlinx.coroutines.flow.Flow

interface GalleryRepository {
    fun getAlbums(): Flow<List<Album>>
    fun getCategoryById(categoryId: Long): Flow<CategoryEntity?>
    fun getEvent(eventId: Long): Flow<EventMock?>
    fun getEventsForCategory(categoryId: Long): Flow<List<EventMock>>
    fun getPhotosForCategory(categoryId: Long): Flow<List<PhotoRefEntity>>
    fun getTaggedPhotoUris(): Flow<Set<String>>
    fun getFavoritePhotoUris(): Flow<Set<String>>

    suspend fun createCategory(name: String, section: String, coverColor: Long): Long
    suspend fun updateCategory(id: Long, name: String, section: String, coverColor: Long)
    suspend fun updateCategoryCover(categoryId: Long, coverPhotoId: Long?)
    suspend fun setCategoryCoverFromUri(categoryId: Long, uri: String)
    suspend fun toggleCategoryFavorite(categoryId: Long)
    suspend fun deleteCategory(categoryId: Long)

    suspend fun createEvent(
        categoryId: Long,
        title: String,
        date: String,
        location: String,
        note: String,
        placeTag: String,
        personTag: String
    ): Long

    suspend fun updateEvent(
        eventId: Long,
        title: String,
        date: String,
        location: String,
        note: String,
        placeTag: String,
        personTag: String
    )

    suspend fun deleteEvent(eventId: Long)

    suspend fun addPhotosToEvent(eventId: Long, photoUris: List<String>)
    suspend fun removePhotoFromEvent(eventId: Long, photoId: Long): Boolean
    suspend fun togglePhotoFavorite(photoId: Long)
    suspend fun togglePhotoFavoriteByUri(contentUri: String)
    suspend fun batchSetFavorite(photoUris: List<String>, isFavorite: Boolean)

    suspend fun exportBackupJson(): String
    suspend fun importBackupJson(jsonString: String): Pair<Int, Int>

    suspend fun getSuggestedEvents(context: android.content.Context): List<SuggestedEvent>
    suspend fun createEventFromSuggested(
        categoryId: Long,
        title: String,
        date: String,
        photoUris: List<String>
    ): Long

    suspend fun cleanupMissingPhotos(context: android.content.Context): Int
}
