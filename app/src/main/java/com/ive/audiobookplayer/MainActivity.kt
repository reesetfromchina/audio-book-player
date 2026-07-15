package com.ive.audiobookplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ive.audiobookplayer.repository.ThemeMode
import com.ive.audiobookplayer.ui.settings.SettingsScreen
import com.ive.audiobookplayer.ui.library.LibraryScreen
import com.ive.audiobookplayer.ui.MainScreen
import com.ive.audiobookplayer.ui.notes.NotesScreen
import com.ive.audiobookplayer.ui.theme.AudioBookTheme
import com.ive.audiobookplayer.viewmodel.AudioViewModel
import com.ive.audiobookplayer.viewmodel.SettingsViewModel


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
            //todo fix
        }
    }
    private val viewModel: AudioViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
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

    private val backupExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    private val backupImportLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.importBackup(it) }
        }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navigateToPlayer.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            !viewModel.startupComplete.value
        }
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            try {
                splashScreenView.iconView
                    .animate()
                    .setDuration(300)
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .alpha(0f)
                    .withEndAction { splashScreenView.remove() }
                    .start()
            } catch (e: Exception) {
                splashScreenView.remove()
            }
        }

        enableEdgeToEdge()
        setContent {

            val playerState by viewModel.playerState.collectAsState()

            val themeMode by settingsViewModel.settingsState.collectAsState()
            val darkTheme = when (themeMode.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            AudioBookTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {

                    val startupComplete by viewModel.startupComplete.collectAsState()


                    if(!startupComplete){
                        return@Surface
                    }

                    val playerState by viewModel.playerState.collectAsState()
                    val libraryState by viewModel.libraryState.collectAsState()
                    val navController = rememberNavController()

                    val startDestination = remember {
                        if(playerState.currentAudiobook != null) "player" else "library"
                    }

                    LaunchedEffect(navigateToPlayer.value) {
                        if(navigateToPlayer.value){
                            navController.navigate("player") { launchSingleTop = true}
                            navigateToPlayer.value = false
                        }
                    }

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("library") {
                            LibraryScreen(
                                viewModel = viewModel,
                                onAudiobookSelected = { book ->
                                    viewModel.playAudiobook(book)
                                    navController.navigate("player")
                                },
                                navController = navController,
                                onPickFolder = { folderPickerLauncher.launch(null) },
                                library = libraryState,
                                onExportBackup = {backupExportLauncher.launch("audiobook_backup.json")},
                                onImportBackup = { backupImportLauncher.launch(arrayOf("application/json"))},
                            )
                        }
                        composable("player") {
                            MainScreen(
                                viewModel = viewModel,
                                settingsViewModel = settingsViewModel,
                                onBackToLibrary = {
                                    if (!navController.popBackStack()){
                                        navController.navigate("library")
                                    }
                                },
                                onPickCover = { imagePicker.launch(arrayOf("image/*")) },
                                navController = navController,
                            )}

                        composable("settings"){
                            SettingsScreen(
                                settingsViewModel = settingsViewModel,
                                onBack = {navController.popBackStack()}
                            )
                        }

                        composable(
                            route = "notes/{audiobookId}",
                            arguments = listOf(navArgument("audiobookId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val audiobookId = backStackEntry.arguments?.getString("audiobookId") ?: return@composable
                            NotesScreen(
                                audiobookId = audiobookId,
                                viewModel = viewModel,
                                navController = navController,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
