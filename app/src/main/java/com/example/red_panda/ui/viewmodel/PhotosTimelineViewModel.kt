package com.example.red_panda.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import androidx.datastore.preferences.core.edit
import com.example.red_panda.data.paging.MediaStorePagingSource
import com.example.red_panda.data.paging.TimelineItem
import com.example.red_panda.data.local.MediaFolder
import com.example.red_panda.data.local.MediaStoreHelper
import com.example.red_panda.data.repository.GalleryRepository
import com.example.red_panda.data.repository.EXCLUDED_MEDIA_FOLDER_IDS
import com.example.red_panda.data.repository.dataStore
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

    // Selection Mode State
    var isSelectionMode by mutableStateOf(false)
        private set

    var selectedPhotoUris by mutableStateOf<Set<String>>(emptySet())
        private set

    var lastSelectedPhotoTakenAt by mutableStateOf<Long?>(null)
        private set

    private val selectedPhotoTakenAtByUri = mutableMapOf<String, Long>()
    private var lastSelectedPhotoUri: String? = null

    fun toggleSelectionMode() {
        isSelectionMode = !isSelectionMode
        if (!isSelectionMode) {
            selectedPhotoUris = emptySet()
            selectedPhotoTakenAtByUri.clear()
            lastSelectedPhotoUri = null
            lastSelectedPhotoTakenAt = null
        }
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedPhotoUris = emptySet()
        selectedPhotoTakenAtByUri.clear()
        lastSelectedPhotoUri = null
        lastSelectedPhotoTakenAt = null
    }

    fun togglePhotoSelection(uri: String, takenAt: Long? = null) {
        val current = selectedPhotoUris.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
            selectedPhotoTakenAtByUri.remove(uri)
            if (lastSelectedPhotoUri == uri) {
                lastSelectedPhotoUri = current.lastOrNull { it in selectedPhotoTakenAtByUri }
                lastSelectedPhotoTakenAt = lastSelectedPhotoUri?.let(selectedPhotoTakenAtByUri::get)
            }
        } else {
            current.add(uri)
            if (takenAt != null) {
                selectedPhotoTakenAtByUri[uri] = takenAt
                lastSelectedPhotoUri = uri
                lastSelectedPhotoTakenAt = takenAt
            }
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
        selectedPhotoTakenAtByUri.clear()
        lastSelectedPhotoUri = null
        lastSelectedPhotoTakenAt = null
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

    fun removeDeletedPhotoReference(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removePhotoReference(uri)
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

    private val _mediaFolders = MutableStateFlow<List<MediaFolder>>(emptyList())
    val mediaFolders: StateFlow<List<MediaFolder>> = _mediaFolders.asStateFlow()

    val excludedMediaFolderIds: StateFlow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[EXCLUDED_MEDIA_FOLDER_IDS].orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    private val _mediaFolderError = MutableStateFlow<String?>(null)
    val mediaFolderError: StateFlow<String?> = _mediaFolderError.asStateFlow()

    fun setExcludedMediaFolderIds(folderIds: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { preferences ->
                preferences[EXCLUDED_MEDIA_FOLDER_IDS] = folderIds
            }
        }
    }

    fun dismissSuggestedEvent(suggestionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.dismissSuggestedEvent(context, suggestionId)
            _suggestedEvents.value = repository.getSuggestedEvents(context)
        }
    }

    fun loadSuggestedEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getSuggestedEvents(context)
            _suggestedEvents.value = list
        }
    }

    init {
        loadSuggestedEvents()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _mediaFolders.value = MediaStoreHelper.getMediaStoreFolders(context)
                _mediaFolderError.value = null
            } catch (exception: Exception) {
                _mediaFolderError.value = exception.localizedMessage
                    ?: "Unable to load media folders."
            }
        }
    }

    // Paged Timeline Items (Flow combines taggedPhotoUris, favoritePhotoUris, and activeFilter)
    val timelineItems: Flow<PagingData<TimelineItem>> = combine(
        taggedPhotoUris,
        favoritePhotoUris,
        androidx.compose.runtime.snapshotFlow { activeFilter },
        excludedMediaFolderIds
    ) { tagged, favorites, filter, excludedFolders ->
        Triple(tagged, favorites, filter to excludedFolders)
    }.flatMapLatest { (tagged, favorites, filterAndFolders) ->
        val (filter, excludedFolders) = filterAndFolders
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
                    excludedFolderIds = excludedFolders,
                    pageSize = pageSize
                )
            }
        ).flow
            .map { pagingData ->
                pagingData
                    .map { photo ->
                        val item: TimelineItem = TimelineItem.Photo(photo)
                        item
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
                location = "",
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
    fun createEventFromSuggestedCard(
        suggested: SuggestedEvent,
        categoryId: Long,
        title: String,
        newCategoryTitle: String? = null,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetCatId = if (newCategoryTitle != null) {
                repository.createCategory(newCategoryTitle, "Recently", 0xFF35354A)
            } else {
                categoryId
            }
            repository.createEventFromSuggested(
                categoryId = targetCatId,
                title = title,
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
