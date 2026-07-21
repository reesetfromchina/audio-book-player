package com.ive.audiobookplayer.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun LibraryOverflowMenu(
    onRescan: () -> Unit,
    onSettings: () -> Unit,
    onPickFolder: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
){
    var expanded by remember { mutableStateOf(false) }


    Box{
        IconButton(onClick = {expanded = true}) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = {expanded = false}) {
            DropdownMenuItem(
                text = { Text("Rescan library") },
                onClick = {expanded = false; onRescan()},
                leadingIcon = {Icon(Icons.Default.Refresh, null)}
            )

            DropdownMenuItem(
                text = { Text("Change folder") },
                onClick = {expanded = false; onPickFolder()},
                leadingIcon = {Icon(Icons.Default.FolderOpen, null)}
            )
            HorizontalDivider()
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Export backup") },
                onClick = { expanded = false; onExportBackup() },
                leadingIcon = { Icon(Icons.Default.Upload, null) }
            )
            DropdownMenuItem(
                text = { Text("Import backup") },
                onClick = { expanded = false; onImportBackup() },
                leadingIcon = { Icon(Icons.Default.Download, null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = { expanded = false; onSettings() },
                leadingIcon = { Icon(Icons.Default.Settings, null) }
            )

        }
    }

}