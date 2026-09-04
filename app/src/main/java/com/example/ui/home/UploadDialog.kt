package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.CloudflareNavy
import com.example.ui.theme.CloudflareOrange
import com.example.ui.theme.CloudflareOrangeDark
import com.example.ui.theme.CloudflareOrangeLight

@Composable
fun UploadDialog(
    status: UploadStatus,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

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
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = status.file.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Size: ${status.file.formattedSize} • Cloudflare Tunnel Ready",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = CloudflareOrangeLight),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CloudflareOrange.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🌩️ Cloudflare Tunnel Download Link:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CloudflareNavy
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = status.directLink,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CloudflareOrangeDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "💡 Paste this Cloudflare link in Chrome on any phone or PC worldwide!",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                copyToClipboard(context, status.directLink, "Cloudflare Tunnel download link copied!")
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy CF Link")
                        }

                        Button(
                            onClick = {
                                openInChrome(context, status.directLink)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CloudflareOrange)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open in Chrome")
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
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    )
}
