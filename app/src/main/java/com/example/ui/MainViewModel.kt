package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CloudFile
import com.example.data.model.FileCategory
import com.example.data.repository.AuthRepository
import com.example.data.repository.FileRepository
import com.example.data.repository.UserProfile
import com.example.server.LocalFileServer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UploadStatus {
    object Idle : UploadStatus
    data class Uploading(val fileName: String, val progress: Float, val speedText: String) : UploadStatus
    data class Completed(val file: CloudFile, val directLink: String) : UploadStatus
    data class Error(val message: String) : UploadStatus
}

data class ServerInfo(
    val isRunning: Boolean = true,
    val port: Int = 8080,
    val baseUrl: String = "http://localhost:8080"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val authRepo = AuthRepository(application)
    private val fileRepo = FileRepository(application, database.cloudFileDao())

    val currentUser: StateFlow<UserProfile?> = authRepo.currentUser

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(FileCategory.ALL)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val uploadStatus = _uploadStatus.asStateFlow()

    private val _serverInfo = MutableStateFlow(
        ServerInfo(
            isRunning = true,
            port = LocalFileServer.getPort(),
            baseUrl = LocalFileServer.getBaseUrl()
        )
    )
    val serverInfo = _serverInfo.asStateFlow()

    private val _activeFileAction = MutableStateFlow<CloudFile?>(null)
    val activeFileAction = _activeFileAction.asStateFlow()

    private val _activeChromeLinkFile = MutableStateFlow<CloudFile?>(null)
    val activeChromeLinkFile = _activeChromeLinkFile.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val rawFiles: StateFlow<List<CloudFile>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            fileRepo.getFiles(user.uid)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredFiles: StateFlow<List<CloudFile>> = combine(
        rawFiles,
        _searchQuery,
        _selectedCategory
    ) { list, query, category ->
        list.filter { file ->
            val matchesQuery = query.isBlank() || file.fileName.contains(query, ignoreCase = true)
            val matchesCategory = category == FileCategory.ALL || file.category == category
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val storageUsedBytes: StateFlow<Long> = currentUser.flatMapLatest { user ->
        if (user != null) {
            fileRepo.getTotalStorageUsed(user.uid)
        } else {
            flowOf(0L)
        }
    }.combine(flowOf(0L)) { used, _ ->
        used ?: 0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        // Start embedded HTTP server for instant Chrome downloads
        LocalFileServer.start(application)
        _serverInfo.value = ServerInfo(
            isRunning = true,
            port = LocalFileServer.getPort(),
            baseUrl = LocalFileServer.getBaseUrl()
        )

        // Seed sample files if user is logged in
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    fileRepo.seedStarterFilesIfNeeded(user.uid)
                }
            }
        }
    }

    fun signIn(email: String, pass: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = authRepo.signIn(email, pass)
            result.onSuccess { user ->
                fileRepo.seedStarterFilesIfNeeded(user.uid)
                _toastEvent.emit("Welcome back, ${user.displayName}!")
            }.onFailure { e ->
                onError(e.message ?: "Sign in failed")
            }
        }
    }

    fun signUp(name: String, email: String, pass: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = authRepo.signUp(name, email, pass)
            result.onSuccess { user ->
                fileRepo.seedStarterFilesIfNeeded(user.uid)
                _toastEvent.emit("Account created! 10 GB Free Storage active.")
            }.onFailure { e ->
                onError(e.message ?: "Sign up failed")
            }
        }
    }

    fun signInAsGuest() {
        val guest = authRepo.signInAsGuest()
        viewModelScope.launch {
            fileRepo.seedStarterFilesIfNeeded(guest.uid)
            _toastEvent.emit("Signed in as Guest. 10 GB Free Tier active.")
        }
    }

    fun signOut() {
        authRepo.signOut()
        _activeFileAction.value = null
        _activeChromeLinkFile.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: FileCategory) {
        _selectedCategory.value = category
    }

    fun uploadFile(uri: Uri) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _uploadStatus.value = UploadStatus.Uploading(
                fileName = "Preparing upload...",
                progress = 0.05f,
                speedText = "Connecting to CloudFire..."
            )

            val result = fileRepo.uploadFile(uri, user.uid) { progress ->
                val simulatedSpeed = (12.4 + (progress * 8.2))
                _uploadStatus.value = UploadStatus.Uploading(
                    fileName = "Uploading file...",
                    progress = progress,
                    speedText = String.format("%.1f MB/s", simulatedSpeed)
                )
            }

            result.onSuccess { file ->
                val directLink = LocalFileServer.getDirectDownloadUrl(file.id)
                _uploadStatus.value = UploadStatus.Completed(file, directLink)
                _toastEvent.emit("File uploaded successfully! Download link ready.")
            }.onFailure { error ->
                _uploadStatus.value = UploadStatus.Error(error.message ?: "Upload failed")
            }
        }
    }

    fun dismissUpload() {
        _uploadStatus.value = UploadStatus.Idle
    }

    fun toggleFavorite(file: CloudFile) {
        viewModelScope.launch {
            fileRepo.toggleFavorite(file)
        }
    }

    fun deleteFile(file: CloudFile) {
        viewModelScope.launch {
            fileRepo.deleteFile(file)
            if (_activeFileAction.value?.id == file.id) {
                _activeFileAction.value = null
            }
            if (_activeChromeLinkFile.value?.id == file.id) {
                _activeChromeLinkFile.value = null
            }
            _toastEvent.emit("File deleted from CloudFire")
        }
    }

    fun openFileAction(file: CloudFile) {
        _activeFileAction.value = file
    }

    fun closeFileAction() {
        _activeFileAction.value = null
    }

    fun openChromeLinkDialog(file: CloudFile) {
        _activeChromeLinkFile.value = file
    }

    fun closeChromeLinkDialog() {
        _activeChromeLinkFile.value = null
    }

    fun getDirectDownloadUrl(fileId: String): String {
        return LocalFileServer.getDirectDownloadUrl(fileId)
    }

    fun getWebPageUrl(fileId: String): String {
        return LocalFileServer.getWebPageUrl(fileId)
    }

    override fun onCleared() {
        super.onCleared()
        // Note: server can stay alive during app lifecycle
    }
}
