package com.ive.audiobookplayer.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ive.audiobookplayer.database.AppDatabase
import com.ive.audiobookplayer.database.BookmarkEntity
import com.ive.audiobookplayer.model.BookmarkEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class BookmarkRepository(context: Context) {

    private val bookmarkDao = AppDatabase.getInstance(context).bookmarkDao()

    val bookmarks: Flow<Map<String, BookmarkEntry>> =
        bookmarkDao.getAllBookmarks().map { list ->
            list.associate { entity ->
                entity.audiobookId to BookmarkEntry(
                    audiobookId = entity.audiobookId,
                    chapterIndex = entity.chapterIndex,
                    positionMs = entity.positionMs,
                    lastPlayedAt = entity.lastPlayedAt
                )
            }
        }

    suspend fun getBookmark(audiobookId: String): BookmarkEntity? =
        bookmarkDao.getBookmark(audiobookId)

    suspend fun saveBookmark(
        audiobookId: String,
        chapterIndex: Int,
        positionMs: Long,
        playbackSpeed: Float
    ) {
        bookmarkDao.saveBookmark(
            BookmarkEntity(
                audiobookId = audiobookId,
                chapterIndex = chapterIndex,
                positionMs = positionMs,
                playbackSpeed = playbackSpeed,
            )
        )
    }
}