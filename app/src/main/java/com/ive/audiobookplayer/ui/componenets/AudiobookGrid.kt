package com.ive.audiobookplayer.ui.componenets

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry

@Composable
fun AudiobookGrid(
    audiobooks: List<Audiobook>,
    bookmarks: Map<String, BookmarkEntry>,
    onAudiobookSelected: (Audiobook) -> Unit,
    resolveCover: (Audiobook) -> Uri?,
    onResetProgress: (Audiobook) -> Unit,
    onFinishedAudiobook: (Audiobook) -> Unit,
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(audiobooks, key = { it.id }) { book ->
            AudiobookGridCard(
                audiobook = book,
                bookmark = bookmarks[book.id],
                coverUri = resolveCover(book),
                onClick = { onAudiobookSelected(book) },
                onResetProgress = {onResetProgress(book)},
                onFinishedAudiobook = {onFinishedAudiobook(book)}
            )
        }
    }
}
