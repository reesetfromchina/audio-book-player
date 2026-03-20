package com.ive.audiobookplayer.service

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.Chapter


class AudiobookScanner(private val context: Context) {

    companion object {
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "m4b", "aac", "ogg", "flac", "wav", "opus")
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
        private val COVER_NAMES = setOf("cover", "folder", "front", "artwork")
    }

    // single data class to hold everything found in a folder scan
    private data class FolderContents(
        val audioFiles: List<Pair<String, String>>,  // docId to name
        val coverUri: String?
    )

    // scan folder contents in ONE query instead of two separate ones
    private fun scanFolderContents(rootUri: Uri, folderDocId: String): FolderContents {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, folderDocId)
        val audioFiles = mutableListOf<Pair<String, String>>()
        var priorityCover: String? = null
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
                val docId = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: continue
                val ext = name.substringAfterLast(".", "").lowercase()

                when {
                    ext in AUDIO_EXTENSIONS -> audioFiles.add(docId to name)
                    ext in IMAGE_EXTENSIONS -> {
                        val uri = DocumentsContract.buildDocumentUriUsingTree(rootUri, docId).toString()
                        val nameWithoutExt = name.substringBeforeLast(".").lowercase()
                        if (nameWithoutExt in COVER_NAMES) {
                            priorityCover = uri
                        } else if (firstImageUri == null) {
                            firstImageUri = uri
                        }
                    }
                }
            }
        }

        return FolderContents(
            audioFiles = audioFiles.sortedBy { it.second },
            coverUri = priorityCover ?: firstImageUri
        )
    }

    private fun getAudioDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0L
            pfd.use {
                retriever.setDataSource(it.fileDescriptor)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            }
        } catch (e: Exception) {
            Log.e("Scanner", "Failed to get duration for $uri", e)
            0L
        } finally {
            retriever.release()
        }
    }

    private fun buildChapter(rootUri: Uri, docId: String, name: String, index: Int): Chapter {
        val uri = DocumentsContract.buildDocumentUriUsingTree(rootUri, docId)
        return Chapter(
            uri = uri.toString(),
            title = name.substringBeforeLast("."),
            index = index,
            durationMs = getAudioDuration(uri)
        )
    }

    // fast scan — only folder names and IDs, no file opening
    fun scanFolderFast(rootUri: Uri): List<Pair<String, String>> {
        val rootDocId = DocumentsContract.getTreeDocumentId(rootUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, rootDocId)
        val folders = mutableListOf<Pair<String, String>>()

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
                val docId = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: continue
                val mime = cursor.getString(2) ?: continue
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    folders.add(docId to name)
                }
            }
        }
        return folders  // only top-level folders — same as before, IDs still match
    }

    // full scan — reads everything including durations
    fun scanFolder(rootUri: Uri): List<Audiobook> {
        val rootDocId = DocumentsContract.getTreeDocumentId(rootUri)
        val audiobooks = mutableListOf<Audiobook>()
        val rootFiles = mutableListOf<Pair<String, String>>()

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, rootDocId)

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
                val docId = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: continue
                val mime = cursor.getString(2) ?: continue

                when {
                    mime == DocumentsContract.Document.MIME_TYPE_DIR -> {
                        // collect all audio files recursively from this folder
                        val allFiles = collectAudioFilesRecursively(rootUri, docId)
                        if (allFiles.isNotEmpty()) {
                            val contents = scanFolderContents(rootUri, docId)
                            val chapters = allFiles.mapIndexed { index, (fileDocId, fileName) ->
                                buildChapter(rootUri, fileDocId, fileName, index)
                            }
                            audiobooks.add(
                                Audiobook(
                                    id = "${name}_${docId.hashCode()}",
                                    title = name,
                                    chapters = chapters,
                                    coverUri = contents.coverUri
                                )
                            )
                        }
                    }
                    isAudioFile(name) -> rootFiles.add(docId to name)
                }
            }
        }

        rootFiles.forEach { (docId, name) ->
            val title = name.substringBeforeLast(".")
            audiobooks.add(
                Audiobook(
                    id = "${title}_${docId.hashCode()}",
                    title = title,
                    chapters = listOf(buildChapter(rootUri, docId, name, 0))
                )
            )
        }

        Log.d("Scanner", "Found ${audiobooks.size} audiobooks")
        return audiobooks.sortedBy { it.title }
    }

    // recursively collects all audio files from a folder and its subfolders
    private fun collectAudioFilesRecursively(
        rootUri: Uri,
        folderDocId: String
    ): List<Pair<String, String>> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, folderDocId)
        val audioFiles = mutableListOf<Pair<String, String>>()
        val subFolders = mutableListOf<String>()

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
                val docId = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: continue
                val mime = cursor.getString(2) ?: continue

                when {
                    mime == DocumentsContract.Document.MIME_TYPE_DIR -> subFolders.add(docId)
                    isAudioFile(name) -> audioFiles.add(docId to name)
                }
            }
        }

        // recurse into subfolders (CD1, CD2, etc)
        subFolders.forEach { subFolderDocId ->
            audioFiles.addAll(collectAudioFilesRecursively(rootUri, subFolderDocId))
        }

        return audioFiles.sortedBy { it.second }
    }

    private fun isAudioFile(name: String): Boolean =
        name.substringAfterLast(".", "").lowercase() in AUDIO_EXTENSIONS
}