package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.data.local.CloudFileDao
import com.example.data.model.CloudFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class FileRepository(
    private val context: Context,
    private val fileDao: CloudFileDao
) {
    fun getFiles(userId: String): Flow<List<CloudFile>> = fileDao.getAllFiles(userId)

    fun searchFiles(userId: String, query: String): Flow<List<CloudFile>> =
        fileDao.searchFiles(userId, query)

    fun getTotalStorageUsed(userId: String): Flow<Long?> = fileDao.getTotalStorageUsed(userId)

    suspend fun getFileById(fileId: String): CloudFile? = fileDao.getFileById(fileId)

    suspend fun toggleFavorite(file: CloudFile) = withContext(Dispatchers.IO) {
        fileDao.updateFavorite(file.id, !file.isFavorite)
    }

    suspend fun recordDownload(fileId: String) = withContext(Dispatchers.IO) {
        fileDao.incrementDownloadCount(fileId)
    }

    suspend fun deleteFile(file: CloudFile) = withContext(Dispatchers.IO) {
        try {
            val localFile = File(file.localFilePath)
            if (localFile.exists()) {
                localFile.delete()
            }
            localFile.parentFile?.delete()
            fileDao.deleteFile(file.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FileRepository", "Error deleting file", e)
            Result.failure(e)
        }
    }

    suspend fun uploadFile(
        uri: Uri,
        userId: String,
        onProgress: (Float) -> Unit
    ): Result<CloudFile> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. Resolve file name & size from Uri
            var fileName = "file_${System.currentTimeMillis()}"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            // 2. Resolve mime type and extension
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val extension = if (fileName.contains(".")) {
                fileName.substringAfterLast(".", "")
            } else {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
            }

            // 3. Create persistent storage directory
            val fileId = UUID.randomUUID().toString()
            val uploadsDir = File(context.filesDir, "uploads/$fileId")
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs()
            }
            val destinationFile = File(uploadsDir, fileName)

            // 4. Stream and copy file with simulated/actual progress feedback
            onProgress(0.15f)
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open input stream for Uri: $uri"))

            inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(65536) // 64 KB
                    var totalRead = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalRead += read
                        if (fileSize > 0) {
                            val progress = (0.2f + (totalRead.toFloat() / fileSize.toFloat()) * 0.7f).coerceIn(0.2f, 0.95f)
                            onProgress(progress)
                        }
                    }
                    if (fileSize <= 0) {
                        fileSize = totalRead
                    }
                }
            }

            onProgress(0.98f)
            delay(150) // Small visual completion settle
            onProgress(1.0f)

            // 5. Store in Room database
            val cloudFile = CloudFile(
                id = fileId,
                fileName = fileName,
                fileSize = destinationFile.length().coerceAtLeast(fileSize),
                mimeType = mimeType,
                extension = extension,
                localFilePath = destinationFile.absolutePath,
                uploadTimestamp = System.currentTimeMillis(),
                downloadCount = 0,
                userId = userId,
                isFavorite = false
            )

            fileDao.insertFile(cloudFile)
            Result.success(cloudFile)
        } catch (e: Exception) {
            Log.e("FileRepository", "Error uploading file", e)
            Result.failure(e)
        }
    }

    /**
     * Seeds initial helpful starter files so the user immediately has files to test Chrome download
     */
    suspend fun seedStarterFilesIfNeeded(userId: String) = withContext(Dispatchers.IO) {
        try {
            val starterDir = File(context.filesDir, "uploads/starter_doc")
            if (!starterDir.exists()) starterDir.mkdirs()

            val starterFile = File(starterDir, "Welcome_to_CloudFire_Guide.txt")
            if (!starterFile.exists()) {
                starterFile.writeText(
                    """
                    ==============================================
                    🔥 Welcome to CloudFire - Fast Cloud Sharing! 🔥
                    ==============================================
                    
                    Inspired by MediaFire, CloudFire allows you to:
                    1. Upload ANY file format: APK, ZIP, PDF, Video, MP3, ISO, and more.
                    2. Instantly generate direct download links.
                    3. Paste the link directly in Google Chrome browser.
                    4. Chrome will AUTOMATICALLY trigger the file download!
                    
                    Free Storage Limit: 10 GB
                    Download Speed: Unlimited
                    Server: Local CloudFire Engine
                    
                    Thank you for using CloudFire!
                    """.trimIndent()
                )

                val fileEntity = CloudFile(
                    id = "starter_doc",
                    fileName = "Welcome_to_CloudFire_Guide.txt",
                    fileSize = starterFile.length(),
                    mimeType = "text/plain",
                    extension = "txt",
                    localFilePath = starterFile.absolutePath,
                    uploadTimestamp = System.currentTimeMillis() - 3600000,
                    downloadCount = 1,
                    userId = userId,
                    isFavorite = true
                )
                fileDao.insertFile(fileEntity)
            }

            // Also seed a sample archive file
            val zipDir = File(context.filesDir, "uploads/starter_pack")
            if (!zipDir.exists()) zipDir.mkdirs()

            val zipFile = File(zipDir, "CloudFire_Demo_Pack.zip")
            if (!zipFile.exists()) {
                zipFile.writeBytes(
                    // Simple sample zip header payload
                    byteArrayOf(0x50, 0x4B, 0x05, 0x06, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                )

                val zipEntity = CloudFile(
                    id = "starter_pack",
                    fileName = "CloudFire_Demo_Pack.zip",
                    fileSize = 1048576L * 4 + 250000L, // 4.2 MB simulated zip
                    mimeType = "application/zip",
                    extension = "zip",
                    localFilePath = zipFile.absolutePath,
                    uploadTimestamp = System.currentTimeMillis() - 86400000,
                    downloadCount = 3,
                    userId = userId,
                    isFavorite = false
                )
                fileDao.insertFile(zipEntity)
            }
        } catch (e: Exception) {
            Log.e("FileRepository", "Error seeding files", e)
        }
    }

    suspend fun deleteAllUserFiles(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            fileDao.deleteAllUserFiles(userId)
            val uploadsDir = File(context.filesDir, "uploads")
            if (uploadsDir.exists()) {
                uploadsDir.listFiles()?.forEach { dir ->
                    if (dir.isDirectory) {
                        dir.deleteRecursively()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FileRepository", "Error clearing files", e)
            Result.failure(e)
        }
    }

    suspend fun renameFile(fileId: String, newName: String) = withContext(Dispatchers.IO) {
        val extension = if (newName.contains(".")) newName.substringAfterLast(".", "") else ""
        fileDao.updateFileName(fileId, newName, extension)
    }

    suspend fun setDownloadCount(fileId: String, count: Int) = withContext(Dispatchers.IO) {
        fileDao.updateDownloadCount(fileId, count)
    }

    suspend fun createDeveloperFile(
        userId: String,
        fileName: String,
        mimeType: String,
        extension: String,
        simulatedSize: Long,
        textContent: String? = null
    ): Result<CloudFile> = withContext(Dispatchers.IO) {
        try {
            val fileId = UUID.randomUUID().toString()
            val dir = File(context.filesDir, "uploads/$fileId")
            if (!dir.exists()) dir.mkdirs()

            val targetFile = File(dir, fileName)
            if (textContent != null) {
                targetFile.writeText(textContent)
            } else {
                when (extension.lowercase()) {
                    "apk" -> targetFile.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x08, 0x00))
                    "zip" -> targetFile.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
                    "pdf" -> targetFile.writeBytes("%PDF-1.4\n%Developer Tharv CloudFire Admin Document\n%%EOF".toByteArray())
                    "mp4" -> targetFile.writeBytes(byteArrayOf(0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6F, 0x6D))
                    "mp3" -> targetFile.writeBytes(byteArrayOf(0x49, 0x44, 0x33, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
                    "iso" -> targetFile.writeBytes(byteArrayOf(0x43, 0x44, 0x30, 0x30, 0x31))
                    else -> targetFile.writeText("CloudFire Developer Generated File: $fileName\nCreated by Tharv (Developer/Admin)")
                }
            }

            val actualSize = targetFile.length().coerceAtLeast(simulatedSize)
            val cloudFile = CloudFile(
                id = fileId,
                fileName = fileName,
                fileSize = actualSize,
                mimeType = mimeType,
                extension = extension,
                localFilePath = targetFile.absolutePath,
                uploadTimestamp = System.currentTimeMillis(),
                downloadCount = 0,
                userId = userId,
                isFavorite = false
            )
            fileDao.insertFile(cloudFile)
            Result.success(cloudFile)
        } catch (e: Exception) {
            Log.e("FileRepository", "Error creating developer file", e)
            Result.failure(e)
        }
    }
}
