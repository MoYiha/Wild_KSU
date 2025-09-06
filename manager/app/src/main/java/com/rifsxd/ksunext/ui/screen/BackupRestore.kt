package com.rifsxd.ksunext.ui.screen

import android.content.Context
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
    
    // Custom backup states
    var customBackupLocation by remember { mutableStateOf(CustomBackup.getCustomBackupLocation(context)) }
    var includeModules by remember { mutableStateOf(true) }
    var includeAllowlist by remember { mutableStateOf(true) }
    var includeManagerSettings by remember { mutableStateOf(true) }
    
    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = it.path?.replace("/tree/primary:", "/sdcard/") ?: CustomBackup.getDefaultBackupLocation()
            customBackupLocation = path
            CustomBackup.setCustomBackupLocation(context, path)
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
        item {
            StandardCard {

                val moduleBackup = stringResource(id = R.string.module_backup)
                val backupMessage = stringResource(id = R.string.module_backup_message)
                CardRowContent(
                    icon = Icons.Filled.Backup,
                    text = moduleBackup,
                    modifier = Modifier.clickable(
                        onClick = {
                            scope.launch {
                                val result = backupDialog.awaitConfirm(title = moduleBackup, content = backupMessage)
                                if (result == ConfirmResult.Confirmed) {
                                    loadingDialog.withLoading {
                                        moduleBackup()
                                    }
                                }
                            }
                        },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )

                CardItemSpacer()

                val moduleRestore = stringResource(id = R.string.module_restore)
                val restoreMessage = stringResource(id = R.string.module_restore_message)

                CardRowContent(
                    icon = Icons.Filled.Restore,
                    text = moduleRestore,
                    modifier = Modifier.clickable(
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
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )

            }
        }
        
        item {
            StandardCard {
                val allowlistBackup = stringResource(id = R.string.allowlist_backup)
                val allowlistBackupMessage = stringResource(id = R.string.allowlist_backup_message)
                CardRowContent(
                    icon = Icons.Filled.Backup,
                    text = allowlistBackup,
                    modifier = Modifier.clickable(
                        onClick = {
                            scope.launch {
                                val result = backupDialog.awaitConfirm(title = allowlistBackup, content = allowlistBackupMessage)
                                if (result == ConfirmResult.Confirmed) {
                                    loadingDialog.withLoading {
                                        allowlistBackup()
                                    }
                                }
                            }
                        },
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )

                CardItemSpacer()

                val allowlistRestore = stringResource(id = R.string.allowlist_restore)
                val allowlistRestoreMessage = stringResource(id = R.string.allowlist_restore_message)
                CardRowContent(
                    icon = Icons.Filled.Restore,
                    text = allowlistRestore,
                    modifier = Modifier.clickable(
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
                        interactionSource = rememberNoRippleInteractionSource(),
                        indication = null
                    )
                )
            }
        }
        
        // Custom Backup Section
        item {
            StandardCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.custom_backup),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Backup location selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.backup_location),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) }
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.select_folder))
                        }
                    }
                    
                    Text(
                        text = customBackupLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Backup options
                    Text(
                        text = stringResource(R.string.backup_options),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeModules,
                            onCheckedChange = { includeModules = it }
                        )
                        Text(stringResource(R.string.include_modules))
                    }
                    
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeAllowlist,
                            onCheckedChange = { includeAllowlist = it }
                        )
                        Text(stringResource(R.string.include_allowlist))
                    }
                    
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeManagerSettings,
                            onCheckedChange = { includeManagerSettings = it }
                        )
                        Text(stringResource(R.string.include_manager_settings))
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Custom backup and restore buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val customBackupTitle = stringResource(R.string.create_custom_backup)
                        val customBackupMessage = stringResource(R.string.custom_backup_message)
                        val backupText = stringResource(R.string.backup)
                        
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val result = backupDialog.awaitConfirm(
                                        title = customBackupTitle,
                                        content = customBackupMessage
                                    )
                                    if (result == ConfirmResult.Confirmed) {
                                        loadingDialog.withLoading {
                                            CustomBackup.createCustomBackup(
                                                context = context,
                                                backupLocation = customBackupLocation,
                                                includeModules = includeModules,
                                                includeAllowlist = includeAllowlist,
                                                includeManagerSettings = includeManagerSettings
                                            )
                                        }
                                    }
                                }
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
    }
}

@Preview
@Composable
private fun BackupPreview() {
    BackupRestoreScreen(EmptyDestinationsNavigator)
}
