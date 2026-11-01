package com.example.red_panda.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.red_panda.data.repository.GalleryRepository
import com.example.red_panda.data.repository.SettingsRepository
import com.example.red_panda.data.repository.ThemeSetting
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val galleryRepository: GalleryRepository? = null
) : ViewModel() {

    val themeSetting: StateFlow<ThemeSetting> = settingsRepository.themeSetting
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeSetting.SYSTEM
        )

    fun setThemeSetting(setting: ThemeSetting) {
        viewModelScope.launch {
            settingsRepository.setThemeSetting(setting)
        }
    }

    fun cleanupMissingPhotos(context: Context, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val removedCount = galleryRepository?.cleanupMissingPhotos(context) ?: 0
            onComplete(removedCount)
        }
    }

    fun exportBackup(context: Context, uri: android.net.Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = galleryRepository?.exportBackupJson()
                    ?: return@launch onResult(false, "Repository unavailable")
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
                onResult(true, "Organization data exported successfully")
            } catch (e: Exception) {
                onResult(false, "Export failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun importBackup(context: Context, uri: android.net.Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                } ?: return@launch onResult(false, "Could not read backup file")

                val result = galleryRepository?.importBackupJson(json)
                    ?: return@launch onResult(false, "Repository unavailable")
                onResult(true, "Restored ${result.first} album(s) and ${result.second} event(s)")
            } catch (e: Exception) {
                onResult(false, "Restore failed: ${e.localizedMessage ?: "Invalid backup file"}")
            }
        }
    }
}

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val galleryRepository: GalleryRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(settingsRepository, galleryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
