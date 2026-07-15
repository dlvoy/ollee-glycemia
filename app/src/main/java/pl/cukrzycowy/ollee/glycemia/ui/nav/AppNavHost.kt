package pl.cukrzycowy.ollee.glycemia.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import pl.cukrzycowy.ollee.glycemia.PreferencesBackupManager
import pl.cukrzycowy.ollee.glycemia.StoragePermissionHelper
import pl.cukrzycowy.ollee.glycemia.StoragePermissionStore
import pl.cukrzycowy.ollee.glycemia.WatchStore
import pl.cukrzycowy.ollee.glycemia.ui.components.FirstRunImportDialog
import pl.cukrzycowy.ollee.glycemia.ui.components.StorageAccessDialog
import pl.cukrzycowy.ollee.glycemia.ui.screens.MainScreen
import pl.cukrzycowy.ollee.glycemia.ui.screens.NightscoutProviderConfigScreen
import pl.cukrzycowy.ollee.glycemia.ui.screens.ProviderConfigScreen
import pl.cukrzycowy.ollee.glycemia.ui.screens.SettingsScreen
import pl.cukrzycowy.ollee.glycemia.ui.screens.WatchPairingScreen

/**
 * Minimal manual navigator: a back-stack of [Route]s. Pushing/popping is
 * exposed to screens via [AppNavController] so each screen doesn't need to
 * know about the stack itself.
 */
class AppNavController(private val backStack: MutableList<Route>) {
    val current: Route get() = backStack.last()

    fun navigate(route: Route) {
        backStack.add(route)
    }

    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }
}

@Composable
fun AppNavHost(startDestination: Route = Route.Main) {
    val context = LocalContext.current
    val backStack = remember { mutableStateListOf(startDestination) }
    val nav = remember(backStack) { AppNavController(backStack) }
    val showFirstRunImport = remember { mutableStateOf(false) }
    val showStorageAccessDialog = remember { mutableStateOf(false) }

    // Check if this is first run (no watches configured) and if backups might exist
    remember {
        val watches = WatchStore.getAll(context)
        if (watches.isEmpty()) {
            val hasAllFilesAccess = StoragePermissionHelper.hasAllFilesAccess()
            if (hasAllFilesAccess) {
                val hasBackup = PreferencesBackupManager.findLatestBackup(context) != null
                showFirstRunImport.value = hasBackup
            } else {
                val userDeclinedPrompt = StoragePermissionStore.hasUserDeclinedPrompt(context)
                if (!userDeclinedPrompt) {
                    showStorageAccessDialog.value = true
                }
            }
        }
        true
    }

    BackHandler(enabled = backStack.size > 1) { nav.back() }

    when (val route = nav.current) {
        is Route.Main -> MainScreen(nav = nav)
        is Route.Settings -> SettingsScreen(onBack = { nav.back() })
        is Route.ProviderConfig -> {
            // Route to specialized Nightscout screen or generic provider config screen
            if (route.providerId == "nightscout") {
                NightscoutProviderConfigScreen(onBack = { nav.back() })
            } else {
                ProviderConfigScreen(providerId = route.providerId, onBack = { nav.back() })
            }
        }
        is Route.WatchPairing -> WatchPairingScreen(onBack = { nav.back() })
    }

    if (showStorageAccessDialog.value) {
        StorageAccessDialog(
            onDismiss = {
                showStorageAccessDialog.value = false
                // Check for backup after storage access is granted
                val hasBackup = PreferencesBackupManager.findLatestBackup(context) != null
                showFirstRunImport.value = hasBackup
            }
        )
    }

    if (showFirstRunImport.value) {
        FirstRunImportDialog(onDismiss = { showFirstRunImport.value = false })
    }
}
