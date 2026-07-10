package pl.cukrzycowy.ollee.glycemia.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import pl.cukrzycowy.ollee.glycemia.ui.screens.MainScreen
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
    val backStack = remember { mutableStateListOf(startDestination) }
    val nav = remember(backStack) { AppNavController(backStack) }

    BackHandler(enabled = backStack.size > 1) { nav.back() }

    when (val route = nav.current) {
        is Route.Main -> MainScreen(nav = nav)
        is Route.Settings -> SettingsScreen(onBack = { nav.back() })
        is Route.ProviderConfig -> ProviderConfigScreen(providerId = route.providerId, onBack = { nav.back() })
        is Route.WatchPairing -> WatchPairingScreen(onBack = { nav.back() })
    }
}
