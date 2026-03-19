package com.ive.audiobookplayer.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
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
import com.ive.audiobookplayer.model.LibraryGroups
import com.ive.audiobookplayer.model.LibraryState
import com.ive.audiobookplayer.utils.calculateProgress
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class AudioViewModel(application: Application) : AndroidViewModel(application) {

    private val visibleBookIds = MutableStateFlow<Set<String>>(emptySet())
    private var coverLoadDebounceJob: Job? = null
    private var loadingCovers = mutableSetOf<String>()
    private val failedLoads = mutableSetOf<String>()

    private val controllerManager = MediaControllerManager(application)
    private val bookmarkRepository = BookmarkRepository(application)
    private val audiobookScanner = AudiobookScanner(application)
    private val coverArtRepository = CoverArtRepository(application)
    private val coverArtExtractor = CoverArtExtractor(application)
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _libraryState = MutableStateFlow(LibraryState())
    val libraryState: StateFlow<LibraryState> = _libraryState.asStateFlow()


    private val _coverCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val coverCache: StateFlow<Map<String, Bitmap>> = _coverCache.asStateFlow()

    private val coverLoadAttempted = mutableSetOf<String>()
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

        viewModelScope.launch {
            val cached = bookmarkRepository.loadLibraryCache()
            if (cached.isNotEmpty()) {
                _libraryState.update {
                    it.copy(audiobooks = cached, isLoaded = true)
                }
            }

            bookmarkRepository.rootFolderUri.first()?.let { uriString ->
                val uri = uriString.toUri()
                if (cached.isEmpty()) {
                    scanLibrary(uri)
                } else {
                    backgroundRescan(uri)
                }
            }

            // todo delete this ??
            // restore last opened book
            bookmarkRepository.getLastBookId()?.let { lastId ->
                val lastBook = _libraryState.value.audiobooks.find {
                    it.id == lastId
                }
                if (lastBook != null) {
                    val bookmark = bookmarkRepository.getBookmark(lastId)
                    val speed = bookmarkRepository.getSpeed(lastId)
                    _playerState.update {
                        it.copy(
                            currentAudiobook = lastBook,
                            currentBookTitle = lastBook.title,
                            currentChapterIndex = bookmark?.chapterIndex ?: 0,
                            currentPosition = bookmark?.positionMs ?: 0L,
                            playbackSpeed = speed,
                        )
                    }
                    loadCoverArt(lastBook)
                }
            }
        }
    }

    fun onBooksVisible(bookIds: List<String>) {
        // cancel any pending debounce
        coverLoadDebounceJob?.cancel()

        //start new debounce timer
        coverLoadDebounceJob = viewModelScope.launch {
            //wait for scrolling to stop
            delay(300)

            // clear the loading queue when new scroll happens
            synchronized(loadingCovers) {
                loadingCovers.clear()
            }

            //now load covers only for currently visible books
            loadCoversForVisibleBooks(bookIds)
        }
    }

    /**
     * Load covers only for books that are:
     * 1. Currently visible
     * 2. Not already loaded or loading
     * 3. Haven't failed before
     */
    private suspend fun loadCoversForVisibleBooks(bookIds: List<String>) {
        // filter books that need loading
        val needLoading = bookIds.filter { bookId ->
            // loading cover mutableSet<String> "PowerOfNow.jpg" "Unlimited.png" etc...
            synchronized(loadingCovers) {
                !_coverCache.value.containsKey(bookId) && // not in cache
                        !loadingCovers.contains(bookId) && // not currently loading
                        !failedLoads.contains(bookId) // hasn't failed before
            }
        }

        if (needLoading.isEmpty()) return

        Log.d("CoverLoading", "Loading ${needLoading.size} covers for visible books")

        // load covers in parallel but with limit (3 at a time)
        needLoading.chunked(3).forEach { batch ->


            withContext(Dispatchers.IO) {
                batch.map { bookId ->
                    launch {
                        val audiobook = _libraryState.value.audiobooks.find { it.id == bookId }
                        if (audiobook != null) {
                            loadSingleCover(audiobook)
                        }
                    }
                }.forEach { it.join() }  //w ait for all to complete
            }
            delay(100)
        }
    }

    private suspend fun loadCoverBitmap(audiobook: Audiobook): Bitmap? {
        return withContext(Dispatchers.IO) {
            when {
                coverArtRepository.getCoverUri(audiobook.id) != null -> {
                    decodeBitmapFromUri(
                        Uri.parse(coverArtRepository.getCoverUri(audiobook.id)),
                        targetSize = 128  //  Small for list view
                    )
                }

                audiobook.coverUri != null -> {
                    decodeBitmapFromUri(audiobook.coverUri.toUri(), targetSize = 128)
                }

                else -> {
                    coverArtExtractor.extractCover(
                        Uri.parse(audiobook.chapters.first().uri), targetSize = 128
                    )
                }
            }
        }
    }

    /**
     * Load a single cover with proper state tracking
     */
    private suspend fun loadSingleCover(audiobook: Audiobook) {
        // Mark as loading
        synchronized(loadingCovers) {
            loadingCovers.add(audiobook.id)
        }

        try {
            val bitmap = withContext(Dispatchers.IO) {
                loadCoverBitmap(audiobook)
            }

            if (bitmap != null) {
                // Success - update cache
                _coverCache.update { it + (audiobook.id to bitmap) }
                synchronized(loadingCovers) {
                    loadingCovers.remove(audiobook.id)
                }
            } else {
                // Failed - mark as failed
                synchronized(loadingCovers) {
                    loadingCovers.remove(audiobook.id)
                    failedLoads.add(audiobook.id)
                }
            }
        } catch (e: Exception) {
            // error - mark as failed
            synchronized(loadingCovers) {
                loadingCovers.remove(audiobook.id)
                failedLoads.add(audiobook.id)
            }
            Log.e("CoverLoading", "Failed to load cover for ${audiobook.id}", e)
        }
    }

    fun loadCoverForBook(audiobook: Audiobook) {
        if (coverLoadAttempted.contains(audiobook.id)) return
        coverLoadAttempted.add(audiobook.id)

        viewModelScope.launch(Dispatchers.IO) {

            val bitmap = when {
                coverArtRepository.getCoverUri(audiobook.id) != null -> {
                    decodeBitmapFromUri(
                        Uri.parse(coverArtRepository.getCoverUri(audiobook.id)), targetSize = 128
                    )
                }

                audiobook.coverUri != null -> {
                    decodeBitmapFromUri(Uri.parse(audiobook.coverUri), targetSize = 128)
                }

                else -> {
                    coverArtExtractor.extractCover(
                        Uri.parse(audiobook.chapters.first().uri), targetSize = 128
                    )
                }
            }
            if (bitmap != null) {
                _coverCache.update { it + (audiobook.id to bitmap) }
            }
        }
    }

    // called when user sets a custom cover so it reloads for that specific book
    fun refreshCoverForBook(audiobook: Audiobook) {
        coverLoadAttempted.remove(audiobook.id)
        _coverCache.update { it - audiobook.id }
        loadCoverForBook(audiobook)
    }

    fun scanLibrary(rootUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _libraryState.update { it.copy(isScanning = true) }
            try {
                bookmarkRepository.saveRootFolderUri(rootUri)
                val audiobooks = audiobookScanner.scanFolder(rootUri)
                bookmarkRepository.saveLibraryCache(audiobooks)
                _libraryState.update {
                    it.copy(
                        audiobooks = audiobooks,
                        isLoaded = true,
                        isScanning = false
                    )
                }
                updateLibraryGroups()
            } catch (e: Exception) {
                Log.e("Library", "Scan failed", e)
                _libraryState.update { it.copy(isScanning = false) }
            }
        }
    }

    private fun backgroundRescan(rootUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _libraryState.update { it.copy(isBackgroundScanning = true) }
            try {
                val fastScan = audiobookScanner.scanFolderFast(rootUri)
                val cachedIds = _libraryState.value.audiobooks.map { it.id }.toSet()
                val freshIds = fastScan.map { (docId, name) ->
                    "${name}_${docId.hashCode()}"
                }.toSet()

                // only do the slow full scan if somehting actually changed
                if(cachedIds != freshIds){
                    val fresh = audiobookScanner.scanFolder(rootUri)
                    bookmarkRepository.saveLibraryCache(fresh)
                    _libraryState.update { it.copy(audiobooks = fresh) }
                    updateLibraryGroups()
                }
            }catch (e: Exception){
                Log.e("Library","Background rescan failed", e)
            } finally {
                _libraryState.update { it.copy(isBackgroundScanning = false) }
                updateLibraryGroups()
            }
            val freshAudiobooks = audiobookScanner.scanFolder(rootUri)
            val cachedAudiobooks = _libraryState.value.audiobooks

            val hasChanges = hasLibraryChanged(cachedAudiobooks, freshAudiobooks)
            if (hasChanges) {
                bookmarkRepository.saveLibraryCache(freshAudiobooks)
                _libraryState.update {
                    it.copy(audiobooks = freshAudiobooks)
                }
            }

            _libraryState.update { it.copy(isBackgroundScanning = false) }
            updateLibraryGroups()
        }
    }

    private fun hasLibraryChanged(
        cached: List<Audiobook>,
        fresh: List<Audiobook>
    ): Boolean {
        if (cached.size != fresh.size) return true
        val cachedMap = cached.associateBy { it.id }
        return fresh.any { freshBook ->
            val cachedBook = cachedMap[freshBook.id] ?: return true
            // check if any chapter duration changed
            cachedBook.chapters.zip(freshBook.chapters).any { (c, f) ->
                c.durationMs != f.durationMs
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun playAudiobook(audiobook: Audiobook) {
        viewModelScope.launch {
            saveCurrentPosition()
            saveCurrentSpeed()
            val bookmark = bookmarkRepository.getBookmark(audiobook.id)
            val speed = bookmarkRepository.getSpeed(audiobook.id)

            val coverUri = when {
                coverArtRepository.getCoverUri(audiobook.id) != null ->
                    Uri.parse(coverArtRepository.getCoverUri(audiobook.id))
                audiobook.coverUri != null -> audiobook.coverUri.toUri() else -> null
            }
            controllerManager.playAudiobook(audiobook, bookmark, coverUri)

            //restore saved speed for this book
            _playerState.value.controller?.setPlaybackSpeed(speed)

            _playerState.update {
                it.copy(
                    currentAudiobook = audiobook,
                    currentBookTitle = audiobook.title,
                    currentChapterIndex = bookmark?.chapterIndex ?: 0,
                    currentPosition = bookmark?.positionMs ?: 0L,
                    currentCoverBitmap = null,
                    playbackSpeed = speed,
                )
            }

            // load cover in background
            loadCoverArt(audiobook)
            startPeriodicBookmarkSave()
        }
    }

    private suspend fun saveCurrentSpeed() {
        val state = _playerState.value
        val audiobook = state.currentAudiobook ?: return
        bookmarkRepository.saveSpeed(audiobook.id, state.playbackSpeed)
    }

    private fun loadCoverArt(audiobook: Audiobook) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = when {
                // user manually imported cover
                coverArtRepository.getCoverUri(audiobook.id) != null -> {
                    decodeBitmapFromUri(
                        Uri.parse(coverArtRepository.getCoverUri(audiobook.id)), targetSize = 512
                    )
                }
                // 2 image file found in audiobook folder
                audiobook.coverUri != null -> {
                    decodeBitmapFromUri(Uri.parse(audiobook.coverUri), targetSize = 512)
                }
                // embedded metadata
                else -> {
                    coverArtExtractor.extractCover(
                        Uri.parse(audiobook.chapters.first().uri), targetSize = 512
                    )
                }
            }
            _playerState.update { it.copy(currentCoverBitmap = bitmap) }
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

    // called when user picks an image for the current book
    fun setCustomCover(uri: Uri) {
        val audiobook = _playerState.value.currentAudiobook ?: run {
            Log.d("Cover", "setCustomCover called but currentAudiobook is null!")
            return
        }
        Log.d("Cover", "Setting cover for ${audiobook.title}, uri=$uri")
        viewModelScope.launch {
            coverArtRepository.saveCoverUri(audiobook.id, uri)
            refreshCoverForBook(audiobook)

            viewModelScope.launch(Dispatchers.IO) {
                val bitmap = decodeBitmapFromUri(uri, targetSize = 512)
                _playerState.update { it.copy(currentCoverBitmap = bitmap) }
            }
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
                currentTitle = currentTitle ?: current.currentTitle,
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
            viewModelScope.launch {
                val audiobook = _playerState.value.currentAudiobook ?: return@launch
                bookmarkRepository.saveSpeed(audiobook.id, playbackSpeed)
            }
        }
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

    // save uses controller's currentMediaItemIndex, not stale state
    private suspend fun saveCurrentPosition() {
        val state = _playerState.value
        val audiobook = state.currentAudiobook ?: return
        val controller = state.controller ?: return

        val position = controller.currentPosition.coerceAtLeast(0L)
        val chapterIndex = controller.currentMediaItemIndex


        audiobook.chapters.sumOf { it.durationMs }

        audiobook.chapters.take(controller.currentMediaItemIndex).sumOf { it.durationMs }

        bookmarkRepository.saveBookmark(
            BookmarkEntry(
                audiobookId = audiobook.id,
                chapterIndex = chapterIndex,
                positionMs = position,
            )
        )
    }

    fun updateCurrentChapterDuration(durationMs: Long) {
        val state = _playerState.value
        val audiobook = state.currentAudiobook ?: return
        val chapterIndex = state.controller?.currentMediaItemIndex ?: return

        val updatedChapters = audiobook.chapters.toMutableList()

        if (chapterIndex < updatedChapters.size) {
            updatedChapters[chapterIndex] =
                updatedChapters[chapterIndex].copy(durationMs = durationMs)
            val updatedBook = audiobook.copy(chapters = updatedChapters)
            _playerState.update { it.copy(currentAudiobook = updatedBook) }
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
                it.copy(
                    showChapterList = false, currentChapterIndex = index
                )
            }
        }
    }

    private fun updateLibraryGroups() {
        val audiobooks = _libraryState.value.audiobooks
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
        // sort in progress by recently played
        inProgress.sortByDescending { bookmarks[it.id]?.lastPlayedAt ?: 0L }

        // unplayed alph
        unplayed.sortBy { it.title }

        _libraryState.update {
            it.copy(
                libraryGroups = LibraryGroups(
                    inProgress, unplayed, finished
                )
            )
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()

        if (minutes == null) {
            //cancel timer
            _playerState.update { it.copy(sleepTimerEndTime = 0L, sleepTimerLabel = "") }
            return
        }

        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        _playerState.update { it.copy(sleepTimerEndTime = endTime) }

        sleepTimerJob = viewModelScope.launch {
            while (true) {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    // timer expired - pause
                    _playerState.value.controller?.pause()
                    _playerState.update { it.copy(sleepTimerEndTime = 0L, sleepTimerLabel = "") }
                    break
                }
                val minutes = (remaining / 1000) / 60
                val seconds = (remaining / 1000) % 60
                _playerState.update {
                    it.copy(
                        sleepTimerLabel = "%02d:%02d".format(
                            minutes, seconds
                        )
                    )
                }
                delay(1000)
            }
        }
    }

    fun setSleepTimerEndOfChapter() {
        sleepTimerJob?.cancel()
        _playerState.update { it.copy(sleepTimerEndTime = -1L, sleepTimerLabel = "End of chapter") }
    }


    override fun onCleared() {
        super.onCleared()

        runBlocking {
            saveCurrentSpeed()
            saveCurrentSpeed()
        }
        // clear bitmap cache to prevent memory leaks
        _coverCache.value.values.forEach { it.recycle() }
        _coverCache.update { emptyMap() }

        viewModelScope.launch {
            controllerManager.release()
        }
    }
}


