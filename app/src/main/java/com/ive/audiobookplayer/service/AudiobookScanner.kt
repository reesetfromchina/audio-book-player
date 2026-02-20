package com.ive.audiobookplayer.service

import android.app.appsearch.observer.DocumentChangeInfo
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.Chapter


class AudiobookScanner(private val context: Context) {

    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a","m4b", "aac", "ogg", "flac", "wav", "opus")

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    private fun findFolderCover(rootUri: Uri, folderDocId: String): String? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, folderDocId)
        var firstImageUri: String? = null

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
                val ext = name.substringAfterLast(".", "").lowercase()

                if (ext in IMAGE_EXTENSIONS) {
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, docId).toString()
                    val nameWithoutExt = name.substringBeforeLast(".").lowercase()
                    if (nameWithoutExt in setOf("cover", "folder", "front", "artwork")) {
                        return fileUri
                    }
                    if (firstImageUri == null) firstImageUri = fileUri
                }
            }
        }
        Log.d("Cover", "findFolderCover result: $firstImageUri")
        return firstImageUri
    }

    private fun buildAudiobook(
        rootUri: Uri,
        folderName: String,
        folderDocId: String,
        files: List<Pair<String, String>>
    ): Audiobook {
        val chapters = files.mapIndexed { index, (docId, name) ->
            val uri = DocumentsContract.buildDocumentUriUsingTree(rootUri, docId)
            Chapter(
                uri = uri.toString(),
                title = name.substringBeforeLast("."),
                index = index
            )
        }
        val id = "${folderName}_${folderDocId.hashCode()}"
        val coverUri = findFolderCover(rootUri, folderDocId)
        return Audiobook(id = id, title = folderName, chapters = chapters, coverUri = coverUri)
    }

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
                    val files = queryAudioFilesRaw(rootUri, docId)
                    if (files.isNotEmpty()) {
                        audiobooks.add(buildAudiobook(rootUri, name, docId, files))
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

    private fun queryAudioFilesRaw(rootUri: Uri, folderDocId: String): List<Pair<String, String>> {
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

    }

    private fun isAudioFile(name: String): Boolean {
        return name.substringAfterLast(".", "").lowercase() in AUDIO_EXTENSIONS
    }
}