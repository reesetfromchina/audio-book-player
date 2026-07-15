package com.ive.audiobookplayer.ui.library

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.LibraryState
import com.ive.audiobookplayer.viewmodel.AudioViewModel
import com.ive.audiobookplayer.viewmodel.AudioViewModel.LibraryEvent

@Composable
fun LibraryScreen(
    viewModel: AudioViewModel,
    library: LibraryState,
    onAudiobookSelected: (Audiobook) -> Unit,
    onPickFolder: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    navController: NavController
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current


    val resolveCover = remember(viewModel) {
        { audiobook: Audiobook -> viewModel.resolveCoverUri(audiobook) }
    }

    val actions = remember(viewModel, navController, onAudiobookSelected) {
        LibraryActions(
            onAudiobookSelected = onAudiobookSelected,
            onResetProgress = { viewModel.resetProgress(it) },
            onFinishedAudiobook = { viewModel.markAsFinished(it) },
            onViewNotes = { navController.navigate("notes/${it.id}") }
        )
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.ImportSuccess -> {
                    Toast.makeText(context, "Import successful", Toast.LENGTH_SHORT).show()
                }

                is LibraryEvent.ImportError -> {
                    Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
                }

                is LibraryEvent.ExportSuccess -> {
                    Toast.makeText(context, "Export successful", Toast.LENGTH_SHORT).show()
                }

                is LibraryEvent.ExportError -> {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        LibraryTopBar(
            isBackgroundScanning = library.isBackgroundScanning,
            isGridView = library.isGridView,
            onRescan = { viewModel.rescan() },
            onToggleView = { viewModel.toggleLibraryView() },
            onSettings = { navController.navigate("settings") },
            onPickFolder = onPickFolder,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup
        )

        Spacer(Modifier.height(2.dp))

        LibraryBody(
            library = library,
            listState = listState,
            resolveCover = resolveCover,
            onPickFolder = onPickFolder,
            actions = actions,
        )

    }
}
@Composable
fun LibrarySectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

