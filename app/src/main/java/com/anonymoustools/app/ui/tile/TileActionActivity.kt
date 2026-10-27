package com.anonymoustools.app.ui.tile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.anonymoustools.app.MainActivity
import com.anonymoustools.app.core.capture.CaptureManager
import com.anonymoustools.app.core.vault.PrivateVaultManager
import com.anonymoustools.app.ui.theme.*

class TileActionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AnonymousToolsTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { finish() },
                    contentAlignment = Alignment.Center
                ) {
                    TileActionModal(
                        onSnapshot = {
                            CaptureManager.takeSilentSnapshot(this@TileActionActivity) { file ->
                                Toast.makeText(
                                    this@TileActionActivity,
                                    if (file != null) "Silent snapshot saved to Vault!" else "Activate capture in app first",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        },
                        onToggleRecord = {
                            if (CaptureManager.isRecording) {
                                CaptureManager.stopScreenRecording()
                                Toast.makeText(this@TileActionActivity, "Recording stopped & saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                val file = PrivateVaultManager.createVideoOutputFile(this@TileActionActivity)
                                val ok = CaptureManager.startScreenRecording(this@TileActionActivity, file)
                                Toast.makeText(
                                    this@TileActionActivity,
                                    if (ok) "Screen recording started..." else "Activate capture in app first",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            finish()
                        },
                        onOpenVault = {
                            val intent = Intent(this@TileActionActivity, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra("target_screen", "vault")
                            }
                            startActivity(intent)
                            finish()
                        },
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun TileActionModal(
    onSnapshot: () -> Unit,
    onToggleRecord: () -> Unit,
    onOpenVault: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable(enabled = false) {}, // Prevent dismiss when tapping inside card
        shape = CardShapeXl,
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONTROL PANEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Anonymous Capture",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextBlack
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextBlack)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TileButton(
                    title = "Snapshot",
                    subtitle = "Silent capture",
                    color = PastelMintGreen,
                    icon = Icons.Default.Camera,
                    onClick = onSnapshot,
                    modifier = Modifier.weight(1f)
                )

                TileButton(
                    title = if (CaptureManager.isRecording) "Stop" else "Record",
                    subtitle = if (CaptureManager.isRecording) "Active now" else "1080p Video",
                    color = if (CaptureManager.isRecording) PastelDustyPink else PastelPeach,
                    icon = Icons.Default.Videocam,
                    onClick = onToggleRecord,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TileButton(
                title = "Private Vault",
                subtitle = "View protected screenshots & recordings",
                color = PastelPowderBlue,
                icon = Icons.Default.FolderSpecial,
                onClick = onOpenVault,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TileButton(
    title: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CardShapeLg)
            .background(color)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Icon(imageVector = icon, contentDescription = null, tint = TextBlack, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextBlack)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}
