package com.ive.audiobookplayer

import android.annotation.SuppressLint
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.benchmark.traceprocessor.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ive.audiobookplayer.ui.theme.AudioBookPlayerTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var player : ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // init exo
        player = ExoPlayer.Builder(this).build()



        setContent {
            var currentTitle by remember { mutableStateOf("No File Selected") }

            var currentPosition by remember { mutableLongStateOf(0L) }
            var trackDuration by remember { mutableLongStateOf(0L) }

            var isPlaying by remember { mutableStateOf(false) }

            val folderPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri: Uri? ->
                uri?.let { handleFolderSelected(it) }
            }

            DisposableEffect(Unit) {
                val listener = object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        currentTitle = mediaItem?.mediaMetadata?.title?.toString()
                            ?: mediaItem?.requestMetadata?.mediaUri?.lastPathSegment
                                    ?: "Unknown Track"
                        trackDuration = player?.duration ?: 0L
                    }

                    override fun onIsPlayingChanged(isPlayingState: Boolean) {
                        isPlaying = isPlayingState
                        if (isPlayingState) {
                            trackDuration = player?.duration?.coerceAtLeast(0L) ?: 0L
                        }
                    }
                }

                player?.addListener(listener)
                onDispose { player?.removeListener(listener) }
            }

            LaunchedEffect(isPlaying) {
                while (isPlaying) {
                    currentPosition = player?.currentPosition ?: 0L
                    delay(1000)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = { folderPickerLauncher.launch(null) }) {
                    Text("Select Folder")
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = currentPosition.toFloat(),
                    valueRange = 0f..(trackDuration.toFloat().coerceAtLeast(1f)),
                    onValueChange = { newPosition ->
                        currentPosition = newPosition.toLong()
                    },
                    onValueChangeFinished = {
                        player?.seekTo(currentPosition)
                    }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = formatTime(currentPosition))
                    Text(text = formatTime(trackDuration))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { player?.seekToPrevious() }) {
                        Text("Prev")
                    }

                    Button(onClick = {
                        if (isPlaying) player?.pause() else player?.play()
                    }) {
                        Text(if (isPlaying) "Pause" else "Play")
                    }

                    Button(onClick = { player?.seekToNext() }) {
                        Text("Next")
                    }
                }
            }
        }

       }
    @SuppressLint("DefaultLocale")
    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }


    private fun handleFolderSelected(uri: Uri){
        val context = this
        val documentFile = DocumentFile.fromTreeUri(context, uri)

        val mp3Files = documentFile?.listFiles()
            ?.filter { it.name?.endsWith(".mp3") == true }
            ?.sortedBy { it.name }
            ?:emptyList()

        if(mp3Files.isNotEmpty()){
            player?.clearMediaItems() // clear current playlist

            mp3Files.forEach { file -> val mediaItem = MediaItem.fromUri(file.uri)
            player?.addMediaItem(mediaItem)}
        }
        player?.prepare()
        player?.play()
        Toast.makeText(this, "Playing ${mp3Files.size} tracks", Toast.LENGTH_SHORT).show()

    }


    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
