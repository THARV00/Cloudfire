package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CloudFile
import com.example.data.model.FileCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CloudFire", appName)
  }

  @Test
  fun `test cloud file formatting and category`() {
    val archiveFile = CloudFile(
      id = "test_1",
      fileName = "archive.zip",
      fileSize = 1048576L * 5, // 5 MB
      mimeType = "application/zip",
      extension = "zip",
      localFilePath = "/dummy/path"
    )
    assertEquals("5.0 MB", archiveFile.formattedSize)
    assertEquals(FileCategory.ARCHIVE, archiveFile.category)
  }
}
