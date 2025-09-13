package com.rifsxd.ksunext.ui.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class for backing up and restoring customization settings
 * Saves settings to /data/adb/ksu/backup/customization.json
 * and background images to /data/adb/ksu/backup/
 */
object CustomizationBackup {
    private const val TAG = "CustomizationBackup"
    private const val BACKUP_DIR = "/data/adb/ksu/backup"
    private const val CUSTOMIZATION_FILE = "customization.json"
    private const val BACKGROUND_IMAGE_FILE = "background"
    
    /**
     * All customization settings keys that should be backed up
     */
    private val SETTINGS_KEYS = listOf(
        // Language settings
        "app_language",
        
        // Theme settings
        "theme_mode",
        "background_image_uri",
        "background_transparency",
        "background_blur",
        "hide_bottom_bar",
        
        // Home settings
        "info_card_always_expanded",
        "show_help_card",
        "selected_icon_type",
        "home_layout_type",
        "info_card_show_manager_version",
        "info_card_show_hook_mode",
        "info_card_show_mount_system",
        "info_card_show_susfs_status",
        "info_card_show_zygisk_status",
        "info_card_show_kernel_version",
        "info_card_show_android_version",
        "info_card_show_abi",
        "info_card_show_selinux_status",
        "selected_app_name",
        
        // Superuser settings
        "show_system_apps",
        "favorite_apps",
        "use_individual_app_cards",
        
        // Module settings
        "keep_modules_expanded",
        "use_banner",
        "hide_module_details"
    )
    
    /**
     * Backup all customization settings to JSON file
     */
    suspend fun backupCustomizationSettings(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val backupDir = File(BACKUP_DIR)
            
            // Create backup directory if it doesn't exist
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            // Create JSON object with all settings
            val jsonObject = JSONObject()
            
            SETTINGS_KEYS.forEach { key ->
                when {
                    prefs.contains(key) -> {
                        val value = prefs.all[key]
                        when (value) {
                            is String -> jsonObject.put(key, value)
                            is Boolean -> jsonObject.put(key, value)
                            is Float -> jsonObject.put(key, value.toDouble())
                            is Int -> jsonObject.put(key, value)
                            is Set<*> -> {
                                // Handle string sets (like favorite_apps)
                                @Suppress("UNCHECKED_CAST")
                                val stringSet = value as Set<String>
                                jsonObject.put(key, stringSet.joinToString(","))
                            }
                        }
                    }
                }
            }
            
            // Add metadata
            jsonObject.put("backup_timestamp", System.currentTimeMillis())
            jsonObject.put("app_version", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
            
            // Write to file with readable formatting
            val backupFile = File(backupDir, CUSTOMIZATION_FILE)
            backupFile.writeText(jsonObject.toString(2)) // Pretty print with 2-space indentation
            
            // Backup background image if it exists
            val backgroundUri = prefs.getString("background_image_uri", null)
            if (backgroundUri != null) {
                backupBackgroundImage(context, backgroundUri)
            }
            
            Log.i(TAG, "Customization settings backed up successfully to ${backupFile.absolutePath}")
            Result.success("Customization settings backed up successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup customization settings", e)
            Result.failure(e)
        }
    }
    
