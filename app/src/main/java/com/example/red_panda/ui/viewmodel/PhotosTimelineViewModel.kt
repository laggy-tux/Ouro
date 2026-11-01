package com.example.red_panda.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.example.red_panda.data.paging.MediaStorePagingSource
import com.example.red_panda.data.paging.TimelineItem
import com.example.red_panda.data.repository.GalleryRepository
import com.example.red_panda.ui.model.Album
import com.example.red_panda.ui.model.SuggestedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PhotoFilter {
    ALL,
    UNTAGGED_ONLY,
    FAVORITES_ONLY
}

@OptIn(ExperimentalCoroutinesApi::class)
class PhotosTimelineViewModel(
    private val context: Context,
    private val repository: GalleryRepository
) : ViewModel() {

    var columnCount by mutableIntStateOf(3)
        private set

    fun toggleColumnCount() {
        columnCount = if (columnCount == 3) 4 else 3
    }

    // Selection Mode State
    var isSelectionMode by mutableStateOf(false)
        private set

    var selectedPhotoUris by mutableStateOf<Set<String>>(emptySet())
        private set

    fun toggleSelectionMode() {
        isSelectionMode = !isSelectionMode
        if (!isSelectionMode) {
            selectedPhotoUris = emptySet()
        }
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedPhotoUris = emptySet()
    }

    fun togglePhotoSelection(uri: String) {
        val current = selectedPhotoUris.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        selectedPhotoUris = current
        if (selectedPhotoUris.isNotEmpty()) {
            isSelectionMode = true
        }
    }

    fun toggleDateGroupSelection(dateGroupUris: List<String>) {
        if (dateGroupUris.isEmpty()) return
        val current = selectedPhotoUris.toMutableSet()
        val allSelected = dateGroupUris.all { current.contains(it) }
        if (allSelected) {
            current.removeAll(dateGroupUris.toSet())
        } else {
            current.addAll(dateGroupUris)
        }
        selectedPhotoUris = current
        if (selectedPhotoUris.isNotEmpty()) {
            isSelectionMode = true
        }
    }

    fun selectAllPhotos(uris: List<String>) {
        val current = selectedPhotoUris.toMutableSet()
        current.addAll(uris)
        selectedPhotoUris = current
        isSelectionMode = true
    }

    fun clearSelection() {
        selectedPhotoUris = emptySet()
        isSelectionMode = false
    }

    // Filter State (ALL, UNTAGGED_ONLY, FAVORITES_ONLY)
    var activeFilter by mutableStateOf(PhotoFilter.ALL)
        private set

    fun setFilter(filter: PhotoFilter) {
        activeFilter = filter
    }

    // Legacy helper for compatibility
    val filterUntaggedOnly: Boolean
        get() = activeFilter == PhotoFilter.UNTAGGED_ONLY

    fun toggleFilterUntaggedOnly() {
        activeFilter = if (activeFilter == PhotoFilter.UNTAGGED_ONLY) PhotoFilter.ALL else PhotoFilter.UNTAGGED_ONLY
    }

    fun toggleFilterFavoritesOnly() {
        activeFilter = if (activeFilter == PhotoFilter.FAVORITES_ONLY) PhotoFilter.ALL else PhotoFilter.FAVORITES_ONLY
    }

    // Reactive Tagged Photo URIs from Room
    val taggedPhotoUris: StateFlow<Set<String>> = repository.getTaggedPhotoUris()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    // Reactive Favorite Photo URIs from Room
    val favoritePhotoUris: StateFlow<Set<String>> = repository.getFavoritePhotoUris()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    fun togglePhotoFavorite(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.togglePhotoFavoriteByUri(uri)
        }
    }

    fun batchFavoriteSelected(isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val uris = selectedPhotoUris.toList()
            repository.batchSetFavorite(uris, isFavorite)
            clearSelection()
        }
    }

    // Albums state for AddToEventDialog
    val albums: StateFlow<List<Album>> = repository.getAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Suggested Events State (Clustered in Background Coroutine)
    private val _suggestedEvents = MutableStateFlow<List<SuggestedEvent>>(emptyList())
    val suggestedEvents: StateFlow<List<SuggestedEvent>> = _suggestedEvents.asStateFlow()

    var isSuggestedEventsDismissed by mutableStateOf(false)
        private set

    fun dismissSuggestedEvents() {
        isSuggestedEventsDismissed = true
    }

    fun loadSuggestedEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getSuggestedEvents(context)
            _suggestedEvents.value = list
        }
    }

    init {
        loadSuggestedEvents()
    }

    // Paged Timeline Items (Flow combines taggedPhotoUris, favoritePhotoUris, and activeFilter)
    val timelineItems: Flow<PagingData<TimelineItem>> = combine(
        taggedPhotoUris,
        favoritePhotoUris,
        androidx.compose.runtime.snapshotFlow { activeFilter }
    ) { tagged, favorites, filter ->
        Triple(tagged, favorites, filter)
    }.flatMapLatest { (tagged, favorites, filter) ->
        val pageSize = 60
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                initialLoadSize = 120
            ),
            pagingSourceFactory = {
                MediaStorePagingSource(
                    context = context.applicationContext,
                    taggedPhotoUris = tagged,
                    filterUntaggedOnly = filter == PhotoFilter.UNTAGGED_ONLY,
                    favoritePhotoUris = favorites,
                    filterFavoritesOnly = filter == PhotoFilter.FAVORITES_ONLY,
                    pageSize = pageSize
                )
            }
        ).flow
            .map { pagingData ->
                pagingData
                    .map { photo -> TimelineItem.Photo(photo) }
                    .insertSeparators { before: TimelineItem.Photo?, after: TimelineItem.Photo? ->
                        if (before == null && after != null) {
                            TimelineItem.Header(MediaStorePagingSource.formatDateHeader(after.photo.takenAt))
                        } else if (before != null && after != null) {
                            val beforeHeader = MediaStorePagingSource.formatDateHeader(before.photo.takenAt)
                            val afterHeader = MediaStorePagingSource.formatDateHeader(after.photo.takenAt)
                            if (beforeHeader != afterHeader) {
                                TimelineItem.Header(afterHeader)
                            } else null
                        } else {
                            null
                        }
                    }
            }
    }.cachedIn(viewModelScope)

    // Add selected photos to existing event
    fun addSelectedToEvent(eventId: Long, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val uris = selectedPhotoUris.toList()
            repository.addPhotosToEvent(eventId, uris)
            clearSelection()
            loadSuggestedEvents()
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // Create new event and add selected photos
    fun createEventAndAddSelected(
        categoryId: Long,
        title: String,
        date: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val uris = selectedPhotoUris.toList()
            val newEventId = repository.createEvent(
                categoryId = categoryId,
                title = title,
                date = date,
                location = "Local Gallery",
                note = "",
                placeTag = "",
                personTag = ""
            )
            repository.addPhotosToEvent(newEventId, uris)
            clearSelection()
            loadSuggestedEvents()
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // Create event directly from suggested event card
    fun createEventFromSuggestedCard(suggested: SuggestedEvent, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createEventFromSuggested(
                categoryId = -1L,
                title = "Event on ${suggested.dateString}",
                date = suggested.dateString,
                photoUris = suggested.photoUris
            )
            loadSuggestedEvents()
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}

class PhotosTimelineViewModelFactory(
    private val context: Context,
    private val repository: GalleryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotosTimelineViewModel::class.java)) {
            return PhotosTimelineViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
