package pl.cukrzycowy.ollee.glycemia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val OlleeColorScheme = lightColorScheme(
    primary = OlleeColors.AccentPrimary,
    onPrimary = OlleeColors.OnAccentPrimary,
    secondary = OlleeColors.SecondaryDark,
    onSecondary = OlleeColors.OnSecondaryDark,
    background = OlleeColors.Background,
    onBackground = OlleeColors.TextPrimary,
    surface = OlleeColors.SurfaceCard,
    onSurface = OlleeColors.TextPrimary,
    surfaceVariant = OlleeColors.SurfaceDisabled,
    onSurfaceVariant = OlleeColors.TextSecondary,
    error = OlleeColors.StatusNegative,
    onError = OlleeColors.OnStatusColor,
    outline = OlleeColors.SurfaceCardBorder
)

/**
 * App-wide theme. Deliberately light-only (the reference "Ollee" UI has no
 * dark variant) and with no dynamic/system color - the brand accent is a
 * fixed token (see [OlleeColors.AccentPrimary]).
 */
@Composable
fun OlleeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OlleeColorScheme,
        typography = Typography,
        shapes = OlleeMaterialShapes,
        content = content
    )
}
