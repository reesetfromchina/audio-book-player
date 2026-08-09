package com.ive.audiobookplayer.viewmodel
import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.content.edit
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
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import com.ive.audiobookplayer.database.entity.NoteEntity
import com.ive.audiobookplayer.database.mapper.toAudiobook
import com.ive.audiobookplayer.database.mapper.toBookmarkEntry
import com.ive.audiobookplayer.utils.SleepTimerState
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.model.LibraryGroups
import com.ive.audiobookplayer.model.LibraryState
import com.ive.audiobookplayer.model.SnackbarEvent
import com.ive.audiobookplayer.repository.LibraryRepository
import com.ive.audiobookplayer.repository.NoteRepository
import com.ive.audiobookplayer.repository.SettingsRepository
import com.ive.audiobookplayer.service.BackupManager
import com.ive.audiobookplayer.service.PlayerWidget
import com.ive.audiobookplayer.utils.calculateProgress
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

class AudioViewModel(application: Application) : AndroidViewModel(application) {

    private val controllerManager = MediaControllerManager(application)
    private val settingsRepository = SettingsRepository(application)
    private val bookmarkRepository = BookmarkRepository(application)
    private val libraryRepository = LibraryRepository(application)
    private val coverArtRepository = CoverArtRepository(application)
    private val noteRepository = NoteRepository(application)

    private val backupManager = BackupManager(
        bookmarkRepository,
        libraryRepository,
        noteRepository,
        coverArtRepository,
        application.contentResolver
    )

    private val audiobookScanner = AudiobookScanner(application)
    private val coverArtExtractor = CoverArtExtractor(application)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _libraryState = MutableStateFlow(LibraryState())
    val libraryState: StateFlow<LibraryState> = _libraryState.asStateFlow()

    private val _events = MutableSharedFlow<LibraryEvent>()
    val events = _events.asSharedFlow()

    private val _snackbarEvent = MutableStateFlow<SnackbarEvent?>(null)
    val snackbarEvent: StateFlow<SnackbarEvent?> = _snackbarEvent.asStateFlow()

