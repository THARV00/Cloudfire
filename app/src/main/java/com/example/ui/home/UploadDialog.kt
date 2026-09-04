package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.UploadStatus
import com.example.ui.theme.CloudFireBlue
import com.example.ui.theme.CloudFireCyan

@Composable
fun UploadDialog(
    status: UploadStatus,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPublicLink by remember { mutableStateOf(false) }

    if (status is UploadStatus.Idle) return

    AlertDialog(
        onDismissRequest = {
            if (status !is UploadStatus.Uploading) {
                onDismiss()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("upload_status_dialog"),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, tint, title) = when (status) {
                    is UploadStatus.Uploading -> Triple(Icons.Default.CloudUpload, CloudFireBlue, "Uploading to CloudFire...")
                    is UploadStatus.Completed -> Triple(Icons.Default.CheckCircle, Color(0xFF00C853), "Upload Complete!")
                    is UploadStatus.Error -> Triple(Icons.Default.Error, Color.Red, "Upload Failed")
                    UploadStatus.Idle -> Triple(Icons.Default.CloudUpload, CloudFireBlue, "")
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            when (status) {
                is UploadStatus.Uploading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = status.fileName,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator(
                            progress = { status.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CloudFireBlue,
                            trackColor = Color.LightGray.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(status.progress * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                color = CloudFireBlue,
                                fontSize = 14.sp
                            )
                            Text(
                                text = status.speedText,
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                is UploadStatus.Completed -> {
                    val publicLink = remember(status.file.id, status.file.fileName) {
                        val cleanName = try {
                            java.net.URLEncoder.encode(status.file.fileName, "UTF-8").replace("+", "%20")
                        } catch (e: Exception) {
                            "file"
                        }
                        "https://www.mediafire.com/file/${status.file.id}/$cleanName"
                    }
                    val activeLink = if (isPublicLink) publicLink else status.directLink

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = status.file.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Size: ${status.file.formattedSize} • ${if (isPublicLink) "Public Worldwide Link" else "MediaFire Link Ready"}",
                            fontSize = 13.sp,
                            color = if (isPublicLink) Color(0xFF2E7D32) else Color.Gray
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPublicLink) Color(0xFFE8F5E9) else Color(0xFFEBF3FF)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.2.dp,
                                if (isPublicLink) Color(0xFF2E7D32) else CloudFireBlue.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isPublicLink) "🌐" else "🔥", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isPublicLink) "Public MediaFire Link:" else "MediaFire Link:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPublicLink) Color(0xFF1B5E20) else CloudFireBlue
                                        )
                                    }
                                    Surface(
                                        color = if (isPublicLink) Color(0xFF2E7D32) else CloudFireBlue,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isPublicLink) "PUBLIC" else "LOCAL",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = activeLink,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isPublicLink) Color(0xFF1B5E20) else CloudFireBlue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Make Public Link Action / Status Toggle
                        if (!isPublicLink) {
                            OutlinedButton(
                                onClick = {
                                    isPublicLink = true
                                    copyToClipboard(context, publicLink, "Public link created & copied to clipboard!")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_make_public_link"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.2.dp, Color(0xFF2E7D32)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🌐 Make Public Link", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "✓ Shareable worldwide over internet",
                                        fontSize = 11.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Medium
                                    )
                                    TextButton(
                                        onClick = { isPublicLink = false },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text("Local Link", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isPublicLink) {
                                "💡 Anyone on the internet can open this link to download your file on any phone or computer!"
                            } else {
                                "💡 Tap 'Make Public Link' to create a universal worldwide shareable link!"
                            },
                            fontSize = 12.sp,
                            color = if (isPublicLink) Color(0xFF1B5E20) else Color(0xFF1E3A8A)
                        )
                    }
                }

                is UploadStatus.Error -> {
                    Text(
                        text = status.message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                UploadStatus.Idle -> {}
            }
        },
        confirmButton = {
            when (status) {
                is UploadStatus.Completed -> {
                    val publicLink = remember(status.file.id, status.file.fileName) {
                        val cleanName = try {
                            java.net.URLEncoder.encode(status.file.fileName, "UTF-8").replace("+", "%20")
                        } catch (e: Exception) {
                            "file"
                        }
                        "https://www.mediafire.com/file/${status.file.id}/$cleanName"
                    }
                    val activeLink = if (isPublicLink) publicLink else status.directLink

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                val label = if (isPublicLink) "Public MediaFire link copied!" else "MediaFire download link copied!"
                                copyToClipboard(context, activeLink, label)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPublicLink) "Copy Public" else "Copy Link")
                        }

                        Button(
                            onClick = {
                                openInBrowser(context, activeLink)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPublicLink) Color(0xFF2E7D32) else CloudFireBlue
                            )
                        ) {
                            Icon(if (isPublicLink) Icons.Default.Public else Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPublicLink) "Open Public" else "Open in MediaFire")
                        }
                    }
                }
                is UploadStatus.Error -> {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
                is UploadStatus.Uploading -> {
                    // Uploading is active
                }
                UploadStatus.Idle -> {}
            }
        },
        dismissButton = {
            if (status is UploadStatus.Completed) {
                val publicLink = remember(status.file.id, status.file.fileName) {
                    val cleanName = try {
                        java.net.URLEncoder.encode(status.file.fileName, "UTF-8").replace("+", "%20")
                    } catch (e: Exception) {
                        "file"
                    }
                    "https://www.mediafire.com/file/${status.file.id}/$cleanName"
                }
                val activeLink = if (isPublicLink) publicLink else status.directLink

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, status.file.fileName)
                                putExtra(android.content.Intent.EXTRA_TEXT, "Download ${status.file.fileName} via MediaFire: $activeLink")
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Download Link"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Share", fontSize = 12.sp)
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Done", fontSize = 12.sp)
                    }
                }
            }
        }
    )
}
