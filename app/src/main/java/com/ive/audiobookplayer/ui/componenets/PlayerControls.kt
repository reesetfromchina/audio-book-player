package com.ive.audiobookplayer.ui.componenets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController

@Composable
fun PlayerControls(
    controller: MediaController,
    isPlaying: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Button(onClick = { controller.seekTo(controller.currentPosition - 15000) }) {
            Text("-15s")
        }

        Button(
            onClick = { if (isPlaying) controller.pause() else controller.play() },
            modifier = Modifier.size(120.dp),
        ) {
            Text(if (isPlaying) "Pause" else "Play")
        }

        Button(onClick = { controller.seekTo(controller.currentPosition + 15000) }) {
            Text("+15s")
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Button(onClick = { controller.seekToPrevious() }) {
            Text("Previous track")
        }

        Button(onClick = { controller.seekToNext() }) {
            Text("Next track")
        }
    }
}