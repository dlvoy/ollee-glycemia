package pl.cukrzycowy.ollee.glycemia.ui.components

import android.util.Log
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import pl.cukrzycowy.ollee.glycemia.PreferencesBackupManager
import pl.cukrzycowy.ollee.glycemia.R
import pl.cukrzycowy.ollee.glycemia.StoragePermissionHelper
import java.io.File

@Composable
fun FirstRunImportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val latestBackup = remember { mutableStateOf<File?>(null) }
    val showRestoreDialog = remember { mutableStateOf(false) }
    val showPermissionDialog = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val hasAllFilesAccess = StoragePermissionHelper.hasAllFilesAccess()
        Log.d("FirstRunImport", "Has all files access: $hasAllFilesAccess")

        if (!hasAllFilesAccess) {
            Log.d("FirstRunImport", "Storage access needed to search for backups")
            showPermissionDialog.value = true
            return@LaunchedEffect
        }

        try {
            val backup = PreferencesBackupManager.findLatestBackup(context)
            if (backup != null) {
                Log.d("FirstRunImport", "Found backup: ${backup.absolutePath}")
                latestBackup.value = backup
                showRestoreDialog.value = true
            } else {
                Log.d("FirstRunImport", "No backup files found")
                onDismiss()
            }
        } catch (e: Exception) {
            Log.e("FirstRunImport", "Error finding backup: ${e.message}", e)
            onDismiss()
        }
    }

    if (showPermissionDialog.value) {
        StorageAccessDialog(
            onDismiss = {
                showPermissionDialog.value = false
                onDismiss()
            }
        )
    }

    if (showRestoreDialog.value && latestBackup.value != null) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(stringResource(R.string.first_run_restore_title)) },
            text = { Text(stringResource(R.string.first_run_restore_message, latestBackup.value!!.name)) },
            confirmButton = {
                val successMsg = stringResource(R.string.first_run_restore_success)
                val restoreButtonLabel = stringResource(R.string.first_run_restore_button)
                TextButton(
                    onClick = {
                        try {
                            PreferencesBackupManager.importPreferences(context, latestBackup.value!!)
                            Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                            Log.d("FirstRunImport", "Import successful: ${latestBackup.value!!.name}")
                        } catch (e: Exception) {
                            val failMsg = context.getString(R.string.first_run_restore_failed, e.message ?: "Unknown error")
                            Toast.makeText(context, failMsg, Toast.LENGTH_LONG).show()
                            Log.e("FirstRunImport", "Import failed: ${e.message}", e)
                        }
                        onDismiss()
                    }
                ) {
                    Text(restoreButtonLabel)
                }
            },
            dismissButton = {
                val skipButtonLabel = stringResource(R.string.first_run_skip_button)
                TextButton(onClick = { onDismiss() }) {
                    Text(skipButtonLabel)
                }
            }
        )
    }
}
