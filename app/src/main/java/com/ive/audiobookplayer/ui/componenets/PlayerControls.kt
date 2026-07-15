package com.ive.audiobookplayer.ui.componenets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController

@Composable
fun PlayerControls(
    controller: MediaController,
    isPlaying: Boolean,
    skipBackSeconds: Int,
    skipForwardSeconds: Int,
    onPlayPauseClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = { controller.seekTo(controller.currentPosition - skipBackSeconds * 1000L) }) {
            Icon(
                (Icons.Default.FastRewind),
                contentDescription = "Back ${skipBackSeconds*1000L} seconds",
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(onClick = { controller.seekToPrevious() }) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous chapter",
                modifier = Modifier.size(36.dp)
            )
        }

        FilledIconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(72.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(36.dp)
            )
        }

        IconButton(onClick = { controller.seekToNext() }) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next chapter",
                modifier = Modifier.size(36.dp)
            )
        }

        IconButton(onClick = { controller.seekTo(controller.currentPosition + skipForwardSeconds*1000L) }) {
            Icon(
                (Icons.Default.FastForward),
                contentDescription = "Forward ${skipForwardSeconds*1000L} seconds",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}