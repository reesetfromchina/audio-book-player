package com.ive.audiobookplayer.ui.componenets

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile


@Composable
fun FolderPickerButton(onFolderSelected: (List<Uri>) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { treeUri ->
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val documentFile = DocumentFile.fromTreeUri(context, treeUri)
            val mp3Uris = documentFile?.listFiles()
                ?.filter { it.name?.endsWith(".mp3") == true }
                ?.sortedBy { it.name }
                ?.map { it.uri } ?: emptyList()
            onFolderSelected(mp3Uris)
        }
    }

    Button(
        onClick = { folderPickerLauncher.launch(null) },
        modifier = modifier.fillMaxWidth()
    ) {
        Text("Open Folder")
    }
}