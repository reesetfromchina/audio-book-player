package com.ive.audiobookplayer.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.ive.audiobookplayer.ui.componenets.PlaybackSpeedControl
import com.ive.audiobookplayer.ui.componenets.PlayerControls
import com.ive.audiobookplayer.ui.componenets.SeekBarControl
import com.ive.audiobookplayer.ui.componenets.SleepTimerDialog
import com.ive.audiobookplayer.viewmodel.AudioViewModel
import kotlinx.coroutines.delay
@Composable
fun MainScreen(
    viewModel: AudioViewModel,
    onBackToLibrary: () -> Unit,
    onPickCover: () -> Unit
) {
    val playerState by viewModel.playerState.collectAsState()
    val controller = playerState.controller
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    if (controller == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
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

    if (playerState.showChapterList && playerState.currentAudiobook != null) {
        ChapterListSheet(
            audiobook = playerState.currentAudiobook!!,
            currentChapterIndex = playerState.currentChapterIndex,
            bookmarks = playerState.bookmarks,
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

    // cover size adapts to screen height
    val configuration = LocalConfiguration.current
    val coverSize = (configuration.screenHeightDp * 0.28f).dp.coerceIn(140.dp, 260.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Book title
        Text(
            text = playerState.currentBookTitle.ifEmpty { playerState.currentTitle },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // chapter info — only shown when relevant
        if ((playerState.currentAudiobook?.chapters?.size ?: 0) > 1) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Chapter ${playerState.currentChapterIndex + 1} · ${playerState.currentTitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.weight(1f))

        // Cover art
        Box(
            modifier = Modifier
                .size(coverSize)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = playerState.currentAudiobook != null) { onPickCover() },
            contentAlignment = Alignment.Center
        ) {
            if (playerState.currentCoverBitmap != null) {
                Image(
                    bitmap = playerState.currentCoverBitmap!!.asImageBitmap(),
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

        Spacer(Modifier.weight(1f))

        SeekBarControl(
            controller = controller,
            currentPosition = playerState.currentPosition,
            duration = playerState.duration,
            onPositionChange = { viewModel.updatePlayerState(currentPosition = it) }
        )

        Spacer(Modifier.height(8.dp))

        PlayerControls(
            controller = controller,
            isPlaying = playerState.isPlaying,
        )

        Spacer(Modifier.height(8.dp))

        PlaybackSpeedControl(
            controller = controller,
            currentSpeed = playerState.playbackSpeed,
            sleepTimerLabel = playerState.sleepTimerLabel,
            onSleepTimerClick = { showSleepTimerDialog = true }
        )

        Spacer(Modifier.weight(1f))

        // bottom actions
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