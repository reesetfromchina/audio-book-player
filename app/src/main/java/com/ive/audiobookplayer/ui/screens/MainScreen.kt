package com.ive.audiobookplayer.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.ui.componenets.PlayerControls
import com.ive.audiobookplayer.ui.componenets.SeekBarControl
import com.ive.audiobookplayer.ui.componenets.SleepTimerDialog
import com.ive.audiobookplayer.ui.componenets.SpeedPickerDialog
import com.ive.audiobookplayer.utils.calculateTimeRemaining
import com.ive.audiobookplayer.utils.formatDuration
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

    // player listener
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
                if (playerState.sleepTimerEndTime == -1L) {
                    controller.pause()
                    viewModel.setSleepTimer(null)
                }
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
    val coverSize = (configuration.screenHeightDp * 0.28f).dp.coerceIn(140.dp, 260.dp)
    val bookmark = libraryState.bookmarks[playerState.currentAudiobook?.id]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // book title
        Text(
            text = playerState.currentBookTitle.ifEmpty { playerState.chapterTitle },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // chapter info
        if ((playerState.currentAudiobook?.chapters?.size ?: 0) > 1) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Chapter ${playerState.currentChapterIndex + 1} · ${playerState.chapterTitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.weight(1f))

        // cover art
        CoverArt(
            coverBitmap = playerState.currentCoverBitmap,
            size = coverSize,
            enabled = playerState.currentAudiobook != null,
            onPickCover = onPickCover
        )

        Spacer(Modifier.weight(1f))

        SeekBarControl(
            controller = controller,
            currentPosition = playerState.currentPosition,
            duration = playerState.duration,
            playbackSpeed = playerState.playbackSpeed,
            onPositionChange = { viewModel.updatePlayerState(currentPosition = it) }
        )

        Spacer(Modifier.height(8.dp))

        PlayerControls(
            controller = controller,
            isPlaying = playerState.isPlaying,
        )

        Spacer(Modifier.height(8.dp))

        // speed + sleep timer + remaining time
        PlayerBottomControls(
            playbackSpeed = playerState.playbackSpeed,
            sleepTimerLabel = playerState.sleepTimerLabel,
            audiobook = playerState.currentAudiobook,
            bookmark = bookmark,
            onSpeedClick = { showSpeedDialog = true },
            onSleepTimerClick = { showSleepTimerDialog = true }
        )

        Spacer(Modifier.weight(1f))

        // chapters + library buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if ((playerState.currentAudiobook?.chapters?.size ?: 0) > 1) {
                OutlinedButton(
                    onClick = { viewModel.toggleChapterList() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Chapters (${playerState.currentAudiobook?.chapters?.size})")
                }
            }
            Button(
                onClick = onBackToLibrary,
                modifier = Modifier.weight(1f)
            ) {
                Text("Library")
            }
        }
    }
}

// extracted components

@Composable
private fun CoverArt(
    coverBitmap: Bitmap?,
    size: Dp,
    enabled: Boolean,
    onPickCover: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled) { onPickCover() },
        contentAlignment = Alignment.Center
    ) {
        if (coverBitmap != null) {
            Image(
                bitmap = coverBitmap.asImageBitmap(),
                contentDescription = "Cover art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Change cover",
                    modifier = Modifier.size(14.dp)
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap to add cover",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlayerBottomControls(
    playbackSpeed: Float,
    sleepTimerLabel: String,
    audiobook: Audiobook?,
    bookmark: BookmarkEntry?,
    onSpeedClick: () -> Unit,
    onSleepTimerClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSpeedClick) {
                Text(
                    text = if (playbackSpeed == 1f) "Speed: 1x" else "Speed: ${playbackSpeed}x",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(onClick = onSleepTimerClick) {
                Text(
                    text = if (sleepTimerLabel.isNotEmpty()) "⏱ $sleepTimerLabel"
                    else "Sleep timer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (sleepTimerLabel.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // remaining time
        if (audiobook != null) {
            val remainingMs = calculateTimeRemaining(audiobook as Audiobook, bookmark, playbackSpeed)
            if (remainingMs > 0) {
                val text = if (playbackSpeed != 1f) {
                    val naturalMs = calculateTimeRemaining(audiobook, bookmark, 1f)
                    "${formatDuration(remainingMs)} remaining · ${formatDuration(naturalMs)} at 1x"
                } else {
                    "${formatDuration(remainingMs)} remaining"
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}