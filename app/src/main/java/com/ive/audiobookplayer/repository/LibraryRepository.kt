package com.ive.audiobookplayer.repository

import android.content.Context
import com.ive.audiobookplayer.database.AppDatabase
import com.ive.audiobookplayer.database.AudiobookEntity
import com.ive.audiobookplayer.database.AudiobookWithChapters
import com.ive.audiobookplayer.database.ChapterEntity
import com.ive.audiobookplayer.model.Audiobook
import kotlinx.coroutines.flow.Flow

class LibraryRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val audiobookDao = db.audiobookDao()

    // flow ui auto update when data changes, just like stateflow

    val audiobooks: Flow<List<AudiobookWithChapters>> = audiobookDao.getAllAudiobooks()

    suspend fun getAllIds(): Set<String> = audiobookDao.getAllIds().toSet()

    suspend fun insertAudiobook(audiobook: Audiobook){
        audiobookDao.insertAudiobook(
            AudiobookEntity(
                id = audiobook.id,
                title = audiobook.title,
                coverUri = audiobook.coverUri
            )
        )
        audiobookDao.insertChapters(
            audiobook.chapters.map { chapter ->
                ChapterEntity(
                    id = "${audiobook.id}_${chapter.index}",
                    audiobookId = audiobook.id,
                    title = chapter.title,
                    uri = chapter.uri,
                    index = chapter.index,
                    durationMs = chapter.durationMs,
                )
            }
        )
    }

    suspend fun deleteAudiobook(id: String) = audiobookDao.deleteAudiobook(id)
    suspend fun markAsNotNew(id: String) = audiobookDao.markAsNotNew(id)
    suspend fun getAllAudiobookEntities(): List<AudiobookEntity> = audiobookDao.getAllAudiobooksList()
    suspend fun updateChapterDuration(audiobookId: String, chapterIndex: Int, durationMS : Long){
        audiobookDao.updateChapterDuration(audiobookId, chapterIndex, durationMS)
    }
}