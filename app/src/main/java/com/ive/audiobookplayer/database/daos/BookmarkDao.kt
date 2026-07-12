package com.ive.audiobookplayer.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ive.audiobookplayer.database.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao{

    @Query("SELECT * FROM bookmarks")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE audiobookId = :audiobookId")
    suspend fun getBookmark(audiobookId: String) : BookmarkEntity?

    @Upsert // insert or update - eqv to Spring save()
    suspend fun saveBookmark(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarks WHERE audiobookId = :audiobookId")
    suspend fun getSpeed(audiobookId: String) : BookmarkEntity?

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarksList() : List<BookmarkEntity>

    @Query("DELETE FROM bookmarks WHERE audiobookId = :audiobookId")
    suspend fun deleteBookmark(audiobookId: String)


}