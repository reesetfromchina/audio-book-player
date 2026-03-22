package com.ive.audiobookplayer.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.PlayerState
import com.ive.audiobookplayer.repository.BookmarkRepository
import com.ive.audiobookplayer.service.AudiobookScanner
import com.ive.audiobookplayer.service.MediaControllerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.ive.audiobookplayer.repository.CoverArtRepository
import com.ive.audiobookplayer.utils.CoverArtExtractor
import androidx.core.net.toUri
import com.ive.audiobookplayer.database.toAudiobook
import com.ive.audiobookplayer.model.AppBackup
import com.ive.audiobookplayer.model.BookmarkBackup
import com.ive.audiobookplayer.model.LibraryGroups
import com.ive.audiobookplayer.model.LibraryState
import com.ive.audiobookplayer.repository.LibraryRepository
import com.ive.audiobookplayer.repository.SettingsRepository
import com.ive.audiobookplayer.utils.calculateProgress
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AudioViewModel(application: Application) : AndroidViewModel(application) {



    private val controllerManager = MediaControllerManager(application)
    private val settingsRepository = SettingsRepository(application)
    private val bookmarkRepository = BookmarkRepository(application)
    private val libraryRepository = LibraryRepository(application)
    private val audiobookScanner = AudiobookScanner(application)
    private val coverArtRepository = CoverArtRepository(application)
    private val coverArtExtractor = CoverArtExtractor(application)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _libraryState = MutableStateFlow(LibraryState())
    val libraryState: StateFlow<LibraryState> = _libraryState.asStateFlow()


    private var bookmarkSaveJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        viewModelScope.launch {
            controllerManager.controller.collect { controller ->
                _playerState.update { it.copy(controller = controller) }
            }
        }

        viewModelScope.launch {
            bookmarkRepository.bookmarks.collect { bookmarks ->
                _libraryState.update { it.copy(bookmarks = bookmarks) }
                updateLibraryGroups()
            }
        }

        // collect from Room — instant on every reopen
        viewModelScope.launch {
            libraryRepository.audiobooks.collect { audiobooksWithChapters ->
                val t1 = System.currentTimeMillis()
                val audiobooks = audiobooksWithChapters.map { it.toAudiobook() }
                val t2 = System.currentTimeMillis()

                _libraryState.update {
                    it.copy(
                        audiobooks = audiobooks, isInitializing = false
                    )
                }
                val t3 = System.currentTimeMillis()
                updateLibraryGroups()
                val t4 = System.currentTimeMillis()
                preloadCoverUris(audiobooks)
                Log.d("Perf", "mapping=${t2-t1}ms state=${t3-t2}ms groups=${t4-t3}ms total=${t4-t1}ms")
            }
        }

        viewModelScope.launch {
            settingsRepository.rootFolderUri.first()?.let { uriString ->
                val uri = uriString.toUri()
                val cachedIds = libraryRepository.getAllIds()
                Log.d("Init", "Room has ${cachedIds.size} books")
                if (cachedIds.isEmpty()) {
                    Log.d("Init", "No books in Room → full scan")
                    scanLibrary(uri)
                } else {
                    Log.d("Init", "Books found → background rescan")
                    backgroundRescan(uri)
                }
            } ?: run {
//                 first launch — no folder picked yet
                _libraryState.update { it.copy(isInitializing = false) }
            }

            // restore last opened book
            settingsRepository.getLastBookId()?.let { lastId ->
                val lastBook = _libraryState.value.audiobooks.find { it.id == lastId }
                if (lastBook != null) {
                    val bookmark = bookmarkRepository.getBookmark(lastId)
                    _playerState.update {
                        it.copy(
                            currentAudiobook = lastBook,
                            currentBookTitle = lastBook.title,
                            currentChapterIndex = bookmark?.chapterIndex ?: 0,
                            currentPosition = bookmark?.positionMs ?: 0L,
                            playbackSpeed = bookmark?.playbackSpeed ?: 1f,
                        )
                    }
                    loadCoverArt(lastBook)
                }
            }
        }
    }

    // region Cover loading


    fun rescan() {
        viewModelScope.launch {
            settingsRepository.rootFolderUri.first()?.let { uriString ->
                backgroundRescan(uriString.toUri())
            }
        }
    }

    // called when user picks a custom cover image
    fun setCustomCover(uri: Uri) {
        val audiobook = _playerState.value.currentAudiobook ?: return
        viewModelScope.launch {
            coverArtRepository.saveCoverUri(audiobook.id, uri)
            resolvedCoverUris.remove(audiobook.id)
            val bitmap = withContext(Dispatchers.IO) {
                decodeBitmapFromUri(uri, targetSize = 512)
            }
            _playerState.update { it.copy(currentCoverBitmap = bitmap) }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allBookmarks = bookmarkRepository.getAllBookmarksList()
                val allAudiobooks = libraryRepository.getAllAudiobookEntities()

                val bookmarkBackup = allBookmarks.map { b->
                    val title = allAudiobooks.find {it.id == b.audiobookId}?.title ?: ""

                    // resolve custom cover
                    val coverBase64 = try{
                        val customUri = coverArtRepository.getCoverUri(b.audiobookId)
                        if(customUri!= null){
                            getApplication<Application>().contentResolver
                                .openInputStream(Uri.parse(customUri))?.use { stream ->
                                    Base64.encodeToString(
                                        stream.readBytes(),
                                        Base64.DEFAULT
                                    )
                                }
                        } else null
                    } catch (e: Exception){
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



                val backup = AppBackup(bookmarks = bookmarkBackup)
                val json = Json.encodeToString(backup)
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray())
                }
                Log.d("Backup", "Export successful to $uri")
            } catch (e: Exception) {
                Log.e("Backup", "Export failed", e)
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = getApplication<Application>().contentResolver
                    .openInputStream(uri)?.use { it.bufferedReader().readText() } ?: return@launch
                val backup = Json.decodeFromString<AppBackup>(json)
                val currentAudiobooks = libraryRepository.getAllAudiobookEntities()

                // restore bookmarks
                bookmarkRepository.importBackup(backup, currentAudiobooks)

                // restore custom covers separately using coverArtRepository
                backup.bookmarks.forEach { bookmarkBackup ->
                    bookmarkBackup.customCoverBase64?.let { base64 ->
                        try {
                            val matchedId = currentAudiobooks
                                .find { it.id == bookmarkBackup.audiobookId || it.title == bookmarkBackup.bookTitle }
                                ?.id ?: bookmarkBackup.audiobookId

                            val bytes = Base64.decode(base64, Base64.DEFAULT)
                            val coverFile = File(
                                getApplication<Application>().filesDir,
                                "custom_covers/${matchedId}.jpg"
                            )
                            coverFile.parentFile?.mkdirs()
                            coverFile.writeBytes(bytes)

                            // use existing saveCoverUri
                            coverArtRepository.saveCoverUri(matchedId, coverFile.toUri())
                        } catch (e: Exception) {
                            Log.e("Import", "Failed to restore cover for ${bookmarkBackup.bookTitle}", e)
                        }
                    }
                }

                resolvedCoverUris.clear()
                Log.d("Backup", "Import successful — ${backup.bookmarks.size} bookmarks restored")
            } catch (e: Exception) {
                Log.e("Backup", "Import failed", e)
            }
        }
    }


    // region Library

    fun scanLibrary(rootUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _libraryState.update { it.copy(isScanning = true) }
            try {
                settingsRepository.saveRootFolderUri(rootUri)
                val audiobooks = audiobookScanner.scanFolder(rootUri)
                Log.d("Scan", "Scanned ${audiobooks.size} books — inserting into Room")
                audiobooks.forEach { libraryRepository.insertAudiobook(it) }
                Log.d("Scan", "All books inserted")
            } catch (e: Exception) {
                Log.e("Scan", "Scan failed", e)
            } finally {
                _libraryState.update { it.copy(isScanning = false) }
            }
        }
    }

    private fun backgroundRescan(rootUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            _libraryState.update { it.copy(isBackgroundScanning = true) }
            try {
                val t1 = System.currentTimeMillis()
                val fastScan = audiobookScanner.scanFolderFast(rootUri)
                Log.d("Perf", "fastScan=${System.currentTimeMillis()-t1}ms")

                val t2 = System.currentTimeMillis()
                val cachedIds = libraryRepository.getAllIds()
                Log.d("Perf", "getAllIds=${System.currentTimeMillis()-t2}ms")

                val freshIds = fastScan.map { (docId, name) ->
                    "${name}_${docId.hashCode()}"
                }.toSet()

                val newIds = freshIds - cachedIds
                val removedIds = cachedIds - freshIds

                Log.d("Rescan", "cached=${cachedIds.size} fresh=${freshIds.size} new=${newIds.size} removed=${removedIds.size}")

                removedIds.forEach { libraryRepository.deleteAudiobook(it) }

                if (newIds.isNotEmpty()) {
                    Log.d("Rescan", "New IDs: $newIds")
                    Log.d("Rescan", "Cached IDs: $cachedIds")
                    val t3 = System.currentTimeMillis()
                    val allFresh = audiobookScanner.scanFolder(rootUri)
                    Log.d("Perf", "scanFolder=${System.currentTimeMillis()-t3}ms")
                    allFresh.filter { it.id in newIds }
                        .forEach { libraryRepository.insertAudiobook(it) }
                }
            } catch (e: Exception) {
                Log.e("Rescan", "Background rescan failed", e)
            } finally {
                _libraryState.update { it.copy(isBackgroundScanning = false) }
                Log.d("Perf", "backgroundRescan total=${System.currentTimeMillis()-start}ms")
            }
        }
    }



    private val resolvedCoverUris = mutableMapOf<String, Uri?>()

    private fun preloadCoverUris(audiobooks: List<Audiobook>){
        viewModelScope.launch(Dispatchers.IO){
            audiobooks.forEach { audiobook->
                if(!resolvedCoverUris.containsKey(audiobook.id)){
                    val customUri = coverArtRepository.getCoverUri(audiobook.id)
                    val uri = when{
                        customUri != null -> Uri.parse(customUri)
                        audiobook.coverUri != null -> audiobook.coverUri.toUri()
                        else -> null
                    }
                    resolvedCoverUris[audiobook.id] = uri
                }
            }
        }
    }

     fun resolveCoverUri(audiobook: Audiobook): Uri? {
        // return from cache — populated by preloadCoverUris
        return resolvedCoverUris[audiobook.id]
    }

    private suspend fun extractAndCacheEmbeddedCover(audiobook: Audiobook) : Uri? =
        withContext(Dispatchers.IO){
            try {
                // check if already extracted

                val cacheFile = File(
                    getApplication<Application>().cacheDir,
                    "covers/${audiobook.id}.jpg"
                )
                if (cacheFile.exists()) return@withContext cacheFile.toUri()

                // extract from audio file
                val bitmap = coverArtExtractor.extractCover(
                    Uri.parse(audiobook.chapters.first().uri),
                    targetSize = 512,
                ) ?: return@withContext null

                // save to disk
                cacheFile.parentFile?.mkdirs()
                cacheFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bitmap.recycle()

                cacheFile.toUri()
            } catch (e: Exception) {
                Log.e("Cover", "Failed to extract embedded cover for ${audiobook.title}", e)
                null
            }
        }

    private fun loadCoverArt(audiobook: Audiobook) {
        viewModelScope.launch(Dispatchers.IO) {
            val customUri = coverArtRepository.getCoverUri(audiobook.id)
            val bitmap = when {
                customUri != null -> decodeBitmapFromUri(Uri.parse(customUri), targetSize = 512)
                audiobook.coverUri != null -> decodeBitmapFromUri(audiobook.coverUri.toUri(), targetSize = 512)
                else -> coverArtExtractor.extractCover(
                    Uri.parse(audiobook.chapters.first().uri), targetSize = 512
                )
            }
            _playerState.update { it.copy(currentCoverBitmap = bitmap) }
        }
    }

    // region playback

    @OptIn(UnstableApi::class)
    fun playAudiobook(audiobook: Audiobook) {
        viewModelScope.launch {
            saveCurrentPosition()

            val bookmark = withContext(Dispatchers.IO) {
                bookmarkRepository.getBookmark(audiobook.id)
            }

            val speed = bookmark?.playbackSpeed ?: 1f
            val customCoverUri = withContext(Dispatchers.IO) {
                coverArtRepository.getCoverUri(audiobook.id)
            }

            val coverUri = when {
                customCoverUri != null -> Uri.parse(customCoverUri)
                audiobook.coverUri != null -> audiobook.coverUri.toUri()
                else -> null
            }

            controllerManager.playAudiobook(
                audiobook,
                startChapterIndex = bookmark?.chapterIndex ?: 0,
                startPositionMs = bookmark?.positionMs ?: 0L,
                coverUri
            )
            _playerState.value.controller?.setPlaybackSpeed(speed)

            _playerState.update {
                it.copy(
                    currentAudiobook = audiobook,
                    currentBookTitle = audiobook.title,
                    currentChapterIndex = bookmark?.chapterIndex ?: 0,
                    currentPosition = bookmark?.positionMs ?: 0L,
                    playbackSpeed = speed,
                )
            }

            settingsRepository.saveLastBookId(audiobook.id)
            loadCoverArt(audiobook)
            startPeriodicBookmarkSave()
        }
    }

    fun updatePlayerState(
        isPlaying: Boolean? = null,
        currentTitle: String? = null,
        currentPosition: Long? = null,
        duration: Long? = null,
        playbackSpeed: Float? = null,
        currentChapterIndex: Int? = null,
    ) {
        _playerState.update { current ->
            current.copy(
                isPlaying = isPlaying ?: current.isPlaying,
                chapterTitle = currentTitle ?: current.chapterTitle,
                currentPosition = currentPosition ?: current.currentPosition,
                duration = duration ?: current.duration,
                playbackSpeed = playbackSpeed ?: current.playbackSpeed,
                currentChapterIndex = currentChapterIndex ?: current.currentChapterIndex,
            )
        }
        if (isPlaying == false) {
            viewModelScope.launch { saveCurrentPosition() }
        }
        if (playbackSpeed != null) {
            viewModelScope.launch { saveCurrentPosition() }
        }
    }

    fun updateCurrentChapterDuration(durationMs: Long) {
        val state = _playerState.value
        val audiobook = state.currentAudiobook ?: return
        val chapterIndex = state.controller?.currentMediaItemIndex ?: return


        val updatedChapters = audiobook.chapters.toMutableList()

        if (chapterIndex < updatedChapters.size) {
            updatedChapters[chapterIndex] =
                updatedChapters[chapterIndex].copy(durationMs = durationMs)
            _playerState.update { it.copy(currentAudiobook = audiobook.copy(chapters = updatedChapters)) }

            viewModelScope.launch(Dispatchers.IO) {
                libraryRepository.updateChapterDuration(audiobook.id, chapterIndex, durationMs)
            }
        }
    }

    fun toggleChapterList() {
        _playerState.update { it.copy(showChapterList = !it.showChapterList) }
    }

    fun jumpToChapter(index: Int) {
        val controller = _playerState.value.controller ?: return
        viewModelScope.launch {
            saveCurrentPosition()
            controller.seekTo(index, 0)
            _playerState.update {
                it.copy(showChapterList = false, currentChapterIndex = index)
            }
        }
    }

    private suspend fun saveCurrentPosition() {
        val state = _playerState.value
        val audiobook = state.currentAudiobook ?: return
        val controller = state.controller ?: return
        bookmarkRepository.saveBookmark(
            audiobookId = audiobook.id,
            chapterIndex = controller.currentMediaItemIndex,
            positionMs = controller.currentPosition.coerceAtLeast(0L),
            playbackSpeed = state.playbackSpeed
        )
    }

    private fun startPeriodicBookmarkSave() {
        bookmarkSaveJob?.cancel()
        bookmarkSaveJob = viewModelScope.launch {
            while (true) {
                delay(10_000)
                saveCurrentPosition()
            }
        }
    }


    // region sleep timer

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null) {
            _playerState.update { it.copy(sleepTimerEndTime = 0L, sleepTimerLabel = "") }
            return
        }
        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        _playerState.update { it.copy(sleepTimerEndTime = endTime) }
        sleepTimerJob = viewModelScope.launch {
            while (true) {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    _playerState.value.controller?.pause()
                    _playerState.update { it.copy(sleepTimerEndTime = 0L, sleepTimerLabel = "") }
                    break
                }
                val mins = (remaining / 1000) / 60
                val secs = (remaining / 1000) % 60
                _playerState.update {
                    it.copy(sleepTimerLabel = "%02d:%02d".format(mins, secs))
                }
                delay(1000)
            }
        }
    }

    fun setSleepTimerEndOfChapter() {
        sleepTimerJob?.cancel()
        _playerState.update { it.copy(sleepTimerEndTime = -1L, sleepTimerLabel = "End of chapter") }
    }

    fun toggleLibraryView() {
        _libraryState.update { it.copy(isGridView = !it.isGridView) }
    }

    private fun updateLibraryGroups() {
        viewModelScope.launch(Dispatchers.Default) {
            val audiobooks = _libraryState.value.audiobooks
            if (audiobooks.isEmpty()) return@launch

            val bookmarks = _libraryState.value.bookmarks

            val inProgress = mutableListOf<Audiobook>()
            val unplayed = mutableListOf<Audiobook>()
            val finished = mutableListOf<Audiobook>()

            audiobooks.forEach { book ->
                val bookmark = bookmarks[book.id]
                val progress = calculateProgress(book, bookmark)
                when {
                    bookmark == null -> unplayed.add(book)
                    progress >= 95f -> finished.add(book)
                    else -> inProgress.add(book)
                }
            }

            inProgress.sortByDescending { bookmarks[it.id]?.lastPlayedAt ?: 0L }
            unplayed.sortBy { it.title }

            _libraryState.update {
                it.copy(groups = LibraryGroups(inProgress, unplayed, finished))
            }
        }
    }

    private fun decodeBitmapFromUri(uri: Uri, targetSize: Int = 128): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            options.inSampleSize =
                calculateSampleSize(options.outWidth, options.outHeight, targetSize)
            options.inJustDecodeBounds = false
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            Log.e("Cover", "Failed to decode image: ${e.message}")
            null
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, targetSize: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > targetSize || height / sampleSize > targetSize) {
            sampleSize *= 2
        }
        return sampleSize
    }

    override fun onCleared() {
        super.onCleared()
        runBlocking { saveCurrentPosition() }
        viewModelScope.launch { controllerManager.release() }
    }
}