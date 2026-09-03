package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.home.ChromeDownloadDialog
import com.example.ui.home.DeveloperConsoleDialog
import com.example.ui.home.FileDetailsSheet
import com.example.ui.home.HomeScreen
import com.example.ui.home.UploadDialog
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                CloudFireApp(viewModel)
            }
        }
    }
}

@Composable
fun CloudFireApp(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val files by viewModel.filteredFiles.collectAsStateWithLifecycle()
    val storageUsedBytes by viewModel.storageUsedBytes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val uploadStatus by viewModel.uploadStatus.collectAsStateWithLifecycle()
    val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()
    val activeFileAction by viewModel.activeFileAction.collectAsStateWithLifecycle()
    val activeChromeLinkFile by viewModel.activeChromeLinkFile.collectAsStateWithLifecycle()
    val showDeveloperConsole by viewModel.showDeveloperConsole.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentUser != null,
                label = "auth_crossfade"
            ) { isAuthenticated ->
                if (isAuthenticated && currentUser != null) {
                    HomeScreen(
                        user = currentUser!!,
                        files = files,
                        storageUsedBytes = storageUsedBytes,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        serverInfo = serverInfo,
                        onSearchChange = viewModel::setSearchQuery,
                        onCategorySelect = viewModel::setSelectedCategory,
                        onUploadClick = viewModel::uploadFile,
                        onFileClick = viewModel::openFileAction,
                        onQuickChromeLink = viewModel::openChromeLinkDialog,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onDeleteFile = viewModel::deleteFile,
                        onSignOut = viewModel::signOut,
                        onOpenDeveloperConsole = viewModel::openDeveloperConsole
                    )
                } else {
                    AuthScreen(
                        onSignIn = viewModel::signIn,
                        onSignUp = viewModel::signUp,
                        onGuestSignIn = viewModel::signInAsGuest,
                        onDeveloperSignIn = viewModel::signInAsDeveloper
                    )
                }
            }

            // Upload Progress Dialog
            UploadDialog(
                status = uploadStatus,
                onDismiss = viewModel::dismissUpload
            )

            // File Details & Chrome Share Bottom Sheet
            activeFileAction?.let { file ->
                FileDetailsSheet(
                    file = file,
                    directDownloadUrl = viewModel.getDirectDownloadUrl(file.id),
                    webPageUrl = viewModel.getWebPageUrl(file.id),
                    onDismiss = viewModel::closeFileAction,
                    onDelete = viewModel::deleteFile,
                    onToggleFavorite = viewModel::toggleFavorite
                )
            }

            // Dedicated Chrome Auto-Download Dialog
            activeChromeLinkFile?.let { file ->
                ChromeDownloadDialog(
                    file = file,
                    directDownloadUrl = viewModel.getDirectDownloadUrl(file.id),
                    onDismiss = viewModel::closeChromeLinkDialog
                )
            }

            // Developer Superuser Console Dialog (devlopertharv@gmail.com / tharvthala07)
            if (showDeveloperConsole) {
                DeveloperConsoleDialog(
                    user = currentUser,
                    onDismiss = viewModel::closeDeveloperConsole,
                    onCreateTestFile = viewModel::createDeveloperTestFile,
                    onDeleteAllFiles = viewModel::deleteAllFiles,
                    onSeedStarterFiles = viewModel::seedStarterFiles
                )
            }
        }
    }
}
