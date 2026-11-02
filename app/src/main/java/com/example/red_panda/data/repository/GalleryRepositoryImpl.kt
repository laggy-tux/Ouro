package com.example.red_panda.data.repository

import android.content.Context
import com.example.red_panda.data.local.MediaStoreHelper
import com.example.red_panda.data.local.dao.CategoryDao
import com.example.red_panda.data.local.dao.EventDao
import com.example.red_panda.data.local.dao.PhotoDao
import com.example.red_panda.data.local.entity.CategoryEntity
import com.example.red_panda.data.local.entity.EventEntity
import com.example.red_panda.data.local.entity.EventPhotoEntity
import com.example.red_panda.data.local.entity.PhotoRefEntity
import com.example.red_panda.ui.model.Album
import com.example.red_panda.ui.model.EventMock
import com.example.red_panda.ui.model.SuggestedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GalleryRepositoryImpl(
    private val context: Context,
    private val categoryDao: CategoryDao,
    private val eventDao: EventDao,
    private val photoDao: PhotoDao
) : GalleryRepository {

    private fun resolveCoverPhotoUri(category: CategoryEntity): String? {
        if (category.coverPhotoId != null) {
            val customCover = photoDao.getPhotoByIdSync(category.coverPhotoId)
            if (customCover != null && MediaStoreHelper.isUriAccessible(context, customCover.contentUri)) {
                return customCover.contentUri
            }
        }
        val categoryPhotos = photoDao.getPhotosForCategorySync(category.id)
        for (photo in categoryPhotos) {
            if (MediaStoreHelper.isUriAccessible(context, photo.contentUri)) {
                if (category.coverPhotoId != photo.id || category.coverPhotoIsManual) {
                    categoryDao.updateCategory(
                        category.copy(coverPhotoId = photo.id, coverPhotoIsManual = false)
                    )
                }
                return photo.contentUri
            }
        }
        if (category.coverPhotoId != null) {
            categoryDao.updateCategory(category.copy(coverPhotoId = null, coverPhotoIsManual = false))
        }
        return null
    }

    private fun toEventMock(event: EventEntity, photos: List<PhotoRefEntity>): EventMock {
        return EventMock(
            id = event.id.toString(),
            title = event.title,
            date = event.date,
            location = event.location,
            totalPhotos = photos.size,
            thumbnailColors = photos.map { it.placeholderColor },
            remainingPhotosCount = (photos.size - 5).coerceAtLeast(0),
            placeTag = if (event.placeTag.isNotBlank()) event.placeTag else "All Places",
            personTag = if (event.personTag.isNotBlank()) event.personTag else "All People",
            note = event.note,
            photoUris = photos.map { it.contentUri },
            photoRefs = photos
        )
    }

    override fun getAlbums(): Flow<List<Album>> {
        return categoryDao.getAllCategories().map { categories ->
            categories.map { category ->
                val events = eventDao.getEventsForCategorySync(category.id)
                val eventMocks = events.map { event ->
                    toEventMock(event, photoDao.getPhotosForEventSync(event.id))
                }
                val totalPhotoCount = if (eventMocks.isNotEmpty()) eventMocks.sumOf { it.totalPhotos } else 0
                val resolvedCoverUri = resolveCoverPhotoUri(category)

                Album(
                    id = category.id.toString(),
                    title = category.name,
                    photoCount = totalPhotoCount,
                    section = category.section,
                    isFavorite = category.isFavorite,
                    coverColor = category.coverColor,
                    coverPhotoUri = resolvedCoverUri,
                    coverPhotoId = category.coverPhotoId,
                    events = eventMocks
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getCategoryById(categoryId: Long): Flow<CategoryEntity?> {
        return categoryDao.getCategoryById(categoryId).flowOn(Dispatchers.IO)
    }

    override fun getEvent(eventId: Long): Flow<EventMock?> {
        return combine(
            eventDao.getEventById(eventId),
            photoDao.getPhotosForEvent(eventId)
        ) { event, photos ->
            event?.let { toEventMock(it, photos) }
        }.flowOn(Dispatchers.IO)
    }

    override fun getEventsForCategory(categoryId: Long): Flow<List<EventMock>> {
        return eventDao.getEventsForCategory(categoryId)
            .map { events ->
                events.map { event ->
                    toEventMock(event, photoDao.getPhotosForEventSync(event.id))
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override fun getPhotosForCategory(categoryId: Long): Flow<List<PhotoRefEntity>> {
        return photoDao.getPhotosForCategory(categoryId).flowOn(Dispatchers.IO)
    }

    override fun getTaggedPhotoUris(): Flow<Set<String>> {
        return photoDao.getAllTaggedPhotoUrisFlow().map { it.toSet() }.flowOn(Dispatchers.IO)
    }

    override fun getFavoritePhotoUris(): Flow<Set<String>> {
        return photoDao.getFavoritePhotoUrisFlow().map { it.toSet() }.flowOn(Dispatchers.IO)
    }

    override suspend fun createCategory(name: String, section: String, coverColor: Long): Long =
        withContext(Dispatchers.IO) {
            val entity = CategoryEntity(
                name = name,
                section = section,
                coverColor = coverColor,
                isFavorite = false
            )
            categoryDao.insertCategory(entity)
        }

    override suspend fun updateCategory(id: Long, name: String, section: String, coverColor: Long) =
        withContext(Dispatchers.IO) {
            val existing = categoryDao.getCategoryByIdSync(id) ?: return@withContext
            val updated = existing.copy(
                name = name,
                section = section,
                coverColor = coverColor
            )
            categoryDao.updateCategory(updated)
            Unit
        }

    override suspend fun updateCategoryCover(categoryId: Long, coverPhotoId: Long?) =
        withContext(Dispatchers.IO) {
            val existing = categoryDao.getCategoryByIdSync(categoryId) ?: return@withContext
            val selectedCoverId = coverPhotoId ?: photoDao.getEarliestPhotoForCategorySync(categoryId)?.id
            val updated = existing.copy(
                coverPhotoId = selectedCoverId,
                coverPhotoIsManual = coverPhotoId != null
            )
            categoryDao.updateCategory(updated)
            Unit
        }

    override suspend fun setCategoryCoverFromUri(categoryId: Long, uri: String) =
        withContext(Dispatchers.IO) {
            val existing = categoryDao.getCategoryByIdSync(categoryId) ?: return@withContext
            val existingPhoto = photoDao.getPhotoByUri(uri)
            val photoId = if (existingPhoto != null) {
                existingPhoto.id
            } else {
                val hash = MediaStoreHelper.calculateContentHash(context, uri)
                val newPhoto = PhotoRefEntity(
                    contentUri = uri,
                    contentHash = hash,
                    takenAt = System.currentTimeMillis()
                )
                photoDao.insertPhoto(newPhoto)
            }
            val updated = existing.copy(coverPhotoId = photoId, coverPhotoIsManual = true)
            categoryDao.updateCategory(updated)
            Unit
        }

    override suspend fun toggleCategoryFavorite(categoryId: Long) =
        withContext(Dispatchers.IO) {
            val existing = categoryDao.getCategoryByIdSync(categoryId) ?: return@withContext
            val updated = existing.copy(isFavorite = !existing.isFavorite)
            categoryDao.updateCategory(updated)
            Unit
        }

    override suspend fun deleteCategory(categoryId: Long) =
        withContext(Dispatchers.IO) {
            categoryDao.deleteCategoryById(categoryId)
            Unit
        }

    override suspend fun createEvent(
        categoryId: Long,
        title: String,
        date: String,
        location: String,
        note: String,
        placeTag: String,
        personTag: String
    ): Long = withContext(Dispatchers.IO) {
        val entity = EventEntity(
            categoryId = categoryId,
            title = title,
            date = date,
            location = location,
            note = note,
            placeTag = placeTag,
            personTag = personTag
        )
        eventDao.insertEvent(entity)
    }

    override suspend fun updateEvent(
        eventId: Long,
        title: String,
        date: String,
        location: String,
        note: String,
        placeTag: String,
        personTag: String
    ) = withContext(Dispatchers.IO) {
        val existing = eventDao.getEventByIdSync(eventId) ?: return@withContext
        val updated = existing.copy(
            title = title,
            date = date,
            location = location,
            note = note,
            placeTag = placeTag,
            personTag = personTag
        )
        eventDao.updateEvent(updated)
        Unit
    }

    override suspend fun deleteEvent(eventId: Long) =
        withContext(Dispatchers.IO) {
            eventDao.deleteEventById(eventId)
            Unit
        }

    override suspend fun addPhotosToEvent(eventId: Long, photoUris: List<String>) =
        withContext(Dispatchers.IO) {
            var event = eventDao.getEventByIdSync(eventId) ?: return@withContext
            var category = categoryDao.getCategoryByIdSync(event.categoryId) ?: return@withContext
            var categoryHadPhotos = photoDao.getPhotoCountForCategorySync(category.id) > 0

            photoUris.forEach { uri ->
                val existingPhoto = photoDao.getPhotoByUri(uri)
                val photoId = if (existingPhoto != null) {
                    existingPhoto.id
                } else {
                    val hash = MediaStoreHelper.calculateContentHash(context, uri)
                    photoDao.insertPhoto(
                        PhotoRefEntity(
                            contentUri = uri,
                            contentHash = hash,
                            takenAt = System.currentTimeMillis()
                        )
                    )
                }
                val insertion = photoDao.insertEventPhoto(
                    EventPhotoEntity(
                        eventId = eventId,
                        photoId = photoId,
                        position = photoDao.getNextPositionForEventSync(eventId)
                    )
                )
                if (insertion != -1L) {
                    if (event.coverPhotoId == null) {
                        event = event.copy(coverPhotoId = photoId, coverPhotoIsManual = false)
                        eventDao.updateEvent(event)
                    }
                    if (!categoryHadPhotos && category.coverPhotoId == null) {
                        category = category.copy(coverPhotoId = photoId, coverPhotoIsManual = false)
                        categoryDao.updateCategory(category)
                    }
                    categoryHadPhotos = true
                }
            }
        }

    override suspend fun removePhotoFromEvent(eventId: Long, photoId: Long): Boolean =
        withContext(Dispatchers.IO) {
            val event = eventDao.getEventByIdSync(eventId) ?: return@withContext false
            if (photoDao.removePhotoFromEvent(eventId, photoId) == 0) {
                return@withContext false
            }

            var coverChanged = false
            if (event.coverPhotoId == photoId) {
                val fallback = photoDao.getPhotosForEventSync(eventId).firstOrNull()
                eventDao.updateEvent(
                    event.copy(coverPhotoId = fallback?.id, coverPhotoIsManual = false)
                )
                coverChanged = true
            }

            val category = categoryDao.getCategoryByIdSync(event.categoryId)
            if (
                category?.coverPhotoId == photoId &&
                !photoDao.isPhotoInCategorySync(event.categoryId, photoId)
            ) {
                val fallback = photoDao.getEarliestPhotoForCategorySync(event.categoryId)
                categoryDao.updateCategory(
                    category.copy(coverPhotoId = fallback?.id, coverPhotoIsManual = false)
                )
                coverChanged = true
            }
            coverChanged
        }

    override suspend fun togglePhotoFavorite(photoId: Long) =
        withContext(Dispatchers.IO) {
            val photo = photoDao.getPhotoByIdSync(photoId)
            if (photo != null) {
                photoDao.updatePhoto(photo.copy(isFavorite = !photo.isFavorite))
            }
        }

    override suspend fun togglePhotoFavoriteByUri(contentUri: String) =
        withContext(Dispatchers.IO) {
            val photo = photoDao.getPhotoByUri(contentUri)
            if (photo != null) {
                photoDao.updatePhoto(photo.copy(isFavorite = !photo.isFavorite))
            } else {
                photoDao.insertPhoto(
                    PhotoRefEntity(
                        contentUri = contentUri,
                        isFavorite = true,
                        takenAt = System.currentTimeMillis()
                    )
                )
            }
            Unit
        }

    override suspend fun batchSetFavorite(photoUris: List<String>, isFavorite: Boolean) =
        withContext(Dispatchers.IO) {
            photoUris.forEach { uri ->
                val photo = photoDao.getPhotoByUri(uri)
                if (photo != null) {
                    photoDao.updatePhoto(photo.copy(isFavorite = isFavorite))
                } else if (isFavorite) {
                    photoDao.insertPhoto(
                        PhotoRefEntity(
                            contentUri = uri,
                            isFavorite = true,
                            takenAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

    override suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = org.json.JSONObject()
        root.put("version", 1)
        root.put("app", "RedPanda")
        root.put("exportedAt", System.currentTimeMillis())

        val categoriesJson = org.json.JSONArray()
        val categories = categoryDao.getAllCategoriesSync()
        for (cat in categories) {
            val catObj = org.json.JSONObject()
            catObj.put("name", cat.name)
            catObj.put("section", cat.section)
            catObj.put("coverColor", cat.coverColor)
            catObj.put("isFavorite", cat.isFavorite)

            val eventsJson = org.json.JSONArray()
            val events = eventDao.getEventsForCategorySync(cat.id)
            for (event in events) {
                val eventObj = org.json.JSONObject()
                eventObj.put("title", event.title)
                eventObj.put("date", event.date)
                eventObj.put("location", event.location)
                eventObj.put("note", event.note)
                eventObj.put("placeTag", event.placeTag)
                eventObj.put("personTag", event.personTag)

                val photosJson = org.json.JSONArray()
                val photos = photoDao.getPhotosForEventSync(event.id)
                for (photo in photos) {
                    val photoObj = org.json.JSONObject()
                    photoObj.put("contentUri", photo.contentUri)
                    photoObj.put("contentHash", photo.contentHash)
                    photoObj.put("takenAt", photo.takenAt)
                    photoObj.put("isFavorite", photo.isFavorite)
                    photosJson.put(photoObj)
                }
                eventObj.put("photos", photosJson)
                eventsJson.put(eventObj)
            }
            catObj.put("events", eventsJson)
            categoriesJson.put(catObj)
        }
        root.put("categories", categoriesJson)
        root.toString(2)
    }

    override suspend fun importBackupJson(jsonString: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val root = org.json.JSONObject(jsonString)
        val categoriesJson = root.optJSONArray("categories") ?: return@withContext Pair(0, 0)
        var restoredCategories = 0
        var restoredEvents = 0

        for (i in 0 until categoriesJson.length()) {
            val catObj = categoriesJson.getJSONObject(i)
            val catName = catObj.optString("name", "Restored Album")
            val catSection = catObj.optString("section", "Restored")
            val coverColor = catObj.optLong("coverColor", 0xFF35354A)
            val isFavorite = catObj.optBoolean("isFavorite", false)

            val categoryId = categoryDao.insertCategory(
                CategoryEntity(
                    name = catName,
                    section = catSection,
                    coverColor = coverColor,
                    isFavorite = isFavorite
                )
            )
            restoredCategories++

            val eventsJson = catObj.optJSONArray("events")
            if (eventsJson != null) {
                for (j in 0 until eventsJson.length()) {
                    val eventObj = eventsJson.getJSONObject(j)
                    val title = eventObj.optString("title", "Event")
                    val date = eventObj.optString("date", "")
                    val location = eventObj.optString("location", "")
                    val note = eventObj.optString("note", "")
                    val placeTag = eventObj.optString("placeTag", "")
                    val personTag = eventObj.optString("personTag", "")

                    val eventId = eventDao.insertEvent(
                        EventEntity(
                            categoryId = categoryId,
                            title = title,
                            date = date,
                            location = location,
                            note = note,
                            placeTag = placeTag,
                            personTag = personTag
                        )
                    )
                    restoredEvents++
                    val categoryHadPhotos =
                        photoDao.getPhotoCountForCategorySync(categoryId) > 0
                    var firstImportedPhotoId: Long? = null

                    val photosJson = eventObj.optJSONArray("photos")
                    if (photosJson != null) {
                        for (k in 0 until photosJson.length()) {
                            val photoObj = photosJson.getJSONObject(k)
                            val contentUri = photoObj.optString("contentUri")
                            val contentHash = photoObj.optString("contentHash")
                            val takenAt = photoObj.optLong("takenAt", System.currentTimeMillis())
                            val photoFav = photoObj.optBoolean("isFavorite", false)

                            if (contentUri.isNotEmpty()) {
                                val existing = photoDao.getPhotoByUri(contentUri)
                                val photoId = if (existing == null) {
                                    photoDao.insertPhoto(
                                        PhotoRefEntity(
                                            contentUri = contentUri,
                                            contentHash = contentHash,
                                            takenAt = takenAt,
                                            isFavorite = photoFav
                                        )
                                    )
                                } else {
                                    existing.id
                                }
                                val insertion = photoDao.insertEventPhoto(
                                    EventPhotoEntity(
                                        eventId = eventId,
                                        photoId = photoId,
                                        position = photoDao.getNextPositionForEventSync(eventId)
                                    )
                                )
                                if (insertion != -1L && firstImportedPhotoId == null) {
                                    firstImportedPhotoId = photoId
                                }
                            }
                        }
                    }
                    firstImportedPhotoId?.let { firstPhotoId ->
                        eventDao.getEventByIdSync(eventId)?.let { event ->
                            eventDao.updateEvent(
                                event.copy(coverPhotoId = firstPhotoId, coverPhotoIsManual = false)
                            )
                        }
                        if (!categoryHadPhotos) {
                            categoryDao.getCategoryByIdSync(categoryId)?.let { category ->
                                if (category.coverPhotoId == null) {
                                    categoryDao.updateCategory(
                                        category.copy(
                                            coverPhotoId = firstPhotoId,
                                            coverPhotoIsManual = false
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Pair(restoredCategories, restoredEvents)
    }

    override suspend fun getSuggestedEvents(context: Context): List<SuggestedEvent> =
        withContext(Dispatchers.IO) {
            val taggedUris = photoDao.getAllTaggedPhotoUris().toSet()
            val suggested = MediaStoreHelper.getSuggestedEvents(context, taggedUris)
            suggested.map { item ->
                SuggestedEvent(
                    dateString = item.dateString,
                    photoCount = item.photos.size,
                    sampleUris = item.photos.take(4).map { it.contentUri },
                    photoUris = item.photos.map { it.contentUri }
                )
            }
        }

    override suspend fun createEventFromSuggested(
        categoryId: Long,
        title: String,
        date: String,
        photoUris: List<String>
    ): Long = withContext(Dispatchers.IO) {
        val targetCategoryId = if (categoryId > 0) {
            categoryId
        } else {
            val firstCategory = categoryDao.getAllCategoriesSync().firstOrNull()
            firstCategory?.id ?: categoryDao.insertCategory(
                CategoryEntity(
                    name = "My Events",
                    section = "Recently",
                    coverColor = 0xFF35354A
                )
            )
        }

        val newEventId = eventDao.insertEvent(
            EventEntity(
                categoryId = targetCategoryId,
                title = title,
                date = date,
                location = ""
            )
        )

        addPhotosToEvent(newEventId, photoUris)
        newEventId
    }

    override suspend fun cleanupMissingPhotos(context: Context): Int = withContext(Dispatchers.IO) {
        val allPhotos = photoDao.getAllPhotosSync()
        var removedCount = 0
        allPhotos.forEach { photo ->
            val accessible = MediaStoreHelper.isUriAccessible(context, photo.contentUri)
            if (!accessible) {
                var reResolvedUri: String? = null
                if (photo.contentHash.isNotBlank()) {
                    reResolvedUri = MediaStoreHelper.reResolvePhotoByHash(context, photo.contentHash)
                }
                if (reResolvedUri != null) {
                    photoDao.updatePhoto(photo.copy(contentUri = reResolvedUri))
                } else {
                    val affectedEvents = eventDao.getAllEventsSync()
                        .filter { it.coverPhotoId == photo.id }
                    val affectedCategories = categoryDao.getAllCategoriesSync()
                        .filter { it.coverPhotoId == photo.id }
                    photoDao.deletePhotoById(photo.id)
                    affectedEvents.forEach { event ->
                        val fallback = photoDao.getPhotosForEventSync(event.id).firstOrNull()
                        eventDao.updateEvent(
                            event.copy(coverPhotoId = fallback?.id, coverPhotoIsManual = false)
                        )
                    }
                    affectedCategories.forEach { category ->
                        val fallback = photoDao.getEarliestPhotoForCategorySync(category.id)
                        categoryDao.updateCategory(
                            category.copy(coverPhotoId = fallback?.id, coverPhotoIsManual = false)
                        )
                    }
                    removedCount++
                }
            }
        }
        removedCount
    }
}
