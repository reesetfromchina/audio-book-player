package com.ive.audiobookplayer.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ive.audiobookplayer.model.BookmarkEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.bookmarkDataStore by preferencesDataStore(name = "bookmarks")
class BookmarkRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val LAST_BOOK_ID_KEY = stringPreferencesKey("last_book_id")
    private val ROOT_FOLDER_URI_KEY = stringPreferencesKey("root_folder_uri")

    val bookmarks: Flow<Map<String, BookmarkEntry>> =
        context.bookmarkDataStore.data.map { prefs ->
            prefs.asMap()
                .filterKeys { it.name.startsWith("bookmark_") }
                .mapNotNull { (key, value) ->
                    runCatching {
                        val entry = json.decodeFromString<BookmarkEntry>(value as String)
                        entry.audiobookId to entry
                    }.getOrNull()
                }
                .toMap()
        }

    val lastBookId: Flow<String?> = context.bookmarkDataStore.data.map { prefs ->
        prefs[LAST_BOOK_ID_KEY]
    }

    val rootFolderUri: Flow<String?> = context.bookmarkDataStore.data.map { prefs ->
        prefs[ROOT_FOLDER_URI_KEY]
    }

    suspend fun saveRootFolderUri(uri: Uri) {
        context.bookmarkDataStore.edit { prefs ->
            prefs[ROOT_FOLDER_URI_KEY] = uri.toString()
        }
    }

    suspend fun getBookmark(audiobookId: String): BookmarkEntry? {
        val key = stringPreferencesKey("bookmark_$audiobookId")
        return runCatching {
            val value: String? = context.bookmarkDataStore.data.first()[key]
            Log.d("Bookmark", "getBookmark for id=$audiobookId, found=$value")
            value?.let { json.decodeFromString<BookmarkEntry>(it) }
        }.getOrNull()
    }

    suspend fun saveBookmark(entry: BookmarkEntry) {
        val key = stringPreferencesKey("bookmark_${entry.audiobookId}")
        Log.d("Bookmark", "saveBookmark id=${entry.audiobookId} chapter=${entry.chapterIndex} pos=${entry.positionMs}")
        context.bookmarkDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(entry)
            prefs[LAST_BOOK_ID_KEY] = entry.audiobookId
        }
    }

    suspend fun clearRootFolderUri() {
        context.bookmarkDataStore.edit { prefs ->
            prefs.remove(ROOT_FOLDER_URI_KEY)
        }
    }


    fun getBookmarkFlow(audiobookId: String): Flow<BookmarkEntry?> =
        context.bookmarkDataStore.data.map { prefs ->
            val key = stringPreferencesKey("bookmark_$audiobookId")
            prefs[key]?.let {
                runCatching { json.decodeFromString<BookmarkEntry>(it) }.getOrNull()
            }
        }
}