    private var sleepTimerJob: Job? = null
    private val sleepTimerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "audiobookplayer.SLEEP_TIMER_FIRED") {
                _playerState.update {
                    it.copy(sleepTimerEndTime = 0L, sleepTimerLabel = "")
                }
            }
        }
    }

    private val _startupComplete = MutableStateFlow(false)
    val startupComplete = _startupComplete.asStateFlow()

    init {

        // register receiver
        val filter = IntentFilter("audiobookplayer.SLEEP_TIMER_FIRED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(
                sleepTimerReceiver, filter, Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            ContextCompat.registerReceiver(
                getApplication<Application>(),
                sleepTimerReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

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
                val audiobooks = audiobooksWithChapters.map { it.toAudiobook() }

                preloadCoverUris(audiobooks)
                _libraryState.update { it.copy(audiobooks = audiobooks, isInitializing = false) }

                updateLibraryGroups()

                restoreLastSession(audiobooks)
            }
        }

        viewModelScope.launch {
            settingsRepository.rootFolderUri.first()?.let { uriString ->
                val uri = uriString.toUri()
                val cachedIds = libraryRepository.getAllIds()
                if (cachedIds.isEmpty()) {
                    scanLibrary(uri)
                } else {
                    backgroundRescan(uri)
                }
            } ?: run {
//                 first launch — no folder picked yet
                _libraryState.update { it.copy(isInitializing = false) }
            }

        }
    }

    // region Cover loading
    fun rescan() {
        viewModelScope.launch {
            val uriString = settingsRepository.rootFolderUri.first()
            if(uriString!= null){
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
                coverArtExtractor.decodeBitmapFromUri(uri, targetSize = 512)
            }
            _playerState.update { it.copy(currentCoverBitmap = bitmap) }
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
                Log.d("Perf", "fastScan=${System.currentTimeMillis() - t1}ms")

                val t2 = System.currentTimeMillis()
                val cachedIds = libraryRepository.getAllIds()
                Log.d("Perf", "getAllIds=${System.currentTimeMillis() - t2}ms")

                val freshIds = fastScan.map { (docId, name) ->
                    "${name}_${docId.hashCode()}"
                }.toSet()

                val newIds = freshIds - cachedIds
                val removedIds = cachedIds - freshIds

                Log.d(
                    "Rescan",
                    "cached=${cachedIds.size} fresh=${freshIds.size} new=${newIds.size} removed=${removedIds.size}"
                )

                removedIds.forEach { libraryRepository.deleteAudiobook(it) }

                if (newIds.isNotEmpty()) {
                    Log.d("Rescan", "New IDs: $newIds")
                    Log.d("Rescan", "Cached IDs: $cachedIds")
                    val t3 = System.currentTimeMillis()
                    val allFresh = audiobookScanner.scanFolder(rootUri)
                    Log.d("Perf", "scanFolder=${System.currentTimeMillis() - t3}ms")
                    allFresh.filter { it.id in newIds }
                        .forEach { libraryRepository.insertAudiobook(it) }
                }
            } catch (e: Exception) {
                Log.e("Rescan", "Background rescan failed", e)
            } finally {
                _libraryState.update { it.copy(isBackgroundScanning = false) }
                Log.d("Perf", "backgroundRescan total=${System.currentTimeMillis() - start}ms")
            }
        }
    }


    private val resolvedCoverUris = mutableMapOf<String, Uri?>()

    private suspend fun preloadCoverUris(audiobooks: List<Audiobook>) {
        withContext(Dispatchers.IO) {
            audiobooks.filter { !resolvedCoverUris.containsKey(it.id) }
                .map { audiobook ->
                    async {
                        val customUri = coverArtRepository.getCoverUri(audiobook.id)
                        val uri = when {
                            customUri != null -> Uri.parse(customUri)
                            audiobook.coverUri != null -> audiobook.coverUri.toUri()
                            else -> null
                        }
                        audiobook.id to uri
                    }
                }
                .awaitAll()
                .forEach { (id, uri) -> resolvedCoverUris[id] = uri }
        }
    }

    fun resolveCoverUri(audiobook: Audiobook): Uri? {
        // return from cache — populated by preloadCoverUris
        return resolvedCoverUris[audiobook.id]
    }

    private fun loadCoverArt(audiobook: Audiobook) {
        viewModelScope.launch(Dispatchers.IO) {
            val customUri = coverArtRepository.getCoverUri(audiobook.id)
            val bitmap = when {
                customUri != null -> coverArtExtractor.decodeBitmapFromUri(
                    Uri.parse(customUri),
                    targetSize = 512
                )

                audiobook.coverUri != null -> coverArtExtractor.decodeBitmapFromUri(
                    audiobook.coverUri.toUri(),
                    targetSize = 512
                )

                else -> coverArtExtractor.extractCover(
                    Uri.parse(audiobook.chapters.first().uri), targetSize = 512
                )
            }
            _playerState.update { it.copy(currentCoverBitmap = bitmap) }
        }
    }


    fun isFinished(audiobook: Audiobook, bookmark: BookmarkEntry?): Boolean {
        return if (calculateProgress(audiobook, bookmark) >= 95f) {
            true
        } else {
            false
        }
    }

    private var isSwitchingBooks = false

    @OptIn(UnstableApi::class)
    fun playAudiobook(audiobook: Audiobook) {
        viewModelScope.launch {
            isSwitchingBooks = true

            resetBooks.remove(audiobook.id)

            try {
                val bookmark = withContext(Dispatchers.IO) {
                    bookmarkRepository.getBookmark(audiobook.id)
                }
                val customCoverUri = withContext(Dispatchers.IO) {
                    coverArtRepository.getCoverUri(audiobook.id)
                }

                val coverUri = when {
                    customCoverUri != null -> Uri.parse(customCoverUri)
                    audiobook.coverUri != null -> audiobook.coverUri.toUri()
                    else -> null
                }

                val playbackSpeed = settingsRepository.defaultPlaybackSpeed.first()

                val isFinished = isFinished(audiobook, bookmark?.toBookmarkEntry())

                val startChapter = if (isFinished) 0 else (bookmark?.chapterIndex ?: 0)
                val startPosition = if (isFinished) 0L else (bookmark?.positionMs ?: 0L)
                val speed = if (isFinished) playbackSpeed else (bookmark?.playbackSpeed ?: playbackSpeed)

                controllerManager.playAudiobook(
                    audiobook,
                    startChapterIndex = startChapter,
                    startPositionMs = startPosition,
                    coverUri
                )

                settingsRepository.saveLastBookId(audiobook.id)
                _playerState.value.controller?.setPlaybackSpeed(speed)
                _playerState.update {
                    it.copy(
                        currentAudiobook = audiobook,
                        currentBookTitle = audiobook.title,
                        currentChapterIndex = startChapter,
                        currentPosition = startPosition,
                        playbackSpeed = speed,
                    )
                }

                loadCoverArt(audiobook)
            } finally {
                isSwitchingBooks = false
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun playAudiobookCustom(audiobook: Audiobook, chapterIndex: Int, positionMs: Long) {
        viewModelScope.launch {
            isSwitchingBooks = true

            resetBooks.remove(audiobook.id)

            try {

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
                    startChapterIndex = chapterIndex,
                    startPositionMs = positionMs,
                    coverUri
                )

                settingsRepository.saveLastBookId(audiobook.id)
                _playerState.value.controller?.setPlaybackSpeed(1f)
                _playerState.update {
                    it.copy(
                        currentAudiobook = audiobook,
                        currentBookTitle = audiobook.title,
                        currentChapterIndex = chapterIndex,
                        currentPosition = positionMs,
                        playbackSpeed = 1f,
                    )
                }

                loadCoverArt(audiobook)
            } finally {
                isSwitchingBooks = false
            }
        }
    }


    fun seekTo(chapterIndex: Int, positionMs: Long) {

        val controller = _playerState.value
            .controller ?: return

        viewModelScope.launch {
            controller.seekTo(chapterIndex, positionMs)

            _playerState.update {
                it.copy(
                    currentChapterIndex = chapterIndex,
                    currentPosition = positionMs
                )
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
                chapterTitle = currentTitle ?: current.chapterTitle,
                currentPosition = currentPosition ?: current.currentPosition,
                duration = duration ?: current.duration,
                playbackSpeed = playbackSpeed ?: current.playbackSpeed,
                currentChapterIndex = currentChapterIndex ?: current.currentChapterIndex,
            )
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
        val controller = state.controller ?: return

        if (isSwitchingBooks) {
            return
        }

        val audiobook = state.currentAudiobook ?: return


        if (resetBooks.contains(audiobook.id)) {
            return
        }
        val position = controller.currentPosition.coerceAtLeast(0L)
        val chapterIndex = controller.currentMediaItemIndex


        if (controller.currentPosition == 0L && controller.currentMediaItemIndex == 0) return

        bookmarkRepository.saveBookmark(
            audiobookId = audiobook.id,
            chapterIndex = chapterIndex,
            positionMs = position,
            playbackSpeed = state.playbackSpeed
        )
    }


    // region sleep timer

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        SleepTimerState.endOfChapterEnabled.set(false)
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

    private val resetBooks = mutableSetOf<String>()

    fun resetProgress(audiobook: Audiobook) {
        viewModelScope.launch(Dispatchers.IO) {
            resetBooks.add(audiobook.id)
            Log.d("Reset", "Deleting bookmark for ${audiobook.title}")
            bookmarkRepository.deleteBookmark(audiobook.id)
            Log.d("Reset", "Bookmark deleted for ${audiobook.title}")

            // if this book is currently playing clear the state.
            if (_playerState.value.currentAudiobook?.id == audiobook.id) {
                _playerState.update {
                    it.copy(
                        currentAudiobook = null,
                        currentPosition = 0L,
                        currentChapterIndex = 0
                    )
                }
            }
        }
    }

    fun setSleepTimerEndOfChapter() {
        sleepTimerJob?.cancel()
        SleepTimerState.endOfChapterEnabled.set(true)
        _playerState.update { it.copy(sleepTimerEndTime = -1L, sleepTimerLabel = "End of chapter") }
    }

    fun toggleLibraryView() {
        _libraryState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun updateAudiobookMetadata(audiobookId: String, title: String, author: String) {
        viewModelScope.launch(Dispatchers.IO) {
            libraryRepository.updateAudiobookMetadata(audiobookId, title, author)
        }
        val current = _playerState.value.currentAudiobook
        if (current?.id == audiobookId) {
            _playerState.update {
                it.copy(
                    currentAudiobook = current.copy(title = title, author = author),
                    currentBookTitle = title
                )
            }
        }
    }

    fun markAsFinished(audiobook: Audiobook) {
        viewModelScope.launch(Dispatchers.IO) {
            val lastChapter = audiobook.chapters.last()

            bookmarkRepository.saveBookmark(
                audiobookId = audiobook.id,
                chapterIndex = audiobook.chapters.size - 1,
                positionMs = lastChapter.durationMs,
                playbackSpeed = 1f
            )
        }
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


    sealed class LibraryEvent {
        object ImportSuccess : LibraryEvent()
        data class ImportError(val message: String) : LibraryEvent()
        object ExportSuccess : LibraryEvent()
        data class ExportError(val message: String) : LibraryEvent()
    }

    fun saveNote(
        audiobookId: String,
        chapterIndex: Int,
        positionMs: Long,
        noteText: String,
    ) {
        viewModelScope.launch {
            try {

                noteRepository.addNote(audiobookId, chapterIndex, positionMs, noteText)
                _snackbarEvent.value = SnackbarEvent(
                    message = "Note saved",
                    hasViewNotesAction = true
                )
            } catch (e: Exception) {
                _snackbarEvent.value = SnackbarEvent(
                    message = "Failed to save note",
                )
            }
        }
    }

    fun onSnackbarShown(){
        _snackbarEvent.value = null
    }

    fun getNotesForAudiobook(audiobookId: String): Flow<List<NoteEntity>> {
        return noteRepository.getNotesForBook(audiobookId)
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
        }
    }

    fun pause() {
        _playerState.value.controller?.pause()
    }

    fun updateWidget(context: Context) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

        val coverCachePath = saveCoverToCache(context, _playerState.value.currentAudiobook?.coverUri)

        prefs.edit {
            putString("title", _playerState.value.currentBookTitle)
            putString("chapter", _playerState.value.chapterTitle)
            putString("coverPath", coverCachePath)
            putInt("progress", _playerState.value.currentAudiobook?.let { book-> calculateProgress(
                book, libraryState.value.bookmarks[book.id]).toInt() } ?: 0 )
            putBoolean("isPlaying", _playerState.value.isPlaying)
        }

        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, PlayerWidget::class.java)
        )
        ids.forEach { PlayerWidget.updateWidget(context, manager, it) }
    }

    private fun saveCoverToCache(context: Context, coverUri: String?): String? {
        coverUri ?: return null
        return try {
            val uri = Uri.parse(coverUri)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "widget_cover.jpg")
            inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("Widget", "Failed to cache cover", e)
            null
        }
    }

    fun exportBackup(uri: Uri){
        viewModelScope.launch(Dispatchers.IO){
            try {
                backupManager.exportBackup(uri)
                _events.emit(LibraryEvent.ExportSuccess)
            } catch (e: Exception){
                _events.emit(LibraryEvent.ExportError(e.message ?: "Error"))
            }
        }
    }

    @kotlin.OptIn(ExperimentalCoilApi::class)
    fun importBackup(uri: Uri){
        viewModelScope.launch(Dispatchers.IO){
            try {
                backupManager.importBackup(uri, getApplication<Application>().filesDir)

                resolvedCoverUris.clear()
                val currentBooks = _libraryState.value.audiobooks
                preloadCoverUris(currentBooks)

                withContext(Dispatchers.Main){
                    Coil.imageLoader(getApplication()).memoryCache?.clear();
                    Coil.imageLoader(getApplication()).diskCache?.clear();
                }

                _events.emit(LibraryEvent.ImportSuccess)
            } catch (e: Exception){
                Log.e("Backup", "Import failed", e)
                _events.emit(LibraryEvent.ImportError(e.message ?: "Error"))
            }
        }
    }
    private var restoreAttempted = false
    private suspend fun restoreLastSession(audiobooks: List<Audiobook>) {
        if (restoreAttempted) return
        restoreAttempted = true


        val lastBookId = settingsRepository.getLastBookId()

        if (lastBookId == null) {
            _startupComplete.value = true
            return
        }

        val audiobook = audiobooks.find { it.id == lastBookId }
        if (audiobook == null) {
            settingsRepository.clearLastBookId()
            _startupComplete.value = true
            return
        }

        val bookmark = bookmarkRepository.getBookmark(lastBookId)

        val chapterIndex = (bookmark?.chapterIndex ?: 0).coerceIn(0, audiobook.chapters.lastIndex)

        _playerState.update {
            it.copy(
                currentAudiobook = audiobook,
                currentBookTitle = audiobook.title,
                currentChapterIndex = chapterIndex,
                currentPosition = bookmark?.positionMs ?: 0L,
                playbackSpeed = bookmark?.playbackSpeed ?: 1f,
                duration = audiobook.chapters[chapterIndex].durationMs
            )
        }

        controllerManager.controller.first { it != null }

        controllerManager.prepareAudiobook(
            audiobook = audiobook,
            startChapterIndex = chapterIndex,
            startPositionMs = bookmark?.positionMs ?: 0L,
            coverUri = resolveCoverUriAnother(audiobook)
        )

        loadCoverArt(audiobook)

        _startupComplete.value = true
    }
    private suspend fun resolveCoverUriAnother(
        audiobook: Audiobook
    ) : Uri?{
        val customCoverUri = coverArtRepository.getCoverUri(audiobook.id)
        val coverUri = when {
            customCoverUri != null -> Uri.parse(customCoverUri)
            audiobook.coverUri != null -> audiobook.coverUri.toUri()
            else -> null
        }
        return coverUri
    }

    private var pausedAt: Long? = null

    fun onPlayPauseClicked() {
        val controller = controllerManager.controller.value ?: return

        if (controller.isPlaying) {
            pausedAt = System.currentTimeMillis()
            controller.pause()
            return
        }

        viewModelScope.launch {
            val rewindSeconds = settingsRepository.autoRewindSeconds.first()

            val pauseDuration = pausedAt?.let {
                System.currentTimeMillis() - it
            } ?: 0L

            if (pauseDuration >= 30_000L && rewindSeconds > 0) {
                controller.seekTo(
                    maxOf(0L, controller.currentPosition - rewindSeconds * 1000L)
                )
            }

            controller.play()
            pausedAt = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(sleepTimerReceiver)
        viewModelScope.launch {
            withTimeout(2000) {
                runCatching { saveCurrentPosition() }
            }
            controllerManager.release()
        }

    }
}