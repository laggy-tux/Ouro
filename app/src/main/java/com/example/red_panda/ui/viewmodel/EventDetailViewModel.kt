package com.example.red_panda.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.red_panda.data.repository.GalleryRepository
import com.example.red_panda.ui.model.EventMock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class EventDetailViewModel(
    private val repository: GalleryRepository,
    private val eventId: Long
) : ViewModel() {

    val event: StateFlow<EventMock?> = repository.getEvent(eventId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    private val _coverFallbackEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val coverFallbackEvents = _coverFallbackEvents.asSharedFlow()

    fun addPhotos(uris: List<String>) {
        viewModelScope.launch {
            repository.addPhotosToEvent(eventId, uris)
        }
    }

    fun removePhoto(photoId: Long) {
        viewModelScope.launch {
            if (repository.removePhotoFromEvent(eventId, photoId)) {
                _coverFallbackEvents.emit(Unit)
            }
        }
    }

    fun togglePhotoFavorite(photoId: Long) {
        viewModelScope.launch {
            repository.togglePhotoFavorite(photoId)
        }
    }

    fun deleteEvent(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteEvent(eventId)
            onDeleted()
        }
    }
}

class EventDetailViewModelFactory(
    private val repository: GalleryRepository,
    private val eventId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EventDetailViewModel(repository, eventId) as T
    }
}
