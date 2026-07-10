package pl.cukrzycowy.ollee.glycemia.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeColors
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeShapes
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeSpacing

enum class PillButtonStyle { PRIMARY, SUCCESS, SECONDARY_DARK, DISABLED }

/**
 * Full-width, fully-rounded call-to-action button matching the reference
 * UI's "Pair Ollee Watch" / "Send to watch" / "Save Current Profile" pills.
 */
@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: PillButtonStyle = PillButtonStyle.PRIMARY,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val (background, content) = when {
        !enabled || style == PillButtonStyle.DISABLED ->
            OlleeColors.SurfaceDisabled to OlleeColors.TextDisabled
        style == PillButtonStyle.SUCCESS -> OlleeColors.StatusPositive to OlleeColors.OnStatusColor
        style == PillButtonStyle.SECONDARY_DARK -> OlleeColors.SecondaryDark to OlleeColors.OnSecondaryDark
        else -> OlleeColors.AccentPrimary to OlleeColors.OnAccentPrimary
    }

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = OlleeShapes.Pill,
        border = null,
        contentPadding = PaddingValues(vertical = OlleeSpacing.md),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = background,
            contentColor = content,
            disabledContainerColor = OlleeColors.SurfaceDisabled,
            disabledContentColor = OlleeColors.TextDisabled
        )
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(Modifier.width(OlleeSpacing.sm))
        }
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}
