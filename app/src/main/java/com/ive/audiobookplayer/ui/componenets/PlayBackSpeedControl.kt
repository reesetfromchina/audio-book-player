package com.ive.audiobookplayer.ui.componenets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController

@Composable
fun PlaybackSpeedControl(
    controller: MediaController,
    currentSpeed: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text("Playback Speed: ${"%.2f".format(currentSpeed)}x")
        Slider(
            value = currentSpeed,
            valueRange = 0.5f..2.5f,
            onValueChange = { controller.setPlaybackSpeed(it) }
        )
    }
}