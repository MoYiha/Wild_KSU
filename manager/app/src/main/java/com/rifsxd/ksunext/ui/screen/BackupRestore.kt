package com.rifsxd.ksunext.ui.screen

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.component.CardItemSpacer
import com.rifsxd.ksunext.ui.component.CardRowContent
import com.rifsxd.ksunext.ui.component.ConfirmResult
import com.rifsxd.ksunext.ui.component.StandardCard
import com.rifsxd.ksunext.ui.component.rememberConfirmDialog
import com.rifsxd.ksunext.ui.component.rememberLoadingDialog
import com.rifsxd.ksunext.ui.component.rememberNoRippleInteractionSource
import com.rifsxd.ksunext.ui.util.CustomBackup
import com.rifsxd.ksunext.ui.util.allowlistBackup
import com.rifsxd.ksunext.ui.util.allowlistRestore
import com.rifsxd.ksunext.ui.util.moduleBackup
import com.rifsxd.ksunext.ui.util.moduleRestore
import com.rifsxd.ksunext.ui.util.readMountSystemFile
import com.rifsxd.ksunext.ui.util.reboot
import kotlinx.coroutines.launch

/**
 * @author rifsxd
 * @date 2025/1/14.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BackupRestoreScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val loadingDialog = rememberLoadingDialog()
    val restoreDialog = rememberConfirmDialog()
    val backupDialog = rememberConfirmDialog()
    
    var showRebootDialog by remember { mutableStateOf(false) }
    var useOverlayFs by rememberSaveable { mutableStateOf(readMountSystemFile()) }
    
    // Helper function to convert URI to path
    fun convertUriToPath(uri: android.net.Uri): String {
        return when {
            uri.path?.contains("/tree/primary:") == true -> {
                "/sdcard/" + uri.path?.substringAfter("/tree/primary:")
            }
            uri.path?.contains("/tree/") == true -> {
                // Handle other storage locations
                uri.path?.substringAfter("/tree/")?.let { treePath ->
                    if (treePath.contains(":")) {
                        "/sdcard/" + treePath.substringAfter(":")
                    } else {
                        "/sdcard/$treePath"
                    }
                } ?: CustomBackup.getDefaultBackupLocation()
            }
            else -> CustomBackup.getDefaultBackupLocation()
        }
    }
    
    // Customizations backup folder picker
    val customizationsBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                
                val path = convertUriToPath(it)
                scope.launch {
                    loadingDialog.withLoading {
                        val backupResult = CustomBackup.createCustomBackup(
                            context = context,
                            backupLocation = path,
                            includeModules = false,
                            includeAllowlist = false,
                            includeManagerSettings = true
                        )
                        backupResult.onSuccess { filePath ->
                            android.widget.Toast.makeText(
                                context,
                                "Customizations backup created: $filePath",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }.onFailure { error ->
                            android.widget.Toast.makeText(
                                context,
                                "Customizations backup failed: ${error.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    "Failed to select folder: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // Modules backup folder picker
    val modulesBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                
                val path = convertUriToPath(it)
                scope.launch {
                    loadingDialog.withLoading {
                        val backupResult = CustomBackup.createCustomBackup(
                            context = context,
                            backupLocation = path,
                            includeModules = true,
                            includeAllowlist = false,
                            includeManagerSettings = false
                        )
                        backupResult.onSuccess { filePath ->
                            android.widget.Toast.makeText(
                                context,
                                "Modules backup created: $filePath",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }.onFailure { error ->
                            android.widget.Toast.makeText(
                                context,
                                "Modules backup failed: ${error.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    "Failed to select folder: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // Allowlist backup folder picker
    val allowlistBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                
                val path = convertUriToPath(it)
                scope.launch {
                    loadingDialog.withLoading {
                        val backupResult = CustomBackup.createCustomBackup(
                            context = context,
                            backupLocation = path,
                            includeModules = false,
                            includeAllowlist = true,
                            includeManagerSettings = false
                        )
                        backupResult.onSuccess { filePath ->
                            android.widget.Toast.makeText(
                                context,
                                "Allowlist backup created: $filePath",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }.onFailure { error ->
                            android.widget.Toast.makeText(
                                context,
                                "Allowlist backup failed: ${error.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    "Failed to select folder: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // File picker launcher for restore
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                loadingDialog.withLoading {
                    val result = CustomBackup.restoreCustomBackup(context, it.path ?: "")
                    result.onSuccess {
                        showRebootDialog = true
                    }
                }
            }
        }
    }

    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.reboot_required),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = { Text(stringResource(R.string.reboot_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRebootDialog = false
                    reboot()
                }) {
                    Text(stringResource(R.string.reboot))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        
        // Customizations Section
        item {
            StandardCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text(
                        text = "Customizations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val customBackupTitle = stringResource(R.string.create_custom_backup)
                        val customBackupMessage = stringResource(R.string.custom_backup_message)
                        val backupText = stringResource(R.string.backup)
                        
                        OutlinedButton(
                            onClick = {
                            customizationsBackupLauncher.launch(null)
                        },
                            modifier = Modifier.weight(1f)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Backup,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(backupText)
                        }
                        
                        val restoreText = stringResource(R.string.restore)
                        
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("application/zip"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(restoreText)
                        }
                    }
                }
            }
        }
        
        // Modules Section
        item {
            StandardCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text(
                        text = "Modules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val moduleBackup = stringResource(id = R.string.module_backup)
                        val backupMessage = stringResource(id = R.string.module_backup_message)
                        
                        OutlinedButton(
                            onClick = {
                                modulesBackupLauncher.launch(null)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Backup,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.backup))
                        }
                        
                        val moduleRestore = stringResource(id = R.string.module_restore)
                        val restoreMessage = stringResource(id = R.string.module_restore_message)
                        
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val result = restoreDialog.awaitConfirm(title = moduleRestore, content = restoreMessage)
                                    if (result == ConfirmResult.Confirmed) {
                                        loadingDialog.withLoading {
                                            moduleRestore()
                                            showRebootDialog = true
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.restore))
                        }
                    }
                }
            }
        }
        
        // Allowlist Section
        item {
            StandardCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text(
                        text = "Allowlist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val allowlistBackup = stringResource(id = R.string.allowlist_backup)
                        val allowlistBackupMessage = stringResource(id = R.string.allowlist_backup_message)
                        
                        OutlinedButton(
                            onClick = {
                                allowlistBackupLauncher.launch(null)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Backup,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.backup))
                        }
                        
                        val allowlistRestore = stringResource(id = R.string.allowlist_restore)
                        val allowlistRestoreMessage = stringResource(id = R.string.allowlist_restore_message)
                        
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val result = restoreDialog.awaitConfirm(title = allowlistRestore, content = allowlistRestoreMessage)
                                    if (result == ConfirmResult.Confirmed) {
                                        loadingDialog.withLoading {
                                            allowlistRestore()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.restore))
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BackupPreview() {
    BackupRestoreScreen(EmptyDestinationsNavigator)
}
