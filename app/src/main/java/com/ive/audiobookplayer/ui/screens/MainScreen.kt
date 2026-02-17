package com.ive.audiobookplayer.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.ive.audiobookplayer.ui.componenets.FolderPickerButton
import com.ive.audiobookplayer.ui.componenets.PlaybackSpeedControl
import com.ive.audiobookplayer.ui.componenets.PlayerControls
import com.ive.audiobookplayer.ui.componenets.SeekBarControl
import com.ive.audiobookplayer.viewmodel.AudioViewModel
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    viewModel: AudioViewModel,
    onFolderPicked: (List<Uri>) -> Unit
) {
    val playerState by viewModel.playerState.collectAsState()
    val controller = playerState.controller

    if (controller == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Connecting to Audio service...")
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
                    duration = controller.duration.coerceAtLeast(0L)
                )
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                viewModel.updatePlayerState(playbackSpeed = playbackParameters.speed)
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

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Now playing",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = playerState.currentTitle,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )

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

        FolderPickerButton(onFolderSelected = onFolderPicked)

        Button(
            onClick = { /* TODO: Implement last session */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Resume Last Session")
        }
    }
}