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
        return "${Environment.getExternalStorageDirectory()}/Download/backup/Wild_KSU"
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
            backupFile.parentFile?.mkdirs()
            
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
        val backgroundPrefs = context.getSharedPreferences("background_settings", Context.MODE_PRIVATE)
        
        val settings = ManagerSettings(
            backgroundImageUri = backgroundPrefs.getString("background_image_uri", null),
            backgroundFitMode = backgroundPrefs.getString("background_fit_mode", "CENTER_CROP"),
            backgroundTransparency = backgroundPrefs.getFloat("background_transparency", 1.0f),
            backgroundBlur = backgroundPrefs.getFloat("background_blur", 0f),
            selectedIconType = prefs.getString("selected_icon_type", "default"),
            hideBottomBar = prefs.getBoolean("hide_bottom_bar", false),
            customizations = prefs.all.filterKeys { !it.startsWith("background_") }.mapValues { it.value ?: "" }
        )
        
        val settingsJson = JSONObject().apply {
            put("backgroundImageUri", settings.backgroundImageUri)
            put("backgroundFitMode", settings.backgroundFitMode)
            put("backgroundTransparency", settings.backgroundTransparency)
            put("backgroundBlur", settings.backgroundBlur)
            put("selectedIconType", settings.selectedIconType)
            put("hideBottomBar", settings.hideBottomBar)
            put("customizations", JSONObject(settings.customizations))
        }
        
        zipOut.putNextEntry(ZipEntry(MANAGER_SETTINGS_FILE))
        zipOut.write(settingsJson.toString().toByteArray())
        zipOut.closeEntry()
        
        // Add background image if exists
        settings.backgroundImageUri?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    zipOut.putNextEntry(ZipEntry(BACKGROUND_IMAGE_FILE))
                    inputStream.copyTo(zipOut)
                    zipOut.closeEntry()
                }
            } catch (e: Exception) {
                // Background image not accessible, skip
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
        val backgroundPrefs = context.getSharedPreferences("background_settings", Context.MODE_PRIVATE)
        
        backgroundPrefs.edit().apply {
            putString("background_image_uri", settingsJson.optString("backgroundImageUri", null))
            putString("background_fit_mode", settingsJson.optString("backgroundFitMode", "CENTER_CROP"))
            putFloat("background_transparency", settingsJson.optDouble("backgroundTransparency", 1.0).toFloat())
            putFloat("background_blur", settingsJson.optDouble("backgroundBlur", 0.0).toFloat())
            apply()
        }
        
        prefs.edit().apply {
            putString("selected_icon_type", settingsJson.optString("selectedIconType", "default"))
            putBoolean("hide_bottom_bar", settingsJson.optBoolean("hideBottomBar", false))
            
            // Restore other customizations
            val customizations = settingsJson.optJSONObject("customizations")
            customizations?.keys()?.forEach { key ->
                when (val value = customizations.get(key)) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Float -> putFloat(key, value)
                    is Long -> putLong(key, value)
                }
            }
            apply()
        }
    }
    
    private fun restoreBackgroundImageFromZip(zipIn: ZipInputStream, context: Context) {
        try {
            // Save background image to app's private directory
            val backgroundFile = File(context.filesDir, "restored_background_image")
            backgroundFile.outputStream().use { zipIn.copyTo(it) }
            
            // Update the background image URI to point to the restored file
            val backgroundPrefs = context.getSharedPreferences("background_settings", Context.MODE_PRIVATE)
            backgroundPrefs.edit()
                .putString("background_image_uri", Uri.fromFile(backgroundFile).toString())
                .apply()
        } catch (e: Exception) {
            // Failed to restore background image, continue without it
        }
    }
}