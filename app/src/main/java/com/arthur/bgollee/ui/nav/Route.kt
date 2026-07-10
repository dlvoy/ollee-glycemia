package com.arthur.bgollee.ui.nav

/** App navigation routes. Kept as a small manual back-stack (see [AppNavHost])
 *  rather than pulling in the Navigation-Compose library, since the app has
 *  only a handful of screens and no deep-linking needs. */
sealed class Route {
    data object Main : Route()
    data object Settings : Route()
    data class ProviderConfig(val providerId: String) : Route()
    data object WatchPairing : Route()
}
