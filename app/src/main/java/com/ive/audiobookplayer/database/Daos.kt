package com.ive.audiobookplayer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiobookDao{
    // get all books with their chapters in one query
    @Transaction
    @Query("SELECT * FROM audiobooks ORDER BY title ASC")
    fun getAllAudiobooks(): Flow<List<AudiobookWithChapters>>

    @Query("SELECT * FROM audiobooks WHERE id = :id")
    suspend fun getAudiobook(id: String) : AudiobookEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE) // ignore if already exists
    suspend fun insertAudiobook(audiobook: AudiobookEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("DELETE FROM audiobooks WHERE id = :id")
    suspend fun deleteAudiobook(id: String)

    @Query("SELECT id FROM audiobooks")
    suspend fun getAllIds(): List<String>

    @Query("UPDATE audiobooks SET isNew = 0 where id = :id")
    suspend fun markAsNotNew(id: String)

    @Query("SELECT * FROM audiobooks")
    suspend fun getAllAudiobooksList() : List<AudiobookEntity>

    @Query("UPDATE chapters SET durationMs = :durationMs WHERE audiobookId = :audiobookId AND `index` = :chapterIndex")
    suspend fun updateChapterDuration(audiobookId: String, chapterIndex: Int, durationMs: Long)
}

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
}