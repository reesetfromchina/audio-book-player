package com.ive.audiobookplayer

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Intent
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class  MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: AudioViewModel = viewModel()

            val controller by viewModel.playerController.collectAsState()

            if (controller == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Connecting to Audio Service...")
                }
            } else {
                AudioPlayerScreen(
                    controller = controller!!,
                    viewModel = viewModel,
                    onFolderPicked = { uris -> viewModel.playPlaylist(uris) }
                )
            }
        }
    }
}


@Composable
fun AudioPlayerScreen(
    controller: MediaController,
    viewModel: AudioViewModel,
    onFolderPicked: (List<Uri>) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // ui states
    var isPlaying by remember { mutableStateOf(controller.isPlaying) }
    var currentTitle by remember { mutableStateOf(controller.currentMediaItem?.mediaMetadata?.title?.toString() ?: "No Track Playing") }
    var currentPosition by remember { mutableLongStateOf(controller.currentPosition) }
    var duration by remember { mutableLongStateOf(controller.duration.coerceAtLeast(0L)) }
    var playbackSpeed by remember { mutableFloatStateOf(controller.playbackParameters.speed) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = controller.currentPosition
            duration = controller.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    // listener for metadata and play/pause changes
    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                isPlaying = isPlayingState
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentTitle = mediaItem?.mediaMetadata?.title?.toString() ?: "Unknown"
                duration = controller.duration.coerceAtLeast(0L)
            }
            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                playbackSpeed = playbackParameters.speed
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { treeUri ->
            context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val documentFile = DocumentFile.fromTreeUri(context, treeUri)
            val mp3Uris = documentFile?.listFiles()
                ?.filter { it.name?.endsWith(".mp3") == true }
                ?.sortedBy { it.name }
                ?.map { it.uri } ?: emptyList()
            onFolderPicked(mp3Uris)
        }
    }


    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Now Playing", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = currentTitle, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, maxLines = 2)

        Spacer(modifier = Modifier.weight(1f))

        // seekbar
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Slider(
                value = currentPosition.toFloat(),
                valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                onValueChange = { currentPosition = it.toLong() },
                onValueChangeFinished = { controller.seekTo(currentPosition) }
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(currentPosition), style = MaterialTheme.typography.bodySmall)
                Text(formatTime(duration), style = MaterialTheme.typography.bodySmall)
            }
        }

        // main controls
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Button(onClick = { controller.seekTo(controller.currentPosition - 15000) }) { Text("-15s") }

            Button(
                onClick = { if (isPlaying) controller.pause() else controller.play() },
                modifier = Modifier.size(80.dp),
                shape = CircleShape
            ) {
                Text(if (isPlaying) "Pause" else "Play")
            }

            Button(onClick = { controller.seekTo(controller.currentPosition + 15000) }) { Text("+15s") }
        }

        // prev/next
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { controller.seekToPrevious() }) { Text("Previous Track") }
            Button(onClick = { controller.seekToNext() }) { Text("Next Track") }
        }

        // speed slider
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Playback Speed: ${"%.2f".format(playbackSpeed)}x", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = playbackSpeed,
                valueRange = 0.5f..2.5f,
                onValueChange = { controller.setPlaybackSpeed(it) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = { folderPickerLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
            Text("Open Folder")
        }

        Button(
//            onClick = { viewModel.loadLastSession() },
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Resume Last Session")
        }
    }
}
private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

class AudioViewModel(application: Application) : AndroidViewModel(application) {

    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _playerController = MutableStateFlow<MediaController?>(null)
    val playerController = _playerController.asStateFlow()

    init {
        startServiceConnection()
    }

    private fun startServiceConnection() {
        val context = getApplication<Application>()

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            _playerController.value = controllerFuture?.get()
        }, MoreExecutors.directExecutor())
    }

    fun playPlaylist(uris: List<Uri>) {
        val controller = _playerController.value ?: return

        val mediaItems = uris.map { uri ->
            val fileName = uri.lastPathSegment ?: "Unknown Track"

            MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(fileName)
                        .build()
                )
                .build()
        }

        controller.setMediaItems(mediaItems)
        controller.prepare()
        controller.play()
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
