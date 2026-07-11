package com.ive.audiobookplayer.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ive.audiobookplayer.database.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao{
    @Query("SELECT * FROM notes WHERE audiobookId = :audiobookId ORDER BY positionMs ASC")
    fun getNotesForBook(audiobookId: String) : Flow<List<NoteEntity>>

    @Insert
    suspend fun insertNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes where audiobookId = :audiobookId")
    suspend fun deleteAllNotesForBook(audiobookId: String)

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesList() : List<NoteEntity>

    @Query(
        """
            SELECT COUNT(*) FROM notes
            WHERE audiobookId = :audiobookId
            AND chapterIndex = :chapterIndex
            AND positionMs = :positionMs
            AND noteText = :noteText
        """
    )
    suspend fun countMatching(
        audiobookId: String,
        chapterIndex: Int,
        positionMs: Long,
        noteText: String,
    ) : Int
}