package com.ive.audiobookplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ive.audiobookplayer.utils.TimeFormatter.formatTime
import com.ive.audiobookplayer.viewmodel.AudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    audiobookId: String,
    viewModel: AudioViewModel,
    navController: NavController,
    onBack: () -> Unit,
) {
    val notes by viewModel
        .getNotesForAudiobook(audiobookId)
        .collectAsState(initial = emptyList())

    val libraryState by viewModel.libraryState.collectAsState()
    val audiobook = libraryState.audiobooks.find { it.id == audiobookId } ?: return
    val playerState by viewModel.playerState.collectAsState()

    val groupedNotes = remember(notes) {
        notes.groupBy { it.chapterIndex }
            .toSortedMap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(audiobook.title)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        when {
            notes == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            groupedNotes.isNullOrEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "You have no notes for this audiobook broski")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    groupedNotes.forEach { (chapterIndex, chapterNotes) ->

                        item {
                            Text(
                                text = "Chapter ${chapterIndex + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )
                        }

                        items(chapterNotes, key = { it.id }) { note ->
                            val dismissStatie = rememberSwipeToDismissBoxState()

                            SwipeToDismissBox(
                                state = dismissStatie,
                                enableDismissFromStartToEnd = false,
                                onDismiss = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.deleteNote(note)
                                    }
                                },
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .padding(end = 16.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Note",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable {
                                            if (playerState.currentAudiobook?.id == audiobookId) {
                                                viewModel.seekTo(note.chapterIndex, note.positionMs)
                                            } else {
                                                viewModel.playAudiobookCustom(audiobook, note.chapterIndex, note.positionMs)
                                            }

                                            navController.navigate("player"){
                                                popUpTo("player") { inclusive = true}
                                            }
                                        }.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = note.noteText,
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text = formatTime(note.positionMs),
                                        style =  MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}