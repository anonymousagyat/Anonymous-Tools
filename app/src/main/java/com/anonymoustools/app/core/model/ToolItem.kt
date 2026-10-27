package com.anonymoustools.app.core.model

import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolCategory(val title: String) {
    PRIVACY("Privacy & Media"),
    SECURITY("Security & Crypto"),
    NETWORK("Network & Identity"),
    DEVICE("Audit & System")
}

data class ToolItem(
    val id: String,
    val title: String,
    val description: String,
    val category: ToolCategory,
    val iconName: String,
    val route: String,
    val badge: String? = null
)
