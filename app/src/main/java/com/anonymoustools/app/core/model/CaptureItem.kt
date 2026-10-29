package com.anonymoustools.app.core.model

import java.io.File

enum class CaptureType {
    SNAPSHOT,
    VIDEO
}

data class CaptureItem(
    val file: File,
    val name: String,
    val type: CaptureType,
    val sizeBytes: Long,
    val timestamp: Long,
    val isExported: Boolean = false
)
