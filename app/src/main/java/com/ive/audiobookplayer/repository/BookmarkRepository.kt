package com.ive.audiobookplayer.repository

import android.content.Context
import com.ive.audiobookplayer.database.AppDatabase
import com.ive.audiobookplayer.database.AudiobookEntity
import com.ive.audiobookplayer.database.BookmarkEntity
import com.ive.audiobookplayer.model.AppBackup
import com.ive.audiobookplayer.model.BookmarkBackup
import com.ive.audiobookplayer.model.BookmarkEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepository(context: Context) {

    private val bookmarkDao = AppDatabase.getInstance(context).bookmarkDao()
    private val audiobookDao = AppDatabase.getInstance(context).audiobookDao()

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

    suspend fun getAllBookmarksList(): List<BookmarkEntity> = bookmarkDao.getAllBookmarksList()

//    suspend fun exportBackup(): AppBackup {
//        val allBookmarks = bookmarkDao.getAllBookmarksList()
//        val allAudiobooks = audiobookDao.getAllAudiobooksList()
//
//        return AppBackup(
//            bookmarks = allBookmarks.map { bookmark ->
//                val title = allAudiobooks.find { it.id == bookmark.audiobookId }?.title ?: ""
//                BookmarkBackup(
//                    bookTitle = title,
//                    audiobookId = bookmark.audiobookId,
//                    chapterIndex = bookmark.chapterIndex,
//                    positionMS = bookmark.positionMs,
//                    playbackSpeed = bookmark.playbackSpeed,
//                    lastPlayedAt = bookmark.lastPlayedAt,
//                )
//            },
//            version = TODO(),
//            exportDate = TODO(),
//            notes = TODO()
//        )
//    }

    suspend fun importBackup(backup: AppBackup, currentAudiobooks: List<AudiobookEntity>) {
        backup.bookmarks.forEach { bookmarkBackup ->
            val matchedId = when {
                currentAudiobooks.any { it.id == bookmarkBackup.audiobookId } ->
                    bookmarkBackup.audiobookId
                else ->
                    currentAudiobooks.find { it.title == bookmarkBackup.bookTitle }?.id
            } ?: bookmarkBackup.audiobookId

            bookmarkDao.saveBookmark(
                BookmarkEntity(
                    audiobookId = matchedId,
                    chapterIndex = bookmarkBackup.chapterIndex,
                    positionMs = bookmarkBackup.positionMS,
                    playbackSpeed = bookmarkBackup.playbackSpeed,
                    lastPlayedAt = bookmarkBackup.lastPlayedAt,
                )
            )
        }
    }

    suspend fun deleteBookmark(audiobookId: String) = bookmarkDao.deleteBookmark(audiobookId)
}