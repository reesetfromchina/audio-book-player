package com.ive.audiobookplayer.ui.screens

import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    onPickCover: () -> Unit
) {
    val playerState by viewModel.playerState.collectAsState()
    val libraryState by viewModel.libraryState.collectAsState()
    val controller = playerState.controller
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    if (controller == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // dialogs
    if (playerState.showChapterList && playerState.currentAudiobook != null) {
        ChapterListSheet(
            audiobook = playerState.currentAudiobook!!,
            currentChapterIndex = playerState.currentChapterIndex,
            bookmarks = libraryState.bookmarks,
            onChapterSelected = { viewModel.jumpToChapter(it) },
            onDismiss = { viewModel.toggleChapterList() }
        )
    }
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentLabel = playerState.sleepTimerLabel,
            onDismiss = { showSleepTimerDialog = false },
            onTimerSelected = { viewModel.setSleepTimer(it) },
            onEndChapter = { viewModel.setSleepTimerEndOfChapter() }
        )
    }
    if (showSpeedDialog) {
        SpeedPickerDialog(
            currentSpeed = playerState.playbackSpeed,
            onSpeedSelected = { speed ->
                controller.setPlaybackSpeed(speed)
                viewModel.updatePlayerState(playbackSpeed = speed)
            },
            onDismiss = { showSpeedDialog = false }
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
        while (true) {
            if (!playerState.isPlaying) break
            viewModel.updatePlayerState(currentPosition = controller.currentPosition)
            delay(500)
        }
    }

    val configuration = LocalConfiguration.current
    val coverSize = (configuration.screenHeightDp * 0.38f).dp.coerceIn(140.dp, 260.dp)
    val bookmark = libraryState.bookmarks[playerState.currentAudiobook?.id]
    val hasChapters = (playerState.currentAudiobook?.chapters?.size ?: 0) > 1

    if (libraryState.isInitializing) {
        Log.d("MainScreen", " Audiobook is initializing")
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        return
    }

    val audiobook = playerState.currentAudiobook

    if (audiobook != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    "Now Playing",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = playerState.currentBookTitle.ifEmpty { playerState.chapterTitle },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )

            if (hasChapters) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = playerState.chapterTitle.ifEmpty {
                        "Chapter ${playerState.currentChapterIndex + 1}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(48.dp))

            // cover art
            Card(
                modifier = Modifier
                    .size(coverSize)
                    .shadow(8.dp, shape = RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                CoverArt(
                    coverBitmap = playerState.currentCoverBitmap,
                    size = coverSize,
                    enabled = playerState.currentAudiobook != null,
                    onPickCover = onPickCover
                )
            }

            Spacer(Modifier.weight(1f))

            // seek bar
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                SeekBarControl(
                    controller = controller,
                    currentPosition = playerState.currentPosition,
                    duration = playerState.duration,
                    playbackSpeed = playerState.playbackSpeed,
                    onPositionChange = { viewModel.updatePlayerState(currentPosition = it) }
                )
            }

            Spacer(Modifier.height(8.dp))

            // player controls
            PlayerControls(
                controller = controller,
                isPlaying = playerState.isPlaying,
            )

            Spacer(Modifier.height(12.dp))

            // speed + sleep timer + remaining time
            PlayerBottomControls(
                playbackSpeed = playerState.playbackSpeed,
                sleepTimerLabel = playerState.sleepTimerLabel,
                audiobook = playerState.currentAudiobook,
                bookmark = bookmark,
                onSpeedClick = { showSpeedDialog = true },
                onSleepTimerClick = { showSleepTimerDialog = true }
            )


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToLibrary) {
                    Icon(
                        Icons.AutoMirrored.Filled.LibraryBooks,
                        contentDescription = "Back to library"
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleChapterList() },
                    enabled = hasChapters
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = "Chapters",
                        tint = if (hasChapters)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

