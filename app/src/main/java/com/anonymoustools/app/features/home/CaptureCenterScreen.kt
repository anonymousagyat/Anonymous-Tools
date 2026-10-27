package com.anonymoustools.app.features.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.anonymoustools.app.core.capture.CaptureManager
import com.anonymoustools.app.core.vault.PrivateVaultManager
import com.anonymoustools.app.navigation.Screen
import com.anonymoustools.app.service.capture.RecordPermissionActivity
import com.anonymoustools.app.service.gesture.GestureAccessibilityService
import com.anonymoustools.app.service.overlay.TileMenuOverlayService
import com.anonymoustools.app.ui.components.PillButton
import com.anonymoustools.app.ui.theme.*

@Composable
fun CaptureCenterScreen(
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    var hasAccessibility by remember { mutableStateOf(GestureAccessibilityService.instance != null) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var vaultFileCount by remember { mutableStateOf(0) }

    fun refreshStates() {
        hasAccessibility = GestureAccessibilityService.instance != null
        hasOverlayPermission = Settings.canDrawOverlays(context)
        vaultFileCount = PrivateVaultManager.listVaultItems(context).size
    }

    LifecycleResumeEffect(Unit) {
        refreshStates()
        onPauseOrDispose {}
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshStates()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Clean Minimal Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Text(
                    text = "Anonymous-Tools",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextBlack
                )
                Text(
                    text = "Silent capture & private storage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Section 1: Accessibility Status & Setup (Volume Shortcut & Silent Capture)
        item {
            SimpleSectionBox(
                title = "Accessibility Service",
                subtitle = if (hasAccessibility) {
                    "Active. Volume Up + Down shortcut is ready."
                } else {
                    "Required for silent snapshot & Volume Up + Down shortcut."
                },
                statusText = if (hasAccessibility) "ENABLED" else "DISABLED",
                statusColor = if (hasAccessibility) PastelMintGreen else PastelPeach
            ) {
                PillButton(
                    text = if (hasAccessibility) "Open Accessibility Settings" else "Enable in Settings",
                    icon = Icons.Default.Settings,
                    isOutline = hasAccessibility,
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }

        // Section 2: Overlay Permission (Required for Floating Menus)
        item {
            SimpleSectionBox(
                title = "Overlay Permission",
                subtitle = if (hasOverlayPermission) {
                    "Allowed. Tile menu & recording controller can appear on screen."
                } else {
                    "Required for the Quick Settings tile menu and recording bar."
                },
                statusText = if (hasOverlayPermission) "GRANTED" else "REQUIRED",
                statusColor = if (hasOverlayPermission) PastelPowderBlue else PastelPeach
            ) {
                if (!hasOverlayPermission) {
                    PillButton(
                        text = "Grant Overlay Permission",
                        icon = Icons.Default.Layers,
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            overlayLauncher.launch(intent)
                        }
                    )
                }
            }
        }

        // Section 3: Private Vault
        item {
            SimpleSectionBox(
                title = "Private Vault",
                subtitle = "$vaultFileCount items stored secretly behind .nomedia guard.",
                statusText = "$vaultFileCount FILES",
                statusColor = PastelGoldenYellow
            ) {
                PillButton(
                    text = "Open Private Vault",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = { onNavigate(Screen.VaultGallery.route) }
                )
            }
        }

        // Section 4: Quick Trigger Actions
        item {
            SimpleSectionBox(
                title = "Quick Actions",
                subtitle = "Test triggers directly from inside the app.",
                statusText = "CONTROLS",
                statusColor = PastelLavender
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        text = "Take Silent Screenshot",
                        icon = Icons.Default.Camera,
                        onClick = {
                            val acc = GestureAccessibilityService.instance
                            if (acc != null) {
                                acc.takeSilentScreenshot { _ ->
                                    refreshStates()
                                }
                            } else {
                                Toast.makeText(context, "Please enable Accessibility first", Toast.LENGTH_SHORT).show()
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PillButton(
                            text = "Open Tile Menu",
                            icon = Icons.Default.Menu,
                            isOutline = true,
                            onClick = {
                                if (Settings.canDrawOverlays(context)) {
                                    TileMenuOverlayService.toggle(context)
                                } else {
                                    Toast.makeText(context, "Allow overlay permission first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        val accService = GestureAccessibilityService.instance
                        val isRec = CaptureManager.isRecording || (accService?.isSequentialRecording == true)

                        PillButton(
                            text = if (isRec) "Stop Recording" else "Record Screen",
                            icon = if (isRec) Icons.Default.Stop else Icons.Default.Videocam,
                            isOutline = true,
                            onClick = {
                                if (accService != null && accService.isSequentialRecording) {
                                    accService.stopSequentialCapture()
                                    refreshStates()
                                } else if (CaptureManager.isRecording) {
                                    CaptureManager.stopScreenRecording()
                                    com.anonymoustools.app.service.capture.CaptureService.stopService(context)
                                    Toast.makeText(context, "Recording stopped & saved", Toast.LENGTH_SHORT).show()
                                    refreshStates()
                                } else {
                                    if (accService != null) {
                                        accService.startSequentialCapture(context)
                                        refreshStates()
                                    } else {
                                        Toast.makeText(context, "Please enable Accessibility first", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleSectionBox(
    title: String,
    subtitle: String,
    statusText: String,
    statusColor: Color,
    action: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, TextBlack, RoundedCornerShape(18.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextBlack
                )

                Surface(
                    shape = PillShape,
                    color = statusColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TextBlack)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextBlack,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            action()
        }
    }
}
