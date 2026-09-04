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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.ui.theme.CloudflareNavy
import com.example.ui.theme.CloudflareOrange
import com.example.ui.theme.CloudflareOrangeDark
import com.example.ui.theme.CloudflareOrangeLight
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
    isCloudflareEnabled: Boolean = true,
    cloudflareDomain: String = "",
    onSearchChange: (String) -> Unit,
    onCategorySelect: (FileCategory) -> Unit,
    onUploadClick: (Uri) -> Unit,
    onFileClick: (CloudFile) -> Unit,
    onQuickChromeLink: (CloudFile) -> Unit,
    onToggleFavorite: (CloudFile) -> Unit,
    onDeleteFile: (CloudFile) -> Unit,
    onSignOut: () -> Unit,
    onOpenDeveloperConsole: () -> Unit = {},
    onOpenCloudflareTunnelDialog: () -> Unit = {}
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
                    // Developer Mode Superuser pill
                    if (user.isDeveloper) {
                        Surface(
                            color = Color(0xFF1E1B4B),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenDeveloperConsole() }
                                .testTag("btn_top_dev_console")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👑", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "THARV DEV",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD700)
                                )
                            }
                        }
                    }

                    // Cloudflare Tunnel status pill
                    Surface(
                        color = if (isCloudflareEnabled) CloudflareOrangeLight else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenCloudflareTunnelDialog() }
                            .testTag("btn_top_cloudflare_tunnel")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌩️", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCloudflareEnabled) "CF Active" else "CF Off",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCloudflareEnabled) CloudflareNavy else Color.Gray
                            )
                        }
                    }

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
                                text = ":${serverInfo.port}",
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
                                    .background(if (user.isDeveloper) Color(0xFF1E1B4B) else CloudFireBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (user.isDeveloper) "👑" else user.displayName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (user.isDeveloper) 16.sp else 15.sp
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
                                if (user.isDeveloper) {
                                    Text(
                                        text = "Role: Superuser / Developer",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4F46E5),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tier: Unlimited Storage",
                                        fontSize = 11.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "Tier: MediaFire 10 GB Free",
                                        fontSize = 11.sp,
                                        color = CloudFireBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            DropdownMenuItem(
                                text = { Text("Cloudflare Tunnel", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Text("🌩️", fontSize = 16.sp)
                                },
                                onClick = {
                                    showUserMenu = false
                                    onOpenCloudflareTunnelDialog()
                                },
                                modifier = Modifier.testTag("menu_cloudflare_tunnel")
                            )

                            if (user.isDeveloper) {
                                DropdownMenuItem(
                                    text = { Text("Developer Console", fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF4F46E5))
                                    },
                                    onClick = {
                                        showUserMenu = false
                                        onOpenDeveloperConsole()
                                    },
                                    modifier = Modifier.testTag("menu_dev_console")
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
                        limitBytes = user.storageLimitBytes,
                        isDeveloper = user.isDeveloper,
                        onOpenDeveloperConsole = onOpenDeveloperConsole
                    )
                }

                // Cloudflare Tunnel Edge Ingress Banner
                item {
                    CloudflareTunnelBanner(
                        domain = cloudflareDomain,
                        isEnabled = isCloudflareEnabled,
                        onOpenSettings = onOpenCloudflareTunnelDialog
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
fun StorageQuotaCard(
    usedBytes: Long,
    limitBytes: Long,
    isDeveloper: Boolean = false,
    onOpenDeveloperConsole: () -> Unit = {}
) {
    val usedMB = usedBytes.toDouble() / (1024.0 * 1024.0)

    if (isDeveloper) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("card_developer_storage"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD700)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👑", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Tharv Developer Quota",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF4F46E5),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "SUPERUSER",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Unlimited Storage • Zero Restrictions Active",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Button(
                        onClick = onOpenDeveloperConsole,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_dev_tools_quota_card")
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dev Tools", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = { 0.01f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFFFD700),
                    trackColor = Color.White.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f MB active storage used", usedMB),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "∞ Unlimited Storage",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
        return
    }

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

            // Quick Chrome / Cloudflare Link Button
            Surface(
                color = CloudflareOrangeLight,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CloudflareOrange.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onQuickChromeLink() }
                    .testTag("btn_quick_chrome_link_${file.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌩️", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "CF Link",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = CloudflareOrangeDark
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

@Composable
fun CloudflareTunnelBanner(
    domain: String,
    isEnabled: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onOpenSettings() }
            .testTag("card_cloudflare_tunnel_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CloudflareOrangeLight),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, CloudflareOrange.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CloudflareOrange),
                contentAlignment = Alignment.Center
            ) {
                Text("🌩️", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cloudflare Tunnel Ingress",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CloudflareNavy
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (isEnabled) Color(0xFF2E7D32) else Color.Gray,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isEnabled) "ACTIVE" else "OFF",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (domain.isNotEmpty()) "https://$domain" else "Universal HTTPS Tunnel",
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = CloudflareOrangeDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Files shareable worldwide via Chrome auto-download",
                    fontSize = 11.sp,
                    color = Color(0xFF4A3E56)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("btn_banner_cf_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Cloudflare Settings",
                    tint = CloudflareNavy
                )
            }
        }
    }
}

