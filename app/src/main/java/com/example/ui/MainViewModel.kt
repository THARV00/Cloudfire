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
    val baseUrl: String = "http://localhost:8080",
    val cloudflareDomain: String = "cloudfire-rapid.trycloudflare.com",
    val isCloudflareActive: Boolean = true
)

private data class DevFileMeta(
    val name: String,
    val mime: String,
    val ext: String,
    val size: Long
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

    private val _cloudflareDomain: MutableStateFlow<String> = MutableStateFlow(LocalFileServer.getCloudflareDomain())
    val cloudflareDomain: StateFlow<String> = _cloudflareDomain.asStateFlow()

    private val _isCloudflareEnabled: MutableStateFlow<Boolean> = MutableStateFlow(LocalFileServer.isCloudflareEnabled())
    val isCloudflareEnabled: StateFlow<Boolean> = _isCloudflareEnabled.asStateFlow()

    private val _showCloudflareDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showCloudflareDialog: StateFlow<Boolean> = _showCloudflareDialog.asStateFlow()

    private val _serverInfo = MutableStateFlow(
        ServerInfo(
            isRunning = true,
            port = LocalFileServer.getPort(),
            baseUrl = LocalFileServer.getBaseUrl(),
            cloudflareDomain = LocalFileServer.getCloudflareDomain(),
            isCloudflareActive = LocalFileServer.isCloudflareEnabled()
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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val storageUsedBytes: StateFlow<Long> = currentUser.flatMapLatest { user ->
        if (user != null) {
            fileRepo.getTotalStorageUsed(user.uid)
        } else {
            flowOf(0L)
        }
    }.combine(flowOf(0L)) { used, _ ->
        used ?: 0L
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

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

    private val _showDeveloperConsole = MutableStateFlow(false)
    val showDeveloperConsole = _showDeveloperConsole.asStateFlow()

    fun openDeveloperConsole() {
        _showDeveloperConsole.value = true
    }

    fun closeDeveloperConsole() {
        _showDeveloperConsole.value = false
    }

    fun signInAsDeveloper() {
        val dev = authRepo.signInAsDeveloper()
        viewModelScope.launch {
            fileRepo.seedStarterFilesIfNeeded(dev.uid)
            _toastEvent.emit("Welcome Tharv! Developer Mode active with Full Access.")
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
        _showDeveloperConsole.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: FileCategory) {
        _selectedCategory.value = category
    }

    fun createDeveloperTestFile(type: String, customName: String? = null, customContent: String? = null) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val (name, mime, ext, size) = when (type.lowercase()) {
                "apk" -> DevFileMeta(customName ?: "CloudFire_Pro_v3.2.apk", "application/vnd.android.package-archive", "apk", 18_874_368L)
                "zip" -> DevFileMeta(customName ?: "Full_Source_Project.zip", "application/zip", "zip", 5_662_310L)
                "pdf" -> DevFileMeta(customName ?: "System_Developer_Manual.pdf", "application/pdf", "pdf", 1_450_000L)
                "video" -> DevFileMeta(customName ?: "Cinematic_4K_Demo.mp4", "video/mp4", "mp4", 32_500_000L)
                "audio" -> DevFileMeta(customName ?: "Synth_Soundtrack_Lossless.mp3", "audio/mpeg", "mp3", 4_200_000L)
                "iso" -> DevFileMeta(customName ?: "Developer_OS_Image.iso", "application/x-iso9660-image", "iso", 64_000_000L)
                else -> DevFileMeta(customName ?: "Custom_Dev_File.txt", "text/plain", "txt", 1024L)
            }

            val result = fileRepo.createDeveloperFile(
                userId = user.uid,
                fileName = name,
                mimeType = mime,
                extension = ext,
                simulatedSize = size,
                textContent = customContent
            )

            result.onSuccess { file ->
                _toastEvent.emit("Generated ${file.fileName} (${file.formattedSize})!")
            }.onFailure { error ->
                _toastEvent.emit("Failed to create file: ${error.message}")
            }
        }
    }

    fun deleteAllFiles() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            fileRepo.deleteAllUserFiles(user.uid)
            _activeFileAction.value = null
            _activeChromeLinkFile.value = null
            _toastEvent.emit("All files wiped clean (Storage reset to 0 MB).")
        }
    }

    fun seedStarterFiles() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            fileRepo.seedStarterFilesIfNeeded(user.uid)
            _toastEvent.emit("Starter bundle seeded successfully.")
        }
    }

    fun renameFile(file: CloudFile, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            fileRepo.renameFile(file.id, newName.trim())
            _activeFileAction.value = null
            _toastEvent.emit("Renamed to $newName")
        }
    }

    fun setDownloadCount(file: CloudFile, count: Int) {
        viewModelScope.launch {
            fileRepo.setDownloadCount(file.id, count.coerceAtLeast(0))
            _toastEvent.emit("Download count updated to $count")
        }
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
                val mediaFireLink = LocalFileServer.getNetworkWebPageUrl(file.id)
                _uploadStatus.value = UploadStatus.Completed(file, mediaFireLink)
                _toastEvent.emit("File uploaded! MediaFire download link ready.")
            }.onFailure { error ->
                _uploadStatus.value = UploadStatus.Error(error.message ?: "Upload failed")
            }
        }
    }

    fun openCloudflareDialog() {
        _showCloudflareDialog.value = true
    }

    fun closeCloudflareDialog() {
        _showCloudflareDialog.value = false
    }

    fun setCloudflareDomain(domain: String) {
        val app = getApplication<Application>()
        LocalFileServer.setCloudflareDomain(app, domain)
        val updated = LocalFileServer.getCloudflareDomain()
        _cloudflareDomain.value = updated
        _serverInfo.value = _serverInfo.value.copy(cloudflareDomain = updated)
        viewModelScope.launch {
            _toastEvent.emit("Server domain updated: $updated")
        }
    }

    fun generateNewQuickTunnel() {
        val app = getApplication<Application>()
        val newDomain = LocalFileServer.generateNewQuickTunnel(app)
        _cloudflareDomain.value = newDomain
        _serverInfo.value = _serverInfo.value.copy(cloudflareDomain = newDomain)
        viewModelScope.launch {
            _toastEvent.emit("Generated new tunnel: $newDomain")
        }
    }

    fun toggleCloudflareTunnel(enabled: Boolean) {
        val app = getApplication<Application>()
        LocalFileServer.setCloudflareEnabled(app, enabled)
        _isCloudflareEnabled.value = enabled
        _serverInfo.value = _serverInfo.value.copy(isCloudflareActive = enabled)
        viewModelScope.launch {
            val msg = if (enabled) "External network active" else "Local Wi-Fi mode only"
            _toastEvent.emit(msg)
        }
    }

    fun getMediaFireDownloadUrl(fileId: String): String {
        return LocalFileServer.getMediaFireDownloadUrl(fileId)
    }

    fun getMediaFireWebPageUrl(fileId: String): String {
        return LocalFileServer.getMediaFireWebPageUrl(fileId)
    }

    fun getCloudflareDownloadUrl(fileId: String): String {
        return LocalFileServer.getMediaFireDownloadUrl(fileId)
    }

    fun getCloudflareWebPageUrl(fileId: String): String {
        return LocalFileServer.getMediaFireWebPageUrl(fileId)
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

    fun getNetworkDownloadUrl(fileId: String): String {
        return LocalFileServer.getNetworkDownloadUrl(fileId)
    }

    fun getNetworkWebPageUrl(fileId: String): String {
        return LocalFileServer.getNetworkWebPageUrl(fileId)
    }

    override fun onCleared() {
        super.onCleared()
        // Note: server can stay alive during app lifecycle
    }
}
