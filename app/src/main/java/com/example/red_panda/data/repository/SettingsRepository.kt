package com.example.red_panda.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")
internal val DISMISSED_SUGGESTION_IDS = stringSetPreferencesKey("dismissed_suggestion_ids")
internal val EXCLUDED_MEDIA_FOLDER_IDS = stringSetPreferencesKey("excluded_media_folder_ids")

enum class ThemeSetting {
    SYSTEM, LIGHT, DARK
}

interface SettingsRepository {
    val themeSetting: Flow<ThemeSetting>
    val albumGridTwoColumn: Flow<Boolean>
    val albumGridColumnCount: Flow<Int>
    suspend fun setThemeSetting(setting: ThemeSetting)
    suspend fun setAlbumGridTwoColumn(enabled: Boolean)
    suspend fun setAlbumGridColumnCount(count: Int)
}

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    private object PreferencesKeys {
        val THEME_SETTING = stringPreferencesKey("theme_setting")
        val ALBUM_GRID_TWO_COLUMN = booleanPreferencesKey("album_grid_two_column")
        val ALBUM_GRID_COLUMN_COUNT = intPreferencesKey("album_grid_column_count")
    }

    override val themeSetting: Flow<ThemeSetting> = context.dataStore.data.map { preferences ->
        val themeName = preferences[PreferencesKeys.THEME_SETTING] ?: ThemeSetting.SYSTEM.name
        try {
            ThemeSetting.valueOf(themeName)
        } catch (e: Exception) {
            ThemeSetting.SYSTEM
        }
    }

    override val albumGridTwoColumn: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.ALBUM_GRID_TWO_COLUMN] ?: true
        }

    override val albumGridColumnCount: Flow<Int> =
        context.dataStore.data.map { preferences ->
            (preferences[PreferencesKeys.ALBUM_GRID_COLUMN_COUNT] ?: MIN_ALBUM_GRID_COLUMNS)
                .coerceIn(MIN_ALBUM_GRID_COLUMNS, MAX_ALBUM_GRID_COLUMNS)
        }

    override suspend fun setThemeSetting(setting: ThemeSetting) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_SETTING] = setting.name
        }
    }

    override suspend fun setAlbumGridTwoColumn(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALBUM_GRID_TWO_COLUMN] = enabled
        }
    }

    override suspend fun setAlbumGridColumnCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALBUM_GRID_COLUMN_COUNT] =
                count.coerceIn(MIN_ALBUM_GRID_COLUMNS, MAX_ALBUM_GRID_COLUMNS)
        }
    }

    private companion object {
        const val MIN_ALBUM_GRID_COLUMNS = 3
        const val MAX_ALBUM_GRID_COLUMNS = 6
    }
}
