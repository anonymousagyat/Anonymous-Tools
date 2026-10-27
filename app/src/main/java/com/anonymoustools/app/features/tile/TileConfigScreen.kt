package com.anonymoustools.app.features.tile

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anonymoustools.app.service.overlay.FloatingCircleService
import com.anonymoustools.app.ui.components.PastelCard
import com.anonymoustools.app.ui.components.PillButton
import com.anonymoustools.app.ui.theme.*

@Composable
fun TileConfigScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 110.dp),
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
                    Text(text = "Control Tile.", style = MaterialTheme.typography.headlineLarge, color = TextBlack)
                    Text(text = "Quick settings & floating circle", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        // Hero Card
        item {
            PastelCard(
                title = "Status Bar Tile & Floating Circle",
                description = "Tapping the Quick Settings tile in your notification shade opens a small circular floating window with instant controls for Snapshot, Recording, and Vault.",
                backgroundColor = PastelLavender,
                badgeText = "QUICK SETTINGS",
                icon = Icons.Default.Tune,
                actionContent = {
                    PillButton(
                        text = "Spawn Floating Circle",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = {
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                Toast.makeText(context, "Please allow 'Display over other apps'", Toast.LENGTH_LONG).show()
                            } else {
                                FloatingCircleService.show(context)
                                Toast.makeText(context, "Floating Circle Window is now visible on screen!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            )
        }

        // How to Add Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShapeXl,
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(text = "How to Add to Control Panel", style = MaterialTheme.typography.titleMedium, color = TextBlack)
                    Spacer(modifier = Modifier.height(10.dp))

                    StepItem(step = "1", text = "Swipe down twice from the top of your screen to fully open Quick Settings.")
                    Spacer(modifier = Modifier.height(8.dp))
                    StepItem(step = "2", text = "Tap the Edit (Pencil) icon in the notification shade.")
                    Spacer(modifier = Modifier.height(8.dp))
                    StepItem(step = "3", text = "Find 'Anonymous Capture' in the available tiles list and drag it into your active tiles.")
                    Spacer(modifier = Modifier.height(8.dp))
                    StepItem(step = "4", text = "Tap the tile anytime from any app to spawn the floating circle window.")
                }
            }
        }

        // Circle Window Features Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShapeXl,
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(text = "Floating Circle Window Features", style = MaterialTheme.typography.titleMedium, color = TextBlack)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionPill(icon = Icons.Default.Camera, text = "Snapshot", color = PastelMintGreen, modifier = Modifier.weight(1f))
                        ActionPill(icon = Icons.Default.Videocam, text = "Record", color = PastelPeach, modifier = Modifier.weight(1f))
                        ActionPill(icon = Icons.Default.FolderSpecial, text = "Vault", color = PastelPowderBlue, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun StepItem(step: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = PillShape,
            color = PastelLavender,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = step, style = MaterialTheme.typography.labelSmall, color = TextBlack)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CardShapeMd,
        color = color,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = TextBlack, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = text, style = MaterialTheme.typography.titleSmall, color = TextBlack)
        }
    }
}
