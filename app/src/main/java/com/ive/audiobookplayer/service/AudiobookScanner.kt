package com.ive.audiobookplayer.service

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.Chapter


class AudiobookScanner(private val context: Context) {

    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "ogg", "flac", "wav", "opus")

    fun scanFolder(rootUri: Uri): List<Audiobook> {
        val rootDocId = DocumentsContract.getTreeDocumentId(rootUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, rootDocId)

        val audiobooks = mutableListOf<Audiobook>()
        val rootFiles = mutableListOf<Pair<String, String>>()

        // query root level in one bulk call
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0)
                val name = cursor.getString(1)
                val mime = cursor.getString(2)

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val chapters = queryAudioFiles(rootUri, docId)
                    if (chapters.isNotEmpty()) {
                        val id = "${name}_${docId.hashCode()}"
                        audiobooks.add(Audiobook(id = id, title = name, chapters = chapters))
                    }
                } else if (isAudioFile(name)) {
                    rootFiles.add(docId to name)
                }
            }
        }

        // loose audio files in root = individual audiobooks
        rootFiles.forEach { (docId, name) ->
            val uri = DocumentsContract.buildDocumentUriUsingTree(rootUri, docId)
            val title = name.substringBeforeLast(".")
            audiobooks.add(
                Audiobook(
                    id = "${title}_${docId.hashCode()}",
                    title = title,
                    chapters = listOf(Chapter(uri = uri.toString(), title = title, index = 0))
                )
            )
        }

        return audiobooks.sortedBy { it.title }
    }

    private fun queryAudioFiles(rootUri: Uri, folderDocId: String): List<Chapter> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, folderDocId)
        val files = mutableListOf<Pair<String, String>>()

        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0)
                val name = cursor.getString(1)
                if (isAudioFile(name)) {
                    files.add(docId to name)
                }
            }
        }

        return files
            .sortedBy { it.second }
            .mapIndexed { index, (docId, name) ->
                val uri = DocumentsContract.buildDocumentUriUsingTree(rootUri, docId)
                Chapter(
                    uri = uri.toString(),
                    title = name.substringBeforeLast("."),
                    index = index
                )
            }
    }

    private fun isAudioFile(name: String): Boolean {
        return name.substringAfterLast(".", "").lowercase() in AUDIO_EXTENSIONS
    }
}