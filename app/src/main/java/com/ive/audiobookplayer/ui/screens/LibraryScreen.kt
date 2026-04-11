package com.ive.audiobookplayer.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.protobuf.LazyStringArrayList.emptyList
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ive.audiobookplayer.database.NoteEntity
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.model.LibraryState
import com.ive.audiobookplayer.ui.componenets.AudiobookGrid
import com.ive.audiobookplayer.ui.componenets.DropdownMenuComponent
import com.ive.audiobookplayer.ui.componenets.NotesBottomSheet
import com.ive.audiobookplayer.utils.calculateProgress
import com.ive.audiobookplayer.viewmodel.AudioViewModel
import com.ive.audiobookplayer.viewmodel.AudioViewModel.LibraryEvent
import kotlin.collections.emptyList

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

    var showNotesSheet by remember { mutableStateOf(false) }
    var selectedAudiobookForNotes by remember { mutableStateOf<Audiobook?>(null) }

    val resolveCover = remember(viewModel) {
        { audiobook: Audiobook -> viewModel.resolveCoverUri(audiobook) }
    }

    var selectedNoteToPlay by remember { mutableStateOf<NoteEntity?>(null) }

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

    LaunchedEffect(selectedNoteToPlay) {
        val note = selectedNoteToPlay ?: return@LaunchedEffect

        viewModel.playAudiobookCustom(
            selectedAudiobookForNotes!!,
            note.chapterIndex,
            note.positionMs
        )

        navController.navigate("player")

        selectedNoteToPlay = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Library", style = MaterialTheme.typography.headlineSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (library.isBackgroundScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
                IconButton(onClick = { viewModel.rescan() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rescan library")
                }
                IconButton(onClick = { viewModel.toggleLibraryView() }) {
                    Icon(
                        imageVector = if (library.isGridView) Icons.Default.ViewList
                        else Icons.Default.GridView,
                        contentDescription = null
                    )
                }
                IconButton(onClick = onPickFolder) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Change folder")
                }
                var showBackupMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showBackupMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showBackupMenu,
                        onDismissRequest = { showBackupMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export backup") },
                            onClick = {
                                showBackupMenu = false
                                onExportBackup()
                            },
                            leadingIcon = { Icon(Icons.Default.Upload, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Import backup") },
                            onClick = {
                                showBackupMenu = false
                                onImportBackup()
                            },
                            leadingIcon = { Icon(Icons.Default.Download, null) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            library.isInitializing -> Box(Modifier.fillMaxSize())

            library.isScanning -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Scanning library...")
                    }
                }
            }

            library.audiobooks.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("No audiobooks found", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Pick a folder containing your audiobooks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onPickFolder) { Text("Pick folder") }
                    }
                }
            }

            library.isGridView -> {
                val sortedBooks = remember(library.groups) {
                    library.groups.inProgress +
                            library.groups.unplayed +
                            library.groups.finished
                }
                AudiobookGrid(
                    audiobooks = sortedBooks,
                    bookmarks = library.bookmarks,
                    resolveCover = resolveCover,
                    onAudiobookSelected = onAudiobookSelected,
                    onResetProgress = { viewModel.resetProgress(it) },
                    onFinishedAudiobook = { viewModel.markAsFinished(it) },
                    onViewNotes = {audiobook ->
                        selectedAudiobookForNotes = audiobook
                        showNotesSheet = true
                    }
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    librarySection(
                        title = "In Progress",
                        books = library.groups.inProgress,
                        bookmarks = library.bookmarks,
                        resolveCover = resolveCover,
                        onAudiobookSelected = onAudiobookSelected,
                        onResetProgress = { viewModel.resetProgress(it) },
                        onFinishedAudiobook = { viewModel.markAsFinished(it) }


                    )
                    librarySection(
                        title = "Unplayed",
                        books = library.groups.unplayed,
                        bookmarks = library.bookmarks,
                        resolveCover = resolveCover,
                        onAudiobookSelected = onAudiobookSelected,
                        onResetProgress = { viewModel.resetProgress(it) },
                        onFinishedAudiobook = { viewModel.markAsFinished(it) }


                    )
                    librarySection(
                        title = "Finished",
                        books = library.groups.finished,
                        bookmarks = library.bookmarks,
                        resolveCover = resolveCover,
                        onAudiobookSelected = onAudiobookSelected,
                        isFinished = true,
                        onResetProgress = { viewModel.resetProgress(it) },
                        onFinishedAudiobook = { viewModel.markAsFinished(it) }

                    )
                }
            }

        }

        if (showNotesSheet && selectedAudiobookForNotes != null) {
            val notes by viewModel.getNotesForAudiobook(selectedAudiobookForNotes!!.id)
                .collectAsState(initial = emptyList())

            NotesBottomSheet(
                audiobook = selectedAudiobookForNotes!!,
                notes = notes as List<NoteEntity>,
//                Log.d("Notes", notes.forEach { note -> note?.toString() })
                onDismiss = {
                    showNotesSheet = false
                    selectedAudiobookForNotes = null
                },
                onNoteClicked = { note ->
                    selectedNoteToPlay = note
                    showNotesSheet = false
                },

                onDeleteNote = { note ->
//                    viewModel.deleteNote(note)
                },

            )
        }
    }
}

