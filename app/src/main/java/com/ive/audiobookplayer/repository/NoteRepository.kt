package com.ive.audiobookplayer.repository

import android.content.Context
import com.ive.audiobookplayer.database.AppDatabase
import com.ive.audiobookplayer.database.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(context: Context) {
    private val noteDao = AppDatabase.getInstance(context).noteDao()

    fun getNotesForBook(audiobookId: String): Flow<List<NoteEntity>> =
        noteDao.getNotesForBook(audiobookId)

    suspend fun addNote(
        audiobookId: String,
        chapterIndex: Int,
        positionMs: Long,
        noteText: String
    ) {
        noteDao.insertNote(
            NoteEntity(
                audiobookId = audiobookId,
                chapterIndex = chapterIndex,
                positionMs = positionMs,
                noteText = noteText
            )
        )
    }

    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun deleteAllNotesForBook(audiobookId: String) =
        noteDao.deleteAllNotesForBook(audiobookId)
}