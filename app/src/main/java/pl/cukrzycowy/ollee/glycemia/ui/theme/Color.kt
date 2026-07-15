package pl.cukrzycowy.ollee.glycemia.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for brand/status colors. Every component should
 * reference these tokens instead of hardcoding hex values, so the brand
 * accent can be swapped (e.g. red -> orange) by editing this file only.
 */
object OlleeColors {
    // Default brand accent: red, seeded from the app logo (#FF0000).
    val AccentPrimary = Color(0xFFE5342A)
    val OnAccentPrimary = Color(0xFFFFFFFF)

    // Reserved alternative accent (Ollee ecosystem's stock orange), not
    // wired into the UI today - kept only to document the swap target.
    val AccentPrimaryAlt = Color(0xFFF5821F)

    val SecondaryDark = Color(0xFF1F2733)
    val OnSecondaryDark = Color(0xFFFFFFFF)

    val StatusPositive = Color(0xFF43A047)
    val StatusNegative = Color(0xFFC2255C)
    val StatusWarning = Color(0xFFFFC107)
    val OnStatusColor = Color(0xFFFFFFFF)

    val HelpText = Color(0xFF1976D2)  // Material blue

    val Background = Color(0xFFFFFFFF)
    val SurfaceCard = Color(0xFFF1F1F3)
    val SurfaceCardBorder = Color(0xFFE0E0E0)
    val SurfaceDisabled = Color(0xFFE4E4E6)

    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF6B6B6B)
    val TextDisabled = Color(0xFFACACAC)
    val Divider = Color(0xFFE0E0E0)
}
