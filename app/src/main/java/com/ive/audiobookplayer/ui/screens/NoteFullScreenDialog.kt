package com.ive.audiobookplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ive.audiobookplayer.utils.formatDurationSeconds

@Composable
fun NoteFullScreenDialog(
    chapterTitle: String,
    positionMs: Long,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var noteText by remember { mutableStateOf("") }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val characterLimit = 500
    val formattedPosition = remember(positionMs) { formatDurationSeconds(positionMs) }

    val attemptDismiss = {
        if (noteText.isNotBlank()) {
            showDiscardConfirm = true
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = attemptDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = attemptDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }

                    TextButton(
                        onClick = { onSave(noteText) },
                        enabled = noteText.isNotBlank()
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }

                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "At $formattedPosition",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { if (it.length <= characterLimit) noteText = it },
                    placeholder = { Text("Write down a thought...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                Text(
                    text = "${noteText.length} / $characterLimit",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (noteText.length >= characterLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(vertical = 8.dp)
                )
            }
        }
    }


    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard note?") },
            text = { Text("Your note hasn't been saved yet.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onDismiss()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("Keep writing") }
            }
        )
    }
}