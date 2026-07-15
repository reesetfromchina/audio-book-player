package com.ive.audiobookplayer.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerDialog(
    currentLabel: String,
    onDismiss: () -> Unit,
    onTimerSelected: (Int?) -> Unit,
    onEndChapter: () -> Unit
) {

    val haptic = LocalHapticFeedback.current
    val sleepTime = listOf(5, 15, 30, 45, 60)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp)

        ) {
            Text(
                "Sleep timer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,

            )
            Spacer(Modifier.height(8.dp))

            HorizontalDivider(
                thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant
            )

            sleepTime.forEach { sleep ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTimerSelected(sleep)
                            onDismiss()
                        }
                        .padding(horizontal = 4.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically

                ) {
                    Text(
                        "${sleep} minutes",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }

            TextButton(
                onClick = {
                    onEndChapter()
                    onDismiss()
                }, modifier = Modifier.fillMaxWidth()
            ) {
                Text("End of chapter", color = MaterialTheme.colorScheme.onSurface)

            }
            if (currentLabel.isNotEmpty()) {
                HorizontalDivider()
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTimerSelected(null)
                        onDismiss()
                    }, modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel timer", color = MaterialTheme.colorScheme.error)
                }
            }
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        }
    }

}