private fun LazyListScope.librarySection(
    title: String,
    books: List<Audiobook>,
    bookmarks: Map<String, BookmarkEntry>,
    resolveCover: (Audiobook) -> Uri?,
    onAudiobookSelected: (Audiobook) -> Unit,
    isFinished: Boolean = false,
    onResetProgress: (Audiobook) -> Unit,
    onFinishedAudiobook: (Audiobook) -> Unit,
) {
    if (books.isEmpty()) return
    val key = title.lowercase().replace(" ", "_")

    item(key = "header_$key") {
        LibrarySectionHeader(title = title, count = books.size)
    }
    items(items = books, key = { it.id }) { book ->
        AudiobookCard(
            audiobook = book,
            bookmark = bookmarks[book.id],
            coverUri = resolveCover(book),
            isFinished = isFinished,
            onClick = { onAudiobookSelected(book) },
            onResetProgress = { onResetProgress(book) },
            onFinishedAudiobook = { onFinishedAudiobook(book) },
            onViewNote = { }
        )
    }
    item(key = "spacer_$key") {
        Spacer(Modifier.height(8.dp))
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
    HorizontalDivider()
    Spacer(Modifier.height(4.dp))
}




















@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudiobookCard(
    audiobook: Audiobook,
    bookmark: BookmarkEntry?,
    coverUri: Uri?,
    isFinished: Boolean = false,
    onClick: () -> Unit,
    onResetProgress: () -> Unit,
    onFinishedAudiobook: () -> Unit,
    onViewNote: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    val progress = remember(audiobook, bookmark) { calculateProgress(audiobook, bookmark) }
    var expanded by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var selectedAudiobookForNotes by remember { mutableStateOf<Audiobook?>(null) }


    Box{
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        expanded = true
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    }),
            colors = CardDefaults.cardColors(
                containerColor = if (isFinished)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {

            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    if (isFinished) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Finished",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }


                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        audiobook.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isFinished)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${audiobook.chapters.size} chapter(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (bookmark != null) {
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (progress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (isFinished)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val minutes = (bookmark.positionMs / 1000) / 60
                            val seconds = (bookmark.positionMs / 1000) % 60
                            Text(
                                if (isFinished) "Finished"
                                else "Ch.${bookmark.chapterIndex + 1} — %02d:%02d".format(
                                    minutes, seconds
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "%.0f%%".format(progress),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFinished)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        DropdownMenuComponent(
            showMenu = expanded,
            onDismissRequest = { expanded = false },
            audiobook = audiobook,
            bookmarkEntry = bookmark,
            onResetProgress = onResetProgress,
            onFinishedProgress = onFinishedAudiobook,
            coverUri = coverUri,
            onViewNotes = onViewNote,
        )

    }
}