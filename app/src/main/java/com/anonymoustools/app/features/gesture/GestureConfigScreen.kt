package com.anonymoustools.app.features.gesture

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anonymoustools.app.service.gesture.GestureAccessibilityService
import com.anonymoustools.app.ui.components.PastelCard
import com.anonymoustools.app.ui.components.PillButton
import com.anonymoustools.app.ui.components.PillSwitch
import com.anonymoustools.app.ui.theme.*

@Composable
fun GestureConfigScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isHapticEnabled by remember { mutableStateOf(true) }
    var serviceActive by remember { mutableStateOf(GestureAccessibilityService.instance != null) }

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
                    Text(text = "Gesture.", style = MaterialTheme.typography.headlineLarge, color = TextBlack)
                    Text(text = "Global 3-finger trigger", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        // Gesture Hero Card
        item {
            PastelCard(
                title = "3-Finger Horizontal Swipe",
                description = "Swipe right horizontally with 3 fingers anywhere on screen to snap a silent screenshot over any application.",
                backgroundColor = PastelPowderBlue,
                badgeText = if (serviceActive) "ENABLED" else "REQUIRES SERVICE",
                icon = Icons.Default.PanTool,
                actionContent = {
                    Column {
                        // Gesture Visualizer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CardShapeMd)
                                .background(CardWhite.copy(alpha = 0.7f))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedGestureDemo()
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        PillButton(
                            text = "Test Silent Snapshot",
                            onClick = {
                                val acc = GestureAccessibilityService.instance
                                if (acc != null) {
                                    acc.takeSilentScreenshot { file ->
                                        android.widget.Toast.makeText(
                                            context,
                                            if (file != null) "Silent screenshot captured to Vault!" else "Snapshot failed",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Please enable Accessibility Service below first",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            )
        }

        // Accessibility Service Setup Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShapeXl,
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Accessibility Service Setup",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextBlack
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enabling Accessibility allows the app to take silent screenshots with zero screen-share casting dialogs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = PillShape,
                            color = if (serviceActive) PastelMintGreen else PastelPeach
                        ) {
                            Text(
                                text = if (serviceActive) "ACTIVE" else "NOT ENABLED",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextBlack,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        PillButton(
                            text = if (serviceActive) "Open Settings" else "Enable in Settings",
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }

        // Preferences Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShapeXl,
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(text = "Preferences", style = MaterialTheme.typography.titleMedium, color = TextBlack)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Haptic Vibration", style = MaterialTheme.typography.titleSmall, color = TextBlack)
                            Text(text = "Subtle vibration on snap", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        PillSwitch(checked = isHapticEnabled, onCheckedChange = { isHapticEnabled = it })
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Gesture Direction", style = MaterialTheme.typography.titleSmall, color = TextBlack)
                            Text(text = "Horizontal Right", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Text(text = "→ Right", style = MaterialTheme.typography.titleMedium, color = TextBlack)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedGestureDemo() {
    val infiniteTransition = rememberInfiniteTransition(label = "gesture")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.offset(x = offsetX.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(9.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TextBlack)
                )
            }
        }
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = "SWIPE RIGHT → SNAP",
            style = MaterialTheme.typography.labelLarge,
            color = TextBlack
        )
    }
}
