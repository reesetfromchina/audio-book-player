package com.ive.audiobookplayer.ui.screens

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.navigation.NavController
import com.ive.audiobookplayer.ui.componenets.CoverArt
import com.ive.audiobookplayer.ui.componenets.PlayerBottomControls
import com.ive.audiobookplayer.ui.componenets.PlayerControls
import com.ive.audiobookplayer.ui.componenets.SeekBarControl
import com.ive.audiobookplayer.ui.componenets.SleepTimerDialog
import com.ive.audiobookplayer.ui.componenets.SpeedPickerDialog
import com.ive.audiobookplayer.viewmodel.AudioViewModel
import com.ive.audiobookplayer.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    viewModel: AudioViewModel,
    settingsViewModel: SettingsViewModel,
    navController: NavController,
    onBackToLibrary: () -> Unit,
    onPickCover: () -> Unit,
) {
    val playerState by viewModel.playerState.collectAsState()
    val libraryState by viewModel.libraryState.collectAsState()
    val controller = playerState.controller

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    val coverSize = (configuration.screenWidthDp * 0.75f).dp.coerceIn(190.dp, 380.dp)
    val horizontalPadding = (screenWidth * 0.06f).coerceIn(16.dp, 32.dp)

    val audiobook = playerState.currentAudiobook ?: return
    val bookmark = libraryState.bookmarks[audiobook.id]
    val hasChapters = audiobook.chapters.size > 1

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarEvent by viewModel.snackbarEvent.collectAsStateWithLifecycle()
    val audiobookId = playerState.currentAudiobook?.id

    val settings by settingsViewModel.settingsState.collectAsState()

    if (controller == null || libraryState.isInitializing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (playerState.showChapterList && playerState.currentAudiobook != null) {
        ChapterListSheet(
            audiobook = playerState.currentAudiobook!!,
            currentChapterIndex = playerState.currentChapterIndex,
            bookmarks = libraryState.bookmarks,
            onChapterSelected = { viewModel.jumpToChapter(it) },
            onDismiss = { viewModel.toggleChapterList() },
        )
    }
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentLabel = playerState.sleepTimerLabel,
            onDismiss = { showSleepTimerDialog = false },
            onTimerSelected = { viewModel.setSleepTimer(it) },
            onEndChapter = { viewModel.setSleepTimerEndOfChapter() },
        )
    }
    if (showSpeedDialog) {
        SpeedPickerDialog(
            currentSpeed = playerState.playbackSpeed,
            onSpeedSelected = { speed ->
                controller.setPlaybackSpeed(speed)
                viewModel.updatePlayerState(playbackSpeed = speed)
            },
            onDismiss = { showSpeedDialog = false },
        )
    }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.updatePlayerState(isPlaying = isPlaying)
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                viewModel.updatePlayerState(
                    currentTitle = mediaItem?.mediaMetadata?.title?.toString() ?: "",
                    duration = controller.duration.coerceAtLeast(0L),
                    currentChapterIndex = controller.currentMediaItemIndex,
                )
            }
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                viewModel.updatePlayerState(playbackSpeed = playbackParameters.speed)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    viewModel.updatePlayerState(duration = controller.duration.coerceAtLeast(0L))
                    viewModel.updateCurrentChapterDuration(controller.duration.coerceAtLeast(0L))
                }
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    LaunchedEffect(playerState.isPlaying) {
        while (playerState.isPlaying) {
            viewModel.updatePlayerState(currentPosition = controller.currentPosition)
            delay(500)
        }
    }

    LaunchedEffect(playerState.isPlaying, playerState.currentPosition, playerState.currentBookTitle) {
        viewModel.updateWidget(context)
    }

    LaunchedEffect(snackbarEvent) {
        snackbarEvent?.let { event->

            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = if (event.hasViewNotesAction) "View notes" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed){
                navController.navigate("notes/${audiobookId}")
            }
            viewModel.onSnackbarShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToLibrary) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Spacer(Modifier.weight(1f))
                if (hasChapters) {
                    IconButton(onClick = { viewModel.toggleChapterList() }) {
                        Icon(Icons.AutoMirrored.Filled.List, null)
                    }
                }
            }


            Spacer(Modifier.weight(0.4f))
            Card(
                modifier = Modifier.size(coverSize),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                CoverArt(
                    coverBitmap = playerState.currentCoverBitmap,
                    size = coverSize,
                    enabled = true,
                    onPickCover = onPickCover
                )
            }


            Spacer(Modifier.weight(0.5f))
            Text(
                text = playerState.currentBookTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        animationMode = MarqueeAnimationMode.Immediately,
                        repeatDelayMillis = 1000,
                    )
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = playerState.chapterTitle.ifBlank { "Chapter ${playerState.currentChapterIndex + 1}" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(0.5f))

            SeekBarControl(
                controller = controller,
                currentPosition = playerState.currentPosition,
                duration = playerState.duration,
                playbackSpeed = playerState.playbackSpeed,
                onPositionChange = {
                    viewModel.updatePlayerState(currentPosition = it)
                }
            )

            PlayerControls(
                controller = controller,
                isPlaying = playerState.isPlaying,
                skipBackSeconds = settings.skipBackwardSeconds,
                skipForwardSeconds = settings.skipForwardSeconds,
                onPlayPauseClick = viewModel::onPlayPauseClicked
            )

            Spacer(Modifier.weight(0.8f).heightIn(min = 16.dp))

            PlayerBottomControls(
                playbackSpeed = playerState.playbackSpeed,
                sleepTimerLabel = playerState.sleepTimerLabel,
                onSpeedClick = { showSpeedDialog = true },
                onSleepTimerClick = { showSleepTimerDialog = true },
                onSaveNote = { chapter, pos, text ->
                    viewModel.saveNote(audiobook.id, chapter, pos, text)
                },
                currentChapterIndex = playerState.currentChapterIndex,
                currentPositionMs = playerState.currentPosition,
                currentChapterTitle = playerState.chapterTitle,
                onPause = { viewModel.pause() }
            )

        }
    }
}