package com.example.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CloudFile
import com.example.data.model.FileCategory
import com.example.data.repository.UserProfile
import com.example.ui.ServerInfo
import com.example.ui.theme.CloudFireBlue
import com.example.ui.theme.CloudFireBlueDark
import com.example.ui.theme.CloudFireCyan
import com.example.ui.theme.FileCategoryApp
import com.example.ui.theme.FileCategoryArchive
import com.example.ui.theme.FileCategoryDocument
import com.example.ui.theme.FileCategoryImage
import com.example.ui.theme.FileCategoryMedia
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: UserProfile,
    files: List<CloudFile>,
    storageUsedBytes: Long,
    searchQuery: String,
    selectedCategory: FileCategory,
    serverInfo: ServerInfo,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (FileCategory) -> Unit,
    onUploadClick: (Uri) -> Unit,
    onFileClick: (CloudFile) -> Unit,
    onQuickChromeLink: (CloudFile) -> Unit,
    onToggleFavorite: (CloudFile) -> Unit,
    onDeleteFile: (CloudFile) -> Unit,
    onSignOut: () -> Unit
) {
    var showUserMenu by remember { mutableStateOf(false) }

    // File picker launcher supporting ANY file format (*/*)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onUploadClick(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "CloudFire",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Row {
                            Text(
                                "Cloud",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Fire",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = CloudFireBlue
                            )
                        }
                    }
                },
                actions = {
                    // Server live pulse badge
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Server :${serverInfo.port}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    // User Profile Chip / Menu
                    Box {
                        IconButton(
                            onClick = { showUserMenu = true },
                            modifier = Modifier.testTag("btn_user_menu")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CloudFireBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.displayName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(
                                    text = user.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = user.email,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tier: MediaFire 10 GB Free",
                                    fontSize = 11.sp,
                                    color = CloudFireBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Sign Out") },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red)
                                },
                                onClick = {
                                    showUserMenu = false
                                    onSignOut()
                                },
                                modifier = Modifier.testTag("menu_signout")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    filePickerLauncher.launch(arrayOf("*/*"))
                },
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                text = { Text("Upload File", fontWeight = FontWeight.Bold) },
                containerColor = CloudFireBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_upload_file")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_file_list"),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Storage Quota Dashboard Card (MediaFire style)
                item {
                    StorageQuotaCard(
                        usedBytes = storageUsedBytes,
                        limitBytes = user.storageLimitBytes
                    )
                }

                // Quick Upload Drop Area
                item {
                    QuickUploadCard(
                        onBrowse = { filePickerLauncher.launch(arrayOf("*/*")) }
                    )
                }

                // Search Bar
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text("Search your files...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = CloudFireBlue)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CloudFireBlue,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_search_files")
                        )
                    }
                }

                // Category Filter Chips
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(FileCategory.values()) { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCategorySelect(category) },
                                label = { Text(category.label, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CloudFireBlue,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Search Results (${files.size})" else "My Files (${files.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auto-Download in Chrome",
                            fontSize = 11.sp,
                            color = CloudFireBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // File items
                if (files.isEmpty()) {
                    item {
                        EmptyFilesView(onUploadClick = { filePickerLauncher.launch(arrayOf("*/*")) })
                    }
                } else {
                    items(files, key = { it.id }) { file ->
                        FileItemRow(
                            file = file,
                            onFileClick = { onFileClick(file) },
                            onQuickChromeLink = { onQuickChromeLink(file) },
                            onToggleFavorite = { onToggleFavorite(file) }
                        )
                    }
                }
            }

            // Developer attribution in corner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                shape = RoundedCornerShape(topStart = 8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .testTag("tag_home_developer_credit")
            ) {
                Text(
                    text = "devloper :- Tharv",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun StorageQuotaCard(usedBytes: Long, limitBytes: Long) {
    val usedMB = usedBytes.toDouble() / (1024.0 * 1024.0)
    val limitGB = limitBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val progress = (usedBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CloudFireBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            tint = CloudFireBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CloudFire Free Tier",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "10 GB Cloud Storage",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    color = CloudFireBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "FREE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CloudFireBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CloudFireBlue,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f MB used", usedMB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                Text(
                    text = String.format(Locale.US, "%.1f GB total", limitGB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun QuickUploadCard(onBrowse: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onBrowse() }
            .testTag("card_quick_upload"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F6FF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCCE0FF))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(CloudFireBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upload Any File Format",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F2B48)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "APK, ZIP, PDF, Video, MP3, ISO • Instant Chrome link",
                    fontSize = 12.sp,
                    color = Color(0xFF4A6882)
                )
            }

            Surface(
                color = CloudFireBlue,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "+ Select",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun FileItemRow(
    file: CloudFile,
    onFileClick: () -> Unit,
    onQuickChromeLink: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val categoryColor = when (file.category) {
        FileCategory.DOCUMENT -> FileCategoryDocument
        FileCategory.ARCHIVE -> FileCategoryArchive
        FileCategory.MEDIA -> FileCategoryMedia
        FileCategory.APP -> FileCategoryApp
        FileCategory.IMAGE -> FileCategoryImage
        else -> CloudFireBlue
    }

    val categoryIcon = when (file.category) {
        FileCategory.DOCUMENT -> Icons.Default.Description
        FileCategory.ARCHIVE -> Icons.Default.Archive
        FileCategory.MEDIA -> Icons.Default.Movie
        FileCategory.APP -> Icons.Default.Archive
        FileCategory.IMAGE -> Icons.Default.Image
        else -> Icons.Default.InsertDriveFile
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onFileClick() }
            .testTag("file_item_${file.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored Category Icon Box
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // File Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.formattedSize,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = " • ",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = file.extension.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                    Text(
                        text = " • ${file.downloadCount} dl",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // Quick Chrome Link Button
            Surface(
                color = Color(0xFFE8F2FF),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onQuickChromeLink() }
                    .testTag("btn_quick_chrome_link_${file.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Chrome Download Link",
                        tint = CloudFireBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Link",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = CloudFireBlue
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Favorite button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (file.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (file.isFavorite) Color.Red else Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyFilesView(onUploadClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F2FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = CloudFireBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Files Yet",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Upload any format (APK, ZIP, PDF, Video, MP3) and generate instant Chrome auto-download links.",
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            color = CloudFireBlue,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onUploadClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upload First File",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}
