package com.example.red_panda.ui.model

import com.example.red_panda.data.local.entity.PhotoRefEntity

data class EventMock(
    val id: String,
    val title: String,
    val date: String,
    val location: String,
    val totalPhotos: Int,
    val thumbnailColors: List<Long>,
    val remainingPhotosCount: Int,
    val placeTag: String = "All Places",
    val personTag: String = "All People",
    val note: String = "",
    val photoUris: List<String> = emptyList(),
    val photoRefs: List<PhotoRefEntity> = emptyList()
)

data class SuggestedEvent(
    val dateString: String,
    val photoCount: Int,
    val sampleUris: List<String>,
    val photoUris: List<String>
)

data class Album(
    val id: String,
    val title: String,
    val photoCount: Int,
    val section: String, // "Recently" or "Other Albums"
    val isFavorite: Boolean = false,
    val coverColor: Long = 0xFF2C2C30,
    val coverPhotoUri: String? = null,
    val coverPhotoId: Long? = null,
    val events: List<EventMock> = emptyList()
)
