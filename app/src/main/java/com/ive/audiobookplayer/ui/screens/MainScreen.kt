package com.ive.audiobookplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.ive.audiobookplayer.ui.componenets.CoverArt
import com.ive.audiobookplayer.ui.componenets.PlayerBottomControls
import com.ive.audiobookplayer.ui.componenets.PlayerControls
import com.ive.audiobookplayer.ui.componenets.SeekBarControl
import com.ive.audiobookplayer.ui.componenets.SleepTimerDialog
import com.ive.audiobookplayer.ui.componenets.SpeedPickerDialog
import com.ive.audiobookplayer.viewmodel.AudioViewModel
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    viewModel: AudioViewModel,
    onBackToLibrary: () -> Unit,
    onPickCover: () -> Unit,
) {
    val playerState by viewModel.playerState.collectAsState()
    val libraryState by viewModel.libraryState.collectAsState()
    val controller = playerState.controller

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

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

    val configuration = LocalConfiguration.current
    val coverSize = (configuration.screenHeightDp * 0.36f).dp.coerceIn(140.dp, 240.dp)
    val bookmark = libraryState.bookmarks[playerState.currentAudiobook?.id]
    val hasChapters = (playerState.currentAudiobook?.chapters?.size ?: 0) > 1
    val audiobook = playerState.currentAudiobook ?: return

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(Modifier.height(8.dp))
            Text(
                text = playerState.currentBookTitle.ifEmpty { playerState.chapterTitle },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (hasChapters) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = playerState.chapterTitle.ifEmpty {
                        "Chapter ${playerState.currentChapterIndex + 1}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.weight(1f))

            Card(
                modifier = Modifier
                    .size(coverSize)
                    .shadow(12.dp, shape = RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                CoverArt(
                    coverBitmap = playerState.currentCoverBitmap,
                    size = coverSize,
                    enabled = true,
                    onPickCover = onPickCover,
                )
            }

            Spacer(Modifier.weight(2f))

            SeekBarControl(
                controller = controller,
                currentPosition = playerState.currentPosition,
                duration = playerState.duration,
                playbackSpeed = playerState.playbackSpeed,
                onPositionChange = { viewModel.updatePlayerState(currentPosition = it) },
            )

            Spacer(Modifier.height(16.dp))

            PlayerControls(
                controller = controller,
                isPlaying = playerState.isPlaying,
            )

            Spacer(Modifier.height(16.dp))

            PlayerBottomControls(
                playbackSpeed = playerState.playbackSpeed,
                sleepTimerLabel = playerState.sleepTimerLabel,
                audiobook = playerState.currentAudiobook,
                bookmark = bookmark,
                onSpeedClick = { showSpeedDialog = true },
                onSleepTimerClick = { showSleepTimerDialog = true },
                onSaveNote = { chapter, pos, text ->
                    viewModel.saveNote(audiobook.id, chapter, pos, text)
                },
                currentChapterIndex = playerState.currentChapterIndex,
                currentPositionMs = playerState.currentPosition,
                currentChapterTitle = playerState.chapterTitle,
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackToLibrary) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to library",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleChapterList() },
                    enabled = hasChapters,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = "Chapters",
                        tint = if (hasChapters)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}
