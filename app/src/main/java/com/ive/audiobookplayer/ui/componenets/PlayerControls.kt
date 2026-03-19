package com.ive.audiobookplayer.ui.componenets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.ive.audiobookplayer.R

@Composable
fun PlayerControls(
    controller: MediaController,
    isPlaying: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = { controller.seekTo(controller.currentPosition - 15_000) }) {
            Icon(
                (Icons.Default.FastRewind),
                contentDescription = "Back 15 seconds",
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
            onClick = { if (isPlaying) controller.pause() else controller.play() },
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

        IconButton(onClick = { controller.seekTo(controller.currentPosition + 30_000) }) {
            Icon(
                (Icons.Default.FastForward),
                contentDescription = "Forward 30 seconds",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}