package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileCategory(val label: String) {
    ALL("All Files"),
    DOCUMENT("Documents"),
    ARCHIVE("Archives"),
    MEDIA("Media"),
    APP("APKs & Apps"),
    IMAGE("Images"),
    OTHER("Other")
}

@Entity(tableName = "cloud_files")
data class CloudFile(
    @PrimaryKey val id: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val extension: String,
    val localFilePath: String,
    val uploadTimestamp: Long = System.currentTimeMillis(),
    val downloadCount: Int = 0,
    val userId: String = "guest_user",
    val isFavorite: Boolean = false
) {
    val formattedSize: String
        get() {
            if (fileSize <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(fileSize.toDouble()) / Math.log10(1024.0)).toInt()
            val clampedGroup = digitGroups.coerceIn(0, units.size - 1)
            val value = fileSize / Math.pow(1024.0, clampedGroup.toDouble())
            return String.format(Locale.US, "%.1f %s", value, units[clampedGroup])
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
            return sdf.format(Date(uploadTimestamp))
        }

    val category: FileCategory
        get() {
            val ext = extension.lowercase()
            return when {
                ext in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso") -> FileCategory.ARCHIVE
                ext in listOf("pdf", "doc", "docx", "txt", "rtf", "xls", "xlsx", "ppt", "pptx", "csv", "json") -> FileCategory.DOCUMENT
                ext in listOf("mp4", "mkv", "avi", "mov", "webm", "mp3", "wav", "flac", "ogg", "m4a", "aac") -> FileCategory.MEDIA
                ext in listOf("apk", "xapk", "apks", "exe", "msi", "dmg") -> FileCategory.APP
                ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg") -> FileCategory.IMAGE
                else -> FileCategory.OTHER
            }
        }
}