    /**
     * Restore customization settings from JSON file
     */
    suspend fun restoreCustomizationSettings(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(BACKUP_DIR, CUSTOMIZATION_FILE)
            
            if (!backupFile.exists()) {
                return@withContext Result.failure(Exception("No customization backup found"))
            }
            
            val jsonContent = backupFile.readText()
            val jsonObject = JSONObject(jsonContent)
            
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            // Restore all settings
            SETTINGS_KEYS.forEach { key ->
                if (jsonObject.has(key)) {
                    when (key) {
                        "background_transparency", "background_blur" -> {
                            // Handle float values
                            val value = jsonObject.getDouble(key).toFloat()
                            editor.putFloat(key, value)
                        }
                        "info_card_always_expanded", "show_help_card", "info_card_show_manager_version",
                        "info_card_show_hook_mode", "info_card_show_mount_system", "info_card_show_susfs_status",
                        "info_card_show_zygisk_status", "info_card_show_kernel_version", "info_card_show_android_version",
                        "info_card_show_abi", "info_card_show_selinux_status", "show_system_apps",
                        "use_individual_app_cards", "keep_modules_expanded", "use_banner",
                        "hide_module_details", "hide_bottom_bar" -> {
                            // Handle boolean values
                            val value = jsonObject.getBoolean(key)
                            editor.putBoolean(key, value)
                        }
                        "favorite_apps" -> {
                            // Handle string sets
                            val value = jsonObject.getString(key)
                            val stringSet = if (value.isNotEmpty()) {
                                value.split(",").toSet()
                            } else {
                                emptySet()
                            }
                            editor.putStringSet(key, stringSet)
                        }
                        else -> {
                            // Handle string values
                            val value = jsonObject.getString(key)
                            editor.putString(key, value)
                        }
                    }
                }
            }
            
            editor.apply()
            
            // Restore background image if it exists
            restoreBackgroundImage(context)
            
            Log.i(TAG, "Customization settings restored successfully")
            Result.success("Customization settings restored successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore customization settings", e)
            Result.failure(e)
        }
    }
    
    /**
     * Backup background image to backup directory
     */
    private suspend fun backupBackgroundImage(context: Context, imageUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            
            if (inputStream != null) {
                val backupDir = File(BACKUP_DIR)
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }
                
                // Determine file extension from URI or default to jpg
                val extension = when {
                    imageUri.contains(".png", ignoreCase = true) -> "png"
                    imageUri.contains(".jpg", ignoreCase = true) || imageUri.contains(".jpeg", ignoreCase = true) -> "jpg"
                    else -> "jpg"
                }
                
                val backupFile = File(backupDir, "$BACKGROUND_IMAGE_FILE.$extension")
                val outputStream = FileOutputStream(backupFile)
                
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
                
                Log.i(TAG, "Background image backed up to ${backupFile.absolutePath}")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup background image", e)
            false
        }
    }
    
    /**
     * Restore background image from backup directory
     */
    private suspend fun restoreBackgroundImage(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(BACKUP_DIR)
            
            // Look for background image files
            val possibleExtensions = listOf("jpg", "jpeg", "png")
            var backupFile: File? = null
            
            for (ext in possibleExtensions) {
                val file = File(backupDir, "$BACKGROUND_IMAGE_FILE.$ext")
                if (file.exists()) {
                    backupFile = file
                    break
                }
            }
            
            if (backupFile != null && backupFile.exists()) {
                // Copy to internal storage using BackgroundCustomization utility
                val restoredPath = BackgroundCustomization.copyFileToInternalStorage(context, backupFile)
                
                if (restoredPath != null) {
                    val restoredUri = BackgroundCustomization.filePathToUri(restoredPath)
                    
                    // Update SharedPreferences with restored image URI
                    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    prefs.edit().putString("background_image_uri", restoredUri.toString()).apply()
                    
                    Log.i(TAG, "Background image restored successfully")
                    true
                } else {
                    false
                }
            } else {
                Log.i(TAG, "No background image backup found")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore background image", e)
            false
        }
    }
    
    /**
     * Check if customization backup exists
     */
    fun hasCustomizationBackup(): Boolean {
        val backupFile = File(BACKUP_DIR, CUSTOMIZATION_FILE)
        return backupFile.exists()
    }
    
    /**
     * Get backup file info
     */
    fun getBackupInfo(): BackupInfo? {
        try {
            val backupFile = File(BACKUP_DIR, CUSTOMIZATION_FILE)
            if (!backupFile.exists()) return null
            
            val jsonContent = backupFile.readText()
            val jsonObject = JSONObject(jsonContent)
            
            val timestamp = jsonObject.optLong("backup_timestamp", 0L)
            val appVersion = jsonObject.optString("app_version", "Unknown")
            
            return BackupInfo(
                timestamp = timestamp,
                appVersion = appVersion,
                fileSize = backupFile.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get backup info", e)
            return null
        }
    }
    
    data class BackupInfo(
        val timestamp: Long,
        val appVersion: String,
        val fileSize: Long
    )
}

/**
 * Extension function for BackgroundCustomization to copy files
 */
private fun BackgroundCustomization.copyFileToInternalStorage(context: Context, sourceFile: File): String? {
    return try {
        val internalDir = File(context.filesDir, "backgrounds")
        if (!internalDir.exists()) {
            internalDir.mkdirs()
        }
        
        val targetFile = File(internalDir, "background_${System.currentTimeMillis()}.${sourceFile.extension}")
        sourceFile.copyTo(targetFile, overwrite = true)
        
        targetFile.absolutePath
    } catch (e: Exception) {
        Log.e("BackgroundCustomization", "Failed to copy file to internal storage", e)
        null
    }
}