package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CloudFile
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudFileDao {
    @Query("SELECT * FROM cloud_files WHERE userId = :userId ORDER BY uploadTimestamp DESC")
    fun getAllFiles(userId: String): Flow<List<CloudFile>>

    @Query("SELECT * FROM cloud_files WHERE userId = :userId AND fileName LIKE '%' || :query || '%' ORDER BY uploadTimestamp DESC")
    fun searchFiles(userId: String, query: String): Flow<List<CloudFile>>

    @Query("SELECT * FROM cloud_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: String): CloudFile?

    @Query("SELECT * FROM cloud_files WHERE id = :id LIMIT 1")
    fun getFileByIdSync(id: String): CloudFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: CloudFile)

    @Query("DELETE FROM cloud_files WHERE id = :id")
    suspend fun deleteFile(id: String)

    @Query("UPDATE cloud_files SET downloadCount = downloadCount + 1 WHERE id = :id")
    suspend fun incrementDownloadCount(id: String)

    @Query("UPDATE cloud_files SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("SELECT SUM(fileSize) FROM cloud_files WHERE userId = :userId")
    fun getTotalStorageUsed(userId: String): Flow<Long?>

    @Query("DELETE FROM cloud_files WHERE userId = :userId")
    suspend fun deleteAllUserFiles(userId: String)

    @Query("UPDATE cloud_files SET fileName = :newFileName, extension = :newExtension WHERE id = :id")
    suspend fun updateFileName(id: String, newFileName: String, newExtension: String)

    @Query("UPDATE cloud_files SET downloadCount = :count WHERE id = :id")
    suspend fun updateDownloadCount(id: String, count: Int)
}
