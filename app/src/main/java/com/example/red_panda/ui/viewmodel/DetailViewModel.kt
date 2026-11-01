package com.example.red_panda.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.red_panda.data.local.entity.CategoryEntity
import com.example.red_panda.data.local.entity.PhotoRefEntity
import com.example.red_panda.data.repository.GalleryRepository
import com.example.red_panda.ui.model.EventMock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: GalleryRepository,
    private val categoryId: Long
) : ViewModel() {

    val category: StateFlow<CategoryEntity?> = repository.getCategoryById(categoryId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val coverPhotoUri: StateFlow<String?> = repository.getAlbums()
        .map { albums -> albums.find { it.id == categoryId.toString() }?.coverPhotoUri }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val events: StateFlow<List<EventMock>> = repository.getEventsForCategory(categoryId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categoryPhotos: StateFlow<List<PhotoRefEntity>> = repository.getPhotosForCategory(categoryId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleFavorite() {
        viewModelScope.launch {
            repository.toggleCategoryFavorite(categoryId)
        }
    }

    fun updateCategory(name: String, section: String, coverColor: Long) {
        viewModelScope.launch {
            repository.updateCategory(categoryId, name, section, coverColor)
        }
    }

    fun updateCategoryCover(coverPhotoId: Long?) {
        viewModelScope.launch {
            repository.updateCategoryCover(categoryId, coverPhotoId)
        }
    }

    fun setCategoryCoverFromUri(uri: String) {
        viewModelScope.launch {
            repository.setCategoryCoverFromUri(categoryId, uri)
        }
    }

    fun resetCategoryCover() {
        updateCategoryCover(null)
    }

    fun deleteCategory(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
            onDeleted()
        }
    }

    fun createEvent(
        title: String,
        date: String,
        location: String,
        note: String,
        placeTag: String,
        personTag: String
    ) {
        viewModelScope.launch {
            repository.createEvent(
                categoryId = categoryId,
                title = title,
                date = date,
                location = location,
                note = note,
                placeTag = placeTag,
                personTag = personTag
            )
        }
    }

    fun updateEvent(
        eventId: String,
        title: String,
        date: String,
        location: String,
        note: String,
        placeTag: String,
        personTag: String
    ) {
        viewModelScope.launch {
            val id = eventId.toLongOrNull() ?: return@launch
            repository.updateEvent(
                eventId = id,
                title = title,
                date = date,
                location = location,
                note = note,
                placeTag = placeTag,
                personTag = personTag
            )
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            val id = eventId.toLongOrNull() ?: return@launch
            repository.deleteEvent(id)
        }
    }
}
