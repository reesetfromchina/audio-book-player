package com.ive.audiobookplayer.ui.componenets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun SleepTimerDialog(
    currentLabel: String,
    onDismiss: () -> Unit,
    onTimerSelected: (Int?) -> Unit,
    onEndChapter: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Sleep timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentLabel.isNotEmpty()) {
                    Text(
                        "Active: $currentLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    HorizontalDivider()
                }
                listOf(15, 30, 45, 60).forEach { minutes ->
                    TextButton(
                        onClick = {
                            onTimerSelected(minutes)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("$minutes minutes")
                    }
                }
                TextButton(
                    onClick = {
                        onEndChapter()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("End of chapter")
                }
                if (currentLabel.isNotEmpty()) {
                    HorizontalDivider()
                    TextButton(
                        onClick = {
                            onTimerSelected(null)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel timer", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}