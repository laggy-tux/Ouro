package com.example.red_panda.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")
internal val DISMISSED_SUGGESTION_IDS = stringSetPreferencesKey("dismissed_suggestion_ids")

enum class ThemeSetting {
    SYSTEM, LIGHT, DARK
}

interface SettingsRepository {
    val themeSetting: Flow<ThemeSetting>
    val albumGridTwoColumn: Flow<Boolean>
    suspend fun setThemeSetting(setting: ThemeSetting)
    suspend fun setAlbumGridTwoColumn(enabled: Boolean)
}

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    private object PreferencesKeys {
        val THEME_SETTING = stringPreferencesKey("theme_setting")
        val ALBUM_GRID_TWO_COLUMN = booleanPreferencesKey("album_grid_two_column")
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
}
