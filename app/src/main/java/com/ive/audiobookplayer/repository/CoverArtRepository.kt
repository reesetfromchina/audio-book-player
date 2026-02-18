package com.ive.audiobookplayer.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.coverDataStore by preferencesDataStore(name = "covers")

class CoverArtRepository(private val context: Context) {

    // save a user-picked image URI for a book
    suspend fun saveCoverUri(audiobookId: String, uri: Uri) {
        val key = stringPreferencesKey("cover_${audiobookId.hashCode()}")
        context.coverDataStore.edit { prefs ->
            prefs[key] = uri.toString()
        }
    }

    fun getCoverUriFlow(audiobookId: String): Flow<String?> {
        val key = stringPreferencesKey("cover_${audiobookId.hashCode()}")
        return context.coverDataStore.data.map { prefs -> prefs[key] }
    }

    suspend fun getCoverUri(audiobookId: String): String? {
        val key = stringPreferencesKey("cover_${audiobookId.hashCode()}")
        return context.coverDataStore.data.first()[key]
    }
}