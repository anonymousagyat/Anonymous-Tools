package com.anonymoustools.app.features.vault

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anonymoustools.app.core.model.CaptureItem
import com.anonymoustools.app.core.model.CaptureType
import com.anonymoustools.app.ui.components.PastelCard
import com.anonymoustools.app.ui.components.PillButton
import com.anonymoustools.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VaultGalleryScreen(
    onNavigateBack: () -> Unit,
    viewModel: VaultViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var viewingItem by remember { mutableStateOf<CaptureItem?>(null) }

    // Auto-refresh vault immediately when user returns to the app / screen!
    LifecycleResumeEffect(Unit) {
        viewModel.loadItems()
        onPauseOrDispose {}
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val filteredItems = remember(uiState.items, uiState.selectedFilter) {
        when (uiState.selectedFilter) {
            1 -> uiState.items.filter { it.type == CaptureType.SNAPSHOT }
            2 -> uiState.items.filter { it.type == CaptureType.VIDEO }
            else -> uiState.items
        }
    }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    // Fullscreen Image Viewer Modal
    val currentViewing = viewingItem
    if (currentViewing != null && currentViewing.type == CaptureType.SNAPSHOT) {
        FullscreenImageViewer(
            item = currentViewing,
            onDismiss = { viewingItem = null },
            onExport = {
                viewModel.exportItem(currentViewing)
            },
            onDelete = {
                viewModel.deleteItem(currentViewing)
                viewingItem = null
            },
            onShare = {
                shareFile(context, currentViewing)
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextBlack)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(text = "Vault", style = MaterialTheme.typography.headlineLarge, color = TextBlack)
                    Text(text = "Protected media storage", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        // Vault Status Banner
        item {
            PastelCard(
                title = "Private Media Vault",
                description = "Media is stored in app-private storage and hidden from public Android galleries with a .nomedia guard.",
                backgroundColor = PastelMintGreen,
                badgeText = "PROTECTED",
                icon = Icons.Default.Shield,
                actionContent = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.items.size} Stored Files",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextBlack
                        )

                        if (uiState.items.isNotEmpty()) {
                            PillButton(
                                text = "Shred Vault",
                                icon = Icons.Default.DeleteSweep,
                                isOutline = true,
                                onClick = { viewModel.shredVault() }
                            )
                        }
                    }
                }
            )
        }

        // Filter Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(text = "All (${uiState.items.size})", selected = uiState.selectedFilter == 0) {
                    viewModel.setFilter(0)
                }
                FilterPill(
                    text = "Screenshots (${uiState.items.count { it.type == CaptureType.SNAPSHOT }})",
                    selected = uiState.selectedFilter == 1
                ) { viewModel.setFilter(1) }
                FilterPill(
                    text = "Videos (${uiState.items.count { it.type == CaptureType.VIDEO }})",
                    selected = uiState.selectedFilter == 2
                ) { viewModel.setFilter(2) }
            }
        }

        if (uiState.isLoading && uiState.items.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TextBlack)
                }
            }
        } else if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No captures found in vault",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredItems, key = { it.file.absolutePath }) { item ->
                VaultMediaCard(
                    item = item,
                    dateText = dateFormatter.format(Date(item.timestamp)),
                    onClick = {
                        if (item.type == CaptureType.SNAPSHOT) {
                            viewingItem = item
                        } else {
                            playVideo(context, item)
                        }
                    },
                    onExport = { viewModel.exportItem(item) },
                    onDelete = { viewModel.deleteItem(item) }
                )
            }
        }
    }
}

@Composable
fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = PillShape,
        color = if (selected) TextBlack else CardWhite,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) CardWhite else TextBlack,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun VaultMediaCard(
    item: CaptureItem,
    dateText: String,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = when (item.type) {
        CaptureType.SNAPSHOT -> PastelLightPink
        CaptureType.VIDEO -> PastelPeach
    }

    // Load actual mini thumbnail asynchronously
    val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, key1 = item.file.absolutePath) {
        value = ThumbnailLoader.loadThumbnail(item)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShapeLg)
            .clickable(onClick = onClick),
        shape = CardShapeLg,
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Preview Box
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CardShapeMd)
                    .background(TextBlack.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (item.type == CaptureType.SNAPSHOT) Icons.Default.Image else Icons.Default.Videocam,
                        contentDescription = null,
                        tint = TextBlack,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (item.type == CaptureType.VIDEO) {
                    Surface(
                        shape = PillShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = PillShape,
                        color = TextBlack
                    ) {
                        Text(
                            text = if (item.type == CaptureType.SNAPSHOT) "IMAGE" else "VIDEO",
                            style = MaterialTheme.typography.labelSmall,
                            color = CardWhite,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    val sizeMb = String.format(Locale.US, "%.1f MB", item.sizeBytes / (1024f * 1024f))
                    Text(
                        text = sizeMb,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PillButton(
                        text = "Export",
                        icon = Icons.Default.FileDownload,
                        isOutline = true,
                        onClick = onExport
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(PillShape)
                            .background(TextBlack.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = TextBlack,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenImageViewer(
    item: CaptureItem,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val fullBitmap = remember(item.file.absolutePath) {
        try {
            BitmapFactory.decodeFile(item.file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (fullBitmap != null) {
                Image(
                    bitmap = fullBitmap.asImageBitmap(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Top Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(PillShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(PillShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }

                    IconButton(
                        onClick = onExport,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(PillShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = Color.White)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(PillShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
        }
    }
}

fun playVideo(context: Context, item: CaptureItem) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            item.file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No video player available", Toast.LENGTH_SHORT).show()
    }
}

fun shareFile(context: Context, item: CaptureItem) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            item.file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (item.type == CaptureType.SNAPSHOT) "image/png" else "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Capture"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share", Toast.LENGTH_SHORT).show()
    }
}
