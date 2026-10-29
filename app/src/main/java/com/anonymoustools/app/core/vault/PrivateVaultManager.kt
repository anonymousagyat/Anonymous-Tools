package com.anonymoustools.app.core.vault

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.anonymoustools.app.core.model.CaptureItem
import com.anonymoustools.app.core.model.CaptureType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PrivateVaultManager {

    private const val VAULT_DIR_NAME = "anonymous_vault"
    private const val PREFS_EXPORTED = "vault_export_history"
    private const val KEY_EXPORTED_SET = "exported_files"
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun getExportedSet(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_EXPORTED, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_EXPORTED_SET, emptySet()) ?: emptySet()
    }

    fun isExported(context: Context, fileName: String): Boolean {
        return getExportedSet(context).contains(fileName)
    }

    fun markAsExported(context: Context, fileName: String) {
        val prefs = context.getSharedPreferences(PREFS_EXPORTED, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_EXPORTED_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(fileName)
        prefs.edit().putStringSet(KEY_EXPORTED_SET, current).apply()
    }

    fun unmarkExported(context: Context, fileName: String) {
        val prefs = context.getSharedPreferences(PREFS_EXPORTED, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_EXPORTED_SET, emptySet())?.toMutableSet() ?: return
        if (current.remove(fileName)) {
            prefs.edit().putStringSet(KEY_EXPORTED_SET, current).apply()
        }
    }

    fun clearExportedHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_EXPORTED, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_EXPORTED_SET).apply()
    }

    fun getVaultDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), VAULT_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        // Ensure .nomedia file exists to hide from Android media indexers
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) {
            noMedia.createNewFile()
        }
        return dir
    }

    fun saveSnapshot(context: Context, bitmap: Bitmap): File {
        val dir = getVaultDir(context)
        val timestamp = System.currentTimeMillis()
        val filename = "Snap_${dateFormat.format(Date(timestamp))}.png"
        val file = File(dir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    fun createVideoOutputFile(context: Context): File {
        val dir = getVaultDir(context)
        val timestamp = System.currentTimeMillis()
        val filename = "Rec_${dateFormat.format(Date(timestamp))}.mp4"
        return File(dir, filename)
    }

    fun listVaultItems(context: Context): List<CaptureItem> {
        val dir = getVaultDir(context)
        val files = dir.listFiles { f ->
            f.isFile && (f.extension.equals("png", ignoreCase = true) ||
                         f.extension.equals("jpg", ignoreCase = true) ||
                         f.extension.equals("mp4", ignoreCase = true))
        } ?: return emptyList()

        val exportedSet = getExportedSet(context)

        return files.map { file ->
            val type = if (file.extension.equals("mp4", ignoreCase = true)) {
                CaptureType.VIDEO
            } else {
                CaptureType.SNAPSHOT
            }
            CaptureItem(
                file = file,
                name = file.name,
                type = type,
                sizeBytes = file.length(),
                timestamp = file.lastModified(),
                isExported = exportedSet.contains(file.name)
            )
        }.sortedByDescending { it.timestamp }
    }

    fun deleteItem(context: Context? = null, file: File): Boolean {
        if (context != null) {
            unmarkExported(context, file.name)
        }
        return file.exists() && file.delete()
    }

    fun shredVault(context: Context): Int {
        clearExportedHistory(context)
        val dir = getVaultDir(context)
        var count = 0
        dir.listFiles()?.forEach { file ->
            if (file.name != ".nomedia") {
                if (file.delete()) count++
            }
        }
        return count
    }

    fun exportToPublicGallery(context: Context, item: CaptureItem): Boolean {
        return try {
            val resolver = context.contentResolver
            val success = if (item.type == CaptureType.SNAPSHOT) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, item.name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AnonymousTools")
                    }
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(item.file).use { input -> input.copyTo(out) }
                }
                true
            } else {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, item.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/AnonymousTools")
                    }
                }
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(item.file).use { input -> input.copyTo(out) }
                }
                true
            }
            if (success) {
                markAsExported(context, item.name)
            }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
