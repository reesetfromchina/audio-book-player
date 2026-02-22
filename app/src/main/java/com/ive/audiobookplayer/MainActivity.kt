package com.ive.audiobookplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ive.audiobookplayer.ui.screens.LibraryScreen
import com.ive.audiobookplayer.ui.screens.MainScreen
import com.ive.audiobookplayer.ui.theme.AudioBookTheme
import com.ive.audiobookplayer.viewmodel.AudioViewModel


class MainActivity : ComponentActivity() {

    private var navigateToPlayer = mutableStateOf(false)

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.setCustomCover(it)
        }
    }
    private val viewModel: AudioViewModel by viewModels()
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.scanLibrary(it)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navigateToPlayer.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            AudioBookTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val playerState by viewModel.playerState.collectAsState()
                    val navController = rememberNavController()
                    if (navigateToPlayer.value) {
                        LaunchedEffect(Unit) {
                            navController.navigate("player") {
                                launchSingleTop = true
                            }
                            navigateToPlayer.value = false
                        }
                        navigateToPlayer.value = false
                    }

                    NavHost(navController = navController, startDestination = "library") {
                        composable("library") {
                            LibraryScreen(
                                viewModel = viewModel,
                                audiobooks = playerState.audiobooks,
                                bookmarks = playerState.bookmarks,
                                onAudiobookSelected = { book ->
                                    viewModel.playAudiobook(book)
                                    navController.navigate("player")
                                },
                                onPickFolder = { folderPickerLauncher.launch(null) },
                                isLibraryLoaded = playerState.isLibraryLoaded,
                                isScanning = playerState.isScanning,
                                isBackgroundScanning = playerState.isBackgroundScanning,
                                libraryGroups = playerState.libraryGroups
                            )
                        }
                        composable("player") {
                            MainScreen(
                                viewModel = viewModel,
                                onBackToLibrary = { navController.popBackStack() },
                                onPickCover = { imagePicker.launch(arrayOf("image/*")) }
                            )
                        }
                    }
                }
            }
        }
    }
}
