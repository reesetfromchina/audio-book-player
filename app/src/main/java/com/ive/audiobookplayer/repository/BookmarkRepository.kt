package com.ive.audiobookplayer.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ive.audiobookplayer.model.Audiobook
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
    private val LIBRARY_CACHE_KEY = stringPreferencesKey("library_cache")
    private val SPEED_KEY_PREFIX = "speed_"
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

    suspend fun saveBookmark(bookmarkEntry: BookmarkEntry) {
        val key = stringPreferencesKey("bookmark_${bookmarkEntry.audiobookId}")
        Log.d(
            "Bookmark",
            "saveBookmark id=${bookmarkEntry.audiobookId} chapter=${bookmarkEntry.chapterIndex} pos=${bookmarkEntry.positionMs}"
        )
        context.bookmarkDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(bookmarkEntry)
            prefs[LAST_BOOK_ID_KEY] = bookmarkEntry.audiobookId
        }
    }

    suspend fun saveLibraryCache(audiobooks: List<Audiobook>) {
        context.bookmarkDataStore.edit { prefs ->
            prefs[LIBRARY_CACHE_KEY] = json.encodeToString(audiobooks)
        }
    }

    suspend fun loadLibraryCache(): List<Audiobook> {
        return runCatching<List<Audiobook>> {
            val value: String? = context.bookmarkDataStore.data.first()[LIBRARY_CACHE_KEY]
            value?.let { json.decodeFromString(it) } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    suspend fun saveSpeed(audiobookId: String, speed: Float) {
        val key = stringPreferencesKey("$SPEED_KEY_PREFIX${audiobookId.hashCode()}")
        context.bookmarkDataStore.edit { prefs ->
            prefs[key] = speed.toString()
        }
    }

    suspend fun getSpeed(audiobookId: String): Float {
        val key = stringPreferencesKey("$SPEED_KEY_PREFIX${audiobookId.hashCode()}")
        return context.bookmarkDataStore.data.first()[key]?.toFloat() ?: 1f
    }

    suspend fun getLastBookId(): String? {
        return context.bookmarkDataStore.data.first()[LAST_BOOK_ID_KEY]
    }


}
