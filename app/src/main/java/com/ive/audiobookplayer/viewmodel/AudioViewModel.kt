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
import androidx.core.net.toUri
import com.ive.audiobookplayer.repository.CoverArtRepository
import com.ive.audiobookplayer.utils.CoverArtExtractor

class AudioViewModel(application: Application) : AndroidViewModel(application) {

    private val controllerManager = MediaControllerManager(application)
    private val bookmarkRepository = BookmarkRepository(application)
    private val audiobookScanner = AudiobookScanner(application)
    private val coverArtRepository = CoverArtRepository(application)
    private val coverArtExtractor = CoverArtExtractor(application)
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _coverCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val coverCache: StateFlow<Map<String, Bitmap>> = _coverCache.asStateFlow()

    private val coverLoadAttempted = mutableSetOf<String>()
    private var bookmarkSaveJob: Job? = null

    init {
        viewModelScope.launch {
            controllerManager.controller.collect { controller ->
                _playerState.update { it.copy(controller = controller) }
            }
        }

        viewModelScope.launch {
            bookmarkRepository.bookmarks.collect { bookmarks ->
                _playerState.update { it.copy(bookmarks = bookmarks) }
            }
        }

        // restore saved library on launch
        viewModelScope.launch {
            bookmarkRepository.rootFolderUri.first()?.let { uriString ->
                Log.d("Library", "Restoring saved folder: $uriString")
                val uri = uriString.toUri()
                scanLibrary(uri)
            } ?: Log.d("Library", "No saved folder found")
        }


    }

    fun loadCoverForBook(audiobook: Audiobook) {
        if (coverLoadAttempted.contains(audiobook.id)) return
        coverLoadAttempted.add(audiobook.id)
        viewModelScope.launch(Dispatchers.IO) {
            val userCover = coverArtRepository.getCoverUri(audiobook.id)
            val bitmap = if (userCover != null) {
                decodeBitmapFromUri(Uri.parse(userCover), targetSize = 128)
            } else {
                coverArtExtractor.extractCover(
                    Uri.parse(audiobook.chapters.first().uri),
                    targetSize = 128
                )
            }
            if (bitmap != null) {
                _coverCache.update { it + (audiobook.id to bitmap) }
            }
        }
    }

    // called when when user sets a custom cover so it reloads for that specific book
    fun refreshCoverForBook(audiobook: Audiobook) {
        coverLoadAttempted.remove(audiobook.id)
        _coverCache.update { it - audiobook.id }
        loadCoverForBook(audiobook)
    }

    fun scanLibrary(rootUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _playerState.update { it.copy(isScanning = true) }

            bookmarkRepository.saveRootFolderUri(rootUri)
            val audiobooks = audiobookScanner.scanFolder(rootUri)

            _playerState.update {
                it.copy(
                    audiobooks = audiobooks,
                    isLibraryLoaded = true,
                    isScanning = false
                )
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun playAudiobook(audiobook: Audiobook) {
        viewModelScope.launch {
            saveCurrentPosition()
            val bookmark = bookmarkRepository.getBookmark(audiobook.id)
            Log.d(
                "Bookmark",
                "playAudiobook title=${audiobook.title} id=${audiobook.id} bookmark=$bookmark"
            )
            controllerManager.playAudiobook(audiobook, bookmark)

            _playerState.update {
                it.copy(
                    currentAudiobook = audiobook,
                    currentBookTitle = audiobook.title,
                    currentChapterIndex = bookmark?.chapterIndex ?: 0,
                    currentPosition = bookmark?.positionMs ?: 0L,
                    currentCoverBitmap = null
                )
            }

            // load cover in background
            loadCoverArt(audiobook)
            startPeriodicBookmarkSave()
        }
    }

    private fun loadCoverArt(audiobook: Audiobook) {
        viewModelScope.launch(Dispatchers.IO) {
            // check user-imported cover first — decode as plain image
            val userCoverUri = coverArtRepository.getCoverUri(audiobook.id)

            if (userCoverUri != null) {
                // larger size for the player screen
                val bitmap = decodeBitmapFromUri(Uri.parse(userCoverUri), targetSize = 512)
                _playerState.update { it.copy(currentCoverBitmap = bitmap) }

                // smaller thumbnail for library cache
                val thumb = decodeBitmapFromUri(Uri.parse(userCoverUri), targetSize = 128)
                if (thumb != null) {
                    _coverCache.update { it + (audiobook.id to thumb) }
                }
                return@launch
            }

            // try embedded art from audio file
            val firstChapterUri = audiobook.chapters.firstOrNull()?.uri ?: return@launch
            val bitmap =
                coverArtExtractor.extractCover(Uri.parse(firstChapterUri), targetSize = 512)
            _playerState.update { it.copy(currentCoverBitmap = bitmap) }
            val thumb = coverArtExtractor.extractCover(Uri.parse(firstChapterUri), targetSize = 128)
            if (thumb != null) {
                _coverCache.update { it + (audiobook.id to thumb) }
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

            viewModelScope.launch(Dispatchers.IO){
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

        bookmarkRepository.saveBookmark(
            BookmarkEntry(
                audiobookId = audiobook.id,
                chapterIndex = chapterIndex,
                positionMs = position
            )
        )
    }

    fun resumeLastSession() {
        viewModelScope.launch {
            val lastId = bookmarkRepository.lastBookId.first() ?: return@launch
            val audiobook = _playerState.value.audiobooks.find { it.id == lastId } ?: return@launch
            playAudiobook(audiobook)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            saveCurrentPosition()
            controllerManager.release()
        }
    }
}