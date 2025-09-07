package com.rifsxd.ksunext.ui.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
// import androidx.documentfile.provider.DocumentFile // Not needed for this implementation
import com.rifsxd.ksunext.ksuApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object CustomBackup {
    
    private const val BACKUP_METADATA_FILE = "backup_metadata.json"
    private const val MANAGER_SETTINGS_FILE = "manager_settings.json"
    private const val BACKGROUND_IMAGE_FILE = "background_image"
    
    data class BackupMetadata(
        val version: String = "1.0",
        val timestamp: Long = System.currentTimeMillis(),
        val appVersion: String = "",
        val includesModules: Boolean = false,
        val includesAllowlist: Boolean = false,
        val includesManagerSettings: Boolean = false,
        val includesBackgroundImage: Boolean = false
    )
    
    data class ManagerSettings(
        val backgroundImageUri: String? = null,
        val backgroundFitMode: String? = null,
        val backgroundTransparency: Float = 1.0f,
        val backgroundBlur: Float = 0f,
        val selectedIconType: String? = null,
        val hideBottomBar: Boolean = false,
        val customizations: Map<String, Any> = emptyMap()
    )
    
    fun getDefaultBackupLocation(): String {
        return "/data/adb/ksu"
    }
    
    fun getCustomBackupLocation(context: Context): String {
        val prefs = context.getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
        return prefs.getString("custom_backup_location", null) ?: getDefaultBackupLocation()
    }
    
    fun setCustomBackupLocation(context: Context, location: String) {
        val prefs = context.getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("custom_backup_location", location).apply()
    }
    
    suspend fun createCustomBackup(
        context: Context,
        backupLocation: String,
        includeModules: Boolean = true,
        includeAllowlist: Boolean = true,
        includeManagerSettings: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "Wild_KSU_backup_$timestamp.zip"
            val backupFile = File(backupLocation, backupFileName)
            
            // Ensure backup directory exists
            val parentDir = backupFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                val created = parentDir.mkdirs()
                if (!created && !parentDir.exists()) {
                    return@withContext Result.failure(Exception("Failed to create backup directory: $backupLocation"))
                }
            }
            
            // Check if we can write to the directory
            if (parentDir != null && !parentDir.canWrite()) {
                return@withContext Result.failure(Exception("No write permission for backup directory: $backupLocation"))
            }
            
            val metadata = BackupMetadata(
                appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown",
                includesModules = includeModules,
                includesAllowlist = includeAllowlist,
                includesManagerSettings = includeManagerSettings,
                includesBackgroundImage = includeManagerSettings
            )
            
            ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
                // Add metadata
                addMetadataToZip(zipOut, metadata)
                
                // Add modules backup if requested
                if (includeModules) {
                    addModulesToZip(zipOut)
                }
                
                // Add allowlist backup if requested
                if (includeAllowlist) {
                    addAllowlistToZip(zipOut)
                }
                
                // Add manager settings if requested
                if (includeManagerSettings) {
                    addManagerSettingsToZip(zipOut, context)
                }
            }
            
            Result.success(backupFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun restoreCustomBackup(
        context: Context,
        backupFilePath: String
    ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(backupFilePath)
            if (!backupFile.exists()) {
                return@withContext Result.failure(Exception("Backup file not found"))
            }
            
            var metadata: BackupMetadata? = null
            
            ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    when (entry?.name) {
                        BACKUP_METADATA_FILE -> {
                            metadata = parseMetadata(zipIn.readBytes().toString(Charsets.UTF_8))
                        }
                        "modules_backup.tar" -> {
                            if (metadata?.includesModules == true) {
                                restoreModulesFromZip(zipIn)
                            }
                        }
                        "allowlist_backup.tar" -> {
                            if (metadata?.includesAllowlist == true) {
                                restoreAllowlistFromZip(zipIn)
                            }
                        }
                        MANAGER_SETTINGS_FILE -> {
                            if (metadata?.includesManagerSettings == true) {
                                restoreManagerSettingsFromZip(zipIn, context)
                            }
                        }
                        BACKGROUND_IMAGE_FILE -> {
                            if (metadata?.includesBackgroundImage == true) {
                                restoreBackgroundImageFromZip(zipIn, context)
                            }
                        }
                    }
                    zipIn.closeEntry()
                }
            }
            
            metadata?.let { Result.success(it) } ?: Result.failure(Exception("Invalid backup file"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun addMetadataToZip(zipOut: ZipOutputStream, metadata: BackupMetadata) {
        val metadataJson = JSONObject().apply {
            put("version", metadata.version)
            put("timestamp", metadata.timestamp)
            put("appVersion", metadata.appVersion)
            put("includesModules", metadata.includesModules)
            put("includesAllowlist", metadata.includesAllowlist)
            put("includesManagerSettings", metadata.includesManagerSettings)
            put("includesBackgroundImage", metadata.includesBackgroundImage)
        }
        
        zipOut.putNextEntry(ZipEntry(BACKUP_METADATA_FILE))
        zipOut.write(metadataJson.toString().toByteArray())
        zipOut.closeEntry()
    }
    
    private fun addModulesToZip(zipOut: ZipOutputStream) {
        // Create temporary tar file for modules
        val tempTarFile = File.createTempFile("modules_backup", ".tar")
        try {
            val tarCmd = "busybox tar -cpf ${tempTarFile.absolutePath} -C /data/adb/modules \$(ls /data/adb/modules)"
            if (Runtime.getRuntime().exec(tarCmd).waitFor() == 0) {
                zipOut.putNextEntry(ZipEntry("modules_backup.tar"))
                tempTarFile.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        } finally {
            tempTarFile.delete()
        }
    }
    
    private fun addAllowlistToZip(zipOut: ZipOutputStream) {
        // Create temporary tar file for allowlist
        val tempTarFile = File.createTempFile("allowlist_backup", ".tar")
        try {
            val tarCmd = "busybox tar -cpf ${tempTarFile.absolutePath} -C /data/adb/ksu .allowlist"
            if (Runtime.getRuntime().exec(tarCmd).waitFor() == 0) {
                zipOut.putNextEntry(ZipEntry("allowlist_backup.tar"))
                tempTarFile.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        } finally {
            tempTarFile.delete()
        }
    }
    
    private fun addManagerSettingsToZip(zipOut: ZipOutputStream, context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        // Collect all settings from the customization screens
        val allSettings = mutableMapOf<String, Any>()
        
        // Theme Settings preferences
        prefs.getString("theme_mode", null)?.let { allSettings["theme_mode"] = it }
        prefs.getString("background_image_uri", null)?.let { allSettings["background_image_uri"] = it }
        allSettings["background_transparency"] = prefs.getFloat("background_transparency", 0.0f)
        allSettings["background_blur"] = prefs.getFloat("background_blur", 0.0f)
        allSettings["ui_transparency"] = prefs.getFloat("ui_transparency", 0.0f)
        allSettings["hide_bottom_bar"] = prefs.getBoolean("hide_bottom_bar", false)
        allSettings["app_dpi"] = prefs.getInt("app_dpi", 160)
        allSettings["original_system_dpi"] = prefs.getInt("original_system_dpi", 160)
        
        // Home Settings preferences
        allSettings["info_card_always_expanded"] = prefs.getBoolean("info_card_always_expanded", false)
        allSettings["show_help_card"] = prefs.getBoolean("show_help_card", true)
        prefs.getString("selected_icon_type", null)?.let { allSettings["selected_icon_type"] = it }
        prefs.getString("home_layout_type", null)?.let { allSettings["home_layout_type"] = it }
        prefs.getString("info_card_item_order", null)?.let { allSettings["info_card_item_order"] = it }
        
        // Superuser Settings preferences
        allSettings["use_individual_app_cards"] = prefs.getBoolean("use_individual_app_cards", false)
        allSettings["disable_favorite_button"] = prefs.getBoolean("disable_favorite_button", false)
        allSettings["show_system_apps"] = prefs.getBoolean("show_system_apps", false)
        prefs.getString("favorite_apps", null)?.let { allSettings["favorite_apps"] = it }
        
        // Module Settings preferences
        allSettings["keep_modules_expanded"] = prefs.getBoolean("keep_modules_expanded", false)
        allSettings["use_banner"] = prefs.getBoolean("use_banner", true)
        allSettings["hide_module_details"] = prefs.getBoolean("hide_module_details", false)
        
        // Customization Settings (language, etc.)
        prefs.getString("language", null)?.let { allSettings["language"] = it }
        
        // Add any other preferences that might exist
        prefs.all.forEach { (key, value) ->
            if (!allSettings.containsKey(key) && value != null) {
                allSettings[key] = value
            }
        }
        
        val settingsJson = JSONObject(allSettings)
        
        zipOut.putNextEntry(ZipEntry(MANAGER_SETTINGS_FILE))
        zipOut.write(settingsJson.toString().toByteArray())
        zipOut.closeEntry()
        
        // Add background image if exists
        allSettings["background_image_uri"]?.let { uriString ->
            try {
                val uri = Uri.parse(uriString.toString())
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    zipOut.putNextEntry(ZipEntry(BACKGROUND_IMAGE_FILE))
                    inputStream.copyTo(zipOut)
                    zipOut.closeEntry()
                }
            } catch (e: Exception) {
                // Background image not accessible, try internal storage
                try {
                    val internalFile = File(context.filesDir, "background_image")
                    if (internalFile.exists()) {
                        internalFile.inputStream().use { inputStream ->
                            zipOut.putNextEntry(ZipEntry(BACKGROUND_IMAGE_FILE))
                            inputStream.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }
                } catch (e2: Exception) {
                    // Background image not accessible from internal storage either, skip
                }
            }
        }
    }
    
    private fun parseMetadata(jsonString: String): BackupMetadata {
        val json = JSONObject(jsonString)
        return BackupMetadata(
            version = json.optString("version", "1.0"),
            timestamp = json.optLong("timestamp", 0),
            appVersion = json.optString("appVersion", ""),
            includesModules = json.optBoolean("includesModules", false),
            includesAllowlist = json.optBoolean("includesAllowlist", false),
            includesManagerSettings = json.optBoolean("includesManagerSettings", false),
            includesBackgroundImage = json.optBoolean("includesBackgroundImage", false)
        )
    }
    
    private fun restoreModulesFromZip(zipIn: ZipInputStream) {
        val tempTarFile = File.createTempFile("modules_restore", ".tar")
        try {
            tempTarFile.outputStream().use { zipIn.copyTo(it) }
            val extractCmd = "busybox tar -xpf ${tempTarFile.absolutePath} -C /data/adb/modules_update"
            Runtime.getRuntime().exec(extractCmd).waitFor()
        } finally {
            tempTarFile.delete()
        }
    }
    
    private fun restoreAllowlistFromZip(zipIn: ZipInputStream) {
        val tempTarFile = File.createTempFile("allowlist_restore", ".tar")
        try {
            tempTarFile.outputStream().use { zipIn.copyTo(it) }
            val extractCmd = "busybox tar -xpf ${tempTarFile.absolutePath} -C /data/adb/ksu"
            Runtime.getRuntime().exec(extractCmd).waitFor()
        } finally {
            tempTarFile.delete()
        }
    }
    
    private fun restoreManagerSettingsFromZip(zipIn: ZipInputStream, context: Context) {
        val settingsJson = JSONObject(zipIn.readBytes().toString(Charsets.UTF_8))
        
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        prefs.edit().apply {
            // Restore all settings from the JSON
            settingsJson.keys().forEach { key ->
                when (val value = settingsJson.get(key)) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Double -> {
                        // Handle both Float and Double values
                        if (key.contains("transparency") || key.contains("blur")) {
                            putFloat(key, value.toFloat())
                        } else {
                            putLong(key, value.toLong())
                        }
                    }
                    is Long -> putLong(key, value)
                }
            }
            apply()
        }
    }
    
    private fun restoreBackgroundImageFromZip(zipIn: ZipInputStream, context: Context) {
        try {
            // Save background image to app's private directory
            val backgroundFile = File(context.filesDir, "background_image")
            backgroundFile.outputStream().use { zipIn.copyTo(it) }
            
            // Update the background image URI to point to the restored file
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("background_image_uri", Uri.fromFile(backgroundFile).toString())
                .apply()
        } catch (e: Exception) {
            // Failed to restore background image, continue without it
        }
    }
}