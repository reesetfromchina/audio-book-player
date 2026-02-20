package com.ive.audiobookplayer.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.ive.audiobookplayer.ui.componenets.PlaybackSpeedControl
import com.ive.audiobookplayer.ui.componenets.PlayerControls
import com.ive.audiobookplayer.ui.componenets.SeekBarControl
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

    if (controller == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }


    // attach player listener
    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {

                viewModel.updatePlayerState(isPlaying = isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                viewModel.updatePlayerState(
                    currentTitle = mediaItem?.mediaMetadata?.title?.toString() ?: "z1Unknown",
                    duration = controller.duration.coerceAtLeast(0L),
                    currentChapterIndex = controller.currentMediaItemIndex,
                )
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                viewModel.updatePlayerState(playbackSpeed = playbackParameters.speed)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    viewModel.updatePlayerState(
                        duration = controller.duration.coerceAtLeast(0L)
                    )

                    // update current chapter duration
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = playerState.currentBookTitle.ifEmpty { playerState.currentTitle },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,

        )

        if ((playerState.currentAudiobook?.chapters?.size ?: 0) > 1) {
            Text(
                text = "Chapter ${playerState.currentChapterIndex + 1} of ${playerState.currentAudiobook?.chapters?.size}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = playerState.currentTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }


        Box(
            modifier = Modifier
                .size(240.dp)
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
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Change cover",
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap to add cover",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        SeekBarControl(
            controller = controller,
            currentPosition = playerState.currentPosition,
            duration = playerState.duration,
            onPositionChange = { viewModel.updatePlayerState(currentPosition = it) }
        )

        PlayerControls(
            controller = controller,
            isPlaying = playerState.isPlaying,
        )

        PlaybackSpeedControl(
            controller = controller,
            currentSpeed = playerState.playbackSpeed
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onBackToLibrary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Library")
        }

    }
}