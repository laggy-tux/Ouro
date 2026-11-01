package com.example.red_panda.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.red_panda.data.repository.GalleryRepository
import com.example.red_panda.ui.model.Album
import com.example.red_panda.ui.model.SuggestedEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _suggestedEvents = MutableStateFlow<List<SuggestedEvent>>(emptyList())
    val suggestedEvents: StateFlow<List<SuggestedEvent>> = _suggestedEvents.asStateFlow()

    val albums: StateFlow<List<Album>> = repository.getAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadSuggestedEvents(context: android.content.Context) {
        viewModelScope.launch {
            val list = repository.getSuggestedEvents(context)
            _suggestedEvents.value = list
        }
    }

    fun createEventFromSuggested(
        context: android.content.Context,
        categoryId: Long,
        title: String,
        date: String,
        photoUris: List<String>
    ) {
        viewModelScope.launch {
            repository.createEventFromSuggested(categoryId, title, date, photoUris)
            loadSuggestedEvents(context)
        }
    }

    fun toggleFavorite(albumId: String) {
        viewModelScope.launch {
            val id = albumId.toLongOrNull() ?: return@launch
            repository.toggleCategoryFavorite(id)
        }
    }

    fun createCategory(name: String, section: String, coverColor: Long) {
        viewModelScope.launch {
            repository.createCategory(name, section, coverColor)
        }
    }

    fun updateCategory(albumId: String, name: String, section: String, coverColor: Long) {
        viewModelScope.launch {
            val id = albumId.toLongOrNull() ?: return@launch
            repository.updateCategory(id, name, section, coverColor)
        }
    }

    fun deleteCategory(albumId: String) {
        viewModelScope.launch {
            val id = albumId.toLongOrNull() ?: return@launch
            repository.deleteCategory(id)
        }
    }
}
