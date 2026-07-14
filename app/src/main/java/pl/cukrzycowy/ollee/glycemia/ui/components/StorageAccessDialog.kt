package pl.cukrzycowy.ollee.glycemia.ui.components

import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import pl.cukrzycowy.ollee.glycemia.StoragePermissionHelper
import pl.cukrzycowy.ollee.glycemia.StoragePermissionStore
import pl.cukrzycowy.ollee.glycemia.R

@Composable
fun StorageAccessDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    // Monitor permission status and auto-dismiss when granted
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            if (StoragePermissionHelper.hasAllFilesAccess()) {
                Log.d("StorageAccessDialog", "Storage access granted, dismissing dialog")
                // Clear the declined flag when permission is granted
                StoragePermissionStore.setUserDeclinedPrompt(context, false)
                onDismiss()
                break
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            // Mark that user declined the prompt
            StoragePermissionStore.setUserDeclinedPrompt(context, true)
            onDismiss()
        },
        title = { Text(context.getString(R.string.backup_storage_access_title)) },
        text = { Text(context.getString(R.string.backup_storage_access_description)) },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        context.startActivity(StoragePermissionHelper.createAllFilesAccessSettingsIntent(context))
                    } catch (e: Exception) {
                        Log.e("StorageAccessDialog", "Failed to open storage settings: ${e.message}", e)
                        onDismiss()
                    }
                }
            ) {
                Text(context.getString(R.string.backup_storage_access_grant))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    // Mark that user declined the prompt
                    StoragePermissionStore.setUserDeclinedPrompt(context, true)
                    onDismiss()
                }
            ) {
                Text(context.getString(android.R.string.cancel))
            }
        }
    )
}
