package com.ive.audiobookplayer.service

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import com.ive.audiobookplayer.model.AppBackup
import com.ive.audiobookplayer.model.BookmarkBackup
import com.ive.audiobookplayer.model.NoteBackup
import com.ive.audiobookplayer.repository.BookmarkRepository
import com.ive.audiobookplayer.repository.CoverArtRepository
import com.ive.audiobookplayer.repository.LibraryRepository
import com.ive.audiobookplayer.repository.NoteRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class BackupManager(
    private val bookmarkRepository: BookmarkRepository,
    private val libraryRepository: LibraryRepository,
    private val noteRepository: NoteRepository,
    private val coverArtRepository: CoverArtRepository,
    private val contentResolver: ContentResolver
) {
    suspend fun exportBackup(uri: Uri) {
        val allBookmarks = bookmarkRepository.getAllBookmarksList()
        val allAudiobooks = libraryRepository.getAllAudiobookEntities()
        val allNotes = noteRepository.getAllNotes()

        val bookmarkBackup = allBookmarks.map { b ->
            val title = allAudiobooks.find { it.id == b.audiobookId }?.title ?: ""

            // resolve custom cover
            val coverBase64 = try {
                val customUri = coverArtRepository.getCoverUri(b.audiobookId)
                if (customUri != null) {
                    contentResolver.openInputStream(customUri.toUri())?.use { stream ->
                            Base64.encodeToString(
                                stream.readBytes(), Base64.DEFAULT
                            )
                        }
                } else null
            } catch (e: Exception) {
                null
            }
            BookmarkBackup(
                bookTitle = title,
                audiobookId = b.audiobookId,
                chapterIndex = b.chapterIndex,
                positionMS = b.positionMs,
                playbackSpeed = b.playbackSpeed,
                lastPlayedAt = b.lastPlayedAt,
                customCoverBase64 = coverBase64,
            )
        }

        val notesBackup = allNotes.map { note ->
            NoteBackup(
                audiobookId = note.audiobookId,
                chapterIndex = note.chapterIndex,
                positionMS = note.positionMs,
                noteText = note.noteText
            )
        }
        val backup = AppBackup(bookmarks = bookmarkBackup, notes = notesBackup)
        val json = Json.encodeToString(backup)
        contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(json.toByteArray())
        }
    }

    suspend fun importBackup(uri: Uri, filesDir: File): List<String> {
        val json = contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            ?: return emptyList()
        val backup = Json.decodeFromString<AppBackup>(json)
        val currentAudiobooks = libraryRepository.getAllAudiobookEntities()
        // restore bookmarks and notes
        bookmarkRepository.importBackup(backup, currentAudiobooks)

        val restoredCoverIds = mutableListOf<String>()

        // restore custom covers separately using coverArtRepository
        backup.bookmarks.forEach { bookmarkBackup ->
            bookmarkBackup.customCoverBase64?.let { base64 ->
                try {
                    val matchedId =
                        currentAudiobooks.find { it.id == bookmarkBackup.audiobookId || it.title == bookmarkBackup.bookTitle }?.id
                            ?: bookmarkBackup.audiobookId

                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val coverFile = File(
                        filesDir, "custom_covers/${matchedId}.jpg"
                    )
                    coverFile.parentFile?.mkdirs()
                    coverFile.writeBytes(bytes)

                    // use existing saveCoverUri
                    coverArtRepository.saveCoverUri(matchedId, coverFile.toUri())
                    restoredCoverIds.add(matchedId)
                } catch (e: Exception) {
                    Log.e(
                        "Import", "Failed to restore cover for ${bookmarkBackup.bookTitle}", e
                    )
                }
            }
        }

        return restoredCoverIds
    }
}