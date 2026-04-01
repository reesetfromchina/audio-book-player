package com.ive.audiobookplayer.service

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.Chapter
import com.ive.audiobookplayer.utils.CueParser
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File


class AudiobookScanner(private val context: Context) {

    companion object {
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "m4b", "aac", "ogg", "flac", "wav", "opus")
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
        private val COVER_NAMES = setOf("cover", "folder", "front", "artwork")
    }

    private data class FolderContents(
        val audioFiles: List<Pair<String, String>>,
        val coverUri: String?,
        val cueFileContent: String? = null
    )

    private fun scanFolderContents(rootUri: Uri, folderDocId: String): FolderContents {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, folderDocId)
        val audioFiles = mutableListOf<Pair<String, String>>()
        var priorityCover: String? = null
        var firstImageUri: String? = null
        var cueContent: String? = null

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
                    ext == "cue" -> {
                        try {
                            val cueUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, docId)
                            cueContent = context.contentResolver.openInputStream(cueUri)
                                ?.bufferedReader()
                                ?.readText()
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
        return FolderContents(
            audioFiles = audioFiles.sortedBy { it.second },
            coverUri = priorityCover ?: firstImageUri,
            cueFileContent = cueContent,
        )
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

    private fun extractAndSaveEmbeddedCover(context: Context, audioUri: Uri, audiobookId: String): String? {
        return try {
            val cacheFile = File(context.cacheDir, "covers/${audiobookId}.jpg")
            if (cacheFile.exists()) return cacheFile.toUri().toString()

            val retriever = MediaMetadataRetriever()
            val pfd = context.contentResolver.openFileDescriptor(audioUri, "r") ?: return null
            pfd.use { retriever.setDataSource(it.fileDescriptor) }

            val art = retriever.embeddedPicture ?: return null
            retriever.release()

            cacheFile.parentFile?.mkdirs()
            cacheFile.writeBytes(art)

            cacheFile.toUri().toString()
        } catch (e: Exception) {
            Log.e("Scanner", "Failed to extract embedded cover", e)
            null
        }
    }

    private suspend fun buildAudiobook(rootUri: Uri, folderName: String, folderDocId: String, files: List<Pair<String, String>>): Audiobook = coroutineScope {

        val contents = scanFolderContents(rootUri, folderDocId)

        val chapters = when {
            contents.cueFileContent != null && files.size == 1 ->{
                val audioUri = DocumentsContract
                    .buildDocumentUriUsingTree(rootUri, files.first().first)
                    .toString()
                val cueChapters = CueParser.parse(contents.cueFileContent, audioUri)

                if (cueChapters.isNotEmpty()){
                    //get total duration for the last chapter
                    val totalDuration = getAudioDuration(
                        DocumentsContract.buildDocumentUriUsingTree(rootUri, files.first().first)
                    )
                    // fix last chapter duration
                    cueChapters.toMutableList().also { list ->
                        val last = list.last()
                        if (last.durationMs == 0L && totalDuration > 0L){
                            list[list.size -1 ] = last.copy(
                                durationMs = totalDuration - last.startMs
                            )
                        }
                    }
                } else{
                    // cue parse failed -- fall back to single chapter
                    listOf(buildChapter(rootUri, files.first().first, files.first().second, 0))
                }
            }
            // multiple audio files - treat each as a chapter
            else ->{
                files.mapIndexed { index, (docId, name) ->
                    async(Dispatchers.IO){
                        buildChapter(rootUri, docId,name, index)
                    }
                }.awaitAll()
            }
        }


        // if no folder cover, try extracting from first audio file
        val coverUri = contents.coverUri ?: run {
            val firstUri = DocumentsContract.buildDocumentUriUsingTree(
                rootUri,
                files.first().first
            )
            val id = "${folderName}_${folderDocId.hashCode()}"
            extractAndSaveEmbeddedCover(context, firstUri, id)
        }

        Audiobook(
            id = "${folderName}_${folderDocId.hashCode()}",
            title = folderName,
            chapters = chapters,
            coverUri = coverUri
        )
    }
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
        return folders
    }
    // full scan — reads everything including durations
    suspend fun scanFolder(rootUri: Uri): List<Audiobook> = coroutineScope {
        val rootDocId = DocumentsContract.getTreeDocumentId(rootUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, rootDocId)
        val audiobookDeferred = mutableListOf<Deferred<Audiobook?>>()
        val rootFiles = mutableListOf<Pair<String, String>>()

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
                        audiobookDeferred.add(async(Dispatchers.IO) {
                            val allFiles = collectAudioFilesRecursively(rootUri, docId)
                            if (allFiles.isNotEmpty()) {
                                buildAudiobook(rootUri, name, docId, allFiles)
                            } else null
                        })
                    }
                    isAudioFile(name) -> rootFiles.add(docId to name)
                }
            }
        }

        val audiobooks = audiobookDeferred.awaitAll().filterNotNull().toMutableList()

        val rootDeferred = rootFiles.map { (docId, name) ->
            async(Dispatchers.IO) {
                val uri = DocumentsContract.buildDocumentUriUsingTree(rootUri, docId)
                val title = name.substringBeforeLast(".")
                Audiobook(
                    id = "${title}_${docId.hashCode()}",
                    title = title,
                    chapters = listOf(
                        Chapter(
                            uri = uri.toString(),
                            title = title,
                            index = 0,
                            durationMs = getAudioDuration(uri)
                        )
                    )
                )
            }
        }
        audiobooks.addAll(rootDeferred.awaitAll())

        audiobooks.sortedBy { it.title }
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