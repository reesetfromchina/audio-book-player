package com.ive.audiobookplayer.repository
import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val ROOT_FOLDER_URI_KEY = stringPreferencesKey("root_folder_uri")
    private val LAST_BOOK_ID_KEY = stringPreferencesKey("last_book_id")

    val rootFolderUri: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[ROOT_FOLDER_URI_KEY]
    }

    suspend fun saveRootFolderUri(uri: Uri) {
        context.settingsDataStore.edit { prefs ->
            prefs[ROOT_FOLDER_URI_KEY] = uri.toString()
        }
    }

    suspend fun getLastBookId(): String? =
        context.settingsDataStore.data.first()[LAST_BOOK_ID_KEY]

    suspend fun saveLastBookId(id: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[LAST_BOOK_ID_KEY] = id
        }
    }
}