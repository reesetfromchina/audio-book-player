package com.ive.audiobookplayer.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.viewmodel.AudioViewModel

@Composable
fun LibraryScreen(
    viewModel: AudioViewModel,
    audiobooks: List<Audiobook>,
    bookmarks: Map<String, BookmarkEntry>,
    onAudiobookSelected: (Audiobook) -> Unit,
    onPickFolder: () -> Unit,
    isLibraryLoaded: Boolean,
    isScanning: Boolean
) {
    val coverCache by viewModel.coverCache.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("My Library", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        if (audiobooks.isNotEmpty()) {
            Button(onClick = onPickFolder, modifier = Modifier.fillMaxWidth()) {
                Text("Change Folder")
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            isScanning -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Scanning library...")
                    }
                }
            }
            !isLibraryLoaded || audiobooks.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No audiobooks found.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onPickFolder) {
                            Text("Pick Library Folder")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(audiobooks) { book ->
                        LaunchedEffect(book.id) {
                            viewModel.loadCoverForBook(book)
                        }
                        AudiobookCard(
                            audiobook = book,
                            bookmark = bookmarks[book.id],
                            coverBitmap = coverCache[book.id],
                            onClick = { onAudiobookSelected(book) }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun AudiobookCard(
    audiobook: Audiobook,
    bookmark: BookmarkEntry?,
    coverBitmap: Bitmap? = null,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(audiobook.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${audiobook.chapters.size} chapter(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (bookmark != null) {
                    val minutes = (bookmark.positionMs / 1000) / 60
                    val seconds = (bookmark.positionMs / 1000) % 60
                    Text(
                        "Resume: Ch.${bookmark.chapterIndex + 1} at %02d:%02d".format(minutes, seconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}