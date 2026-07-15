package com.ive.audiobookplayer.ui.components

import android.net.Uri
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.ui.library.AudiobookInfoSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuComponent(
    onDismissRequest: () -> Unit,
    showMenu: Boolean, audiobook: Audiobook,
    bookmarkEntry: BookmarkEntry?,
    onResetProgress: () -> Unit,
    onFinishedProgress: () -> Unit,
    coverUri: Uri?,
    onViewNotes: () -> Unit,
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text("Reset progress") },
            onClick = {
                onResetProgress()
                onDismissRequest()
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Mark as finished") },
            onClick = {
                onFinishedProgress()
                onDismissRequest()
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("View notes") },
            onClick = {
                onViewNotes()
                onDismissRequest()
            }
        )

        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Book info") },
            onClick = {
                showBottomSheet = true
                onDismissRequest()
            }
        )
    }
    if (showBottomSheet) {
        AudiobookInfoSheet(
            audiobook = audiobook,
            bookmarkEntry = bookmarkEntry,
            overrideCoverUri = coverUri,
            onDismiss = { showBottomSheet = false }
        )
    }

}


