package pl.cukrzycowy.ollee.glycemia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeColors
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeShapes
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeSpacing
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeTextStyles

/**
 * The reusable "rich two-line row" pattern from the reference UI: a leading
 * icon, a bold primary line, a grey secondary line, and trailing action
 * icon(s). Used for the provider selector, watch list rows, and the watch
 * pairing list.
 */
@Composable
fun RichSelectorRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    highlighted: Boolean = false,
    subtitleColor: Color = OlleeColors.TextSecondary,
    onClick: (() -> Unit)? = null,
    trailingActions: @Composable RowScope.() -> Unit = {}
) {
    val titleColor = if (highlighted) OlleeColors.AccentPrimary else OlleeColors.TextPrimary

    var rowModifier = modifier
        .fillMaxWidth()
        .background(OlleeColors.SurfaceCard, OlleeShapes.Pill)
    if (highlighted) {
        rowModifier = rowModifier.border(1.5.dp, OlleeColors.AccentPrimary, OlleeShapes.Pill)
    }
    if (onClick != null) {
        rowModifier = rowModifier.clickable(onClick = onClick)
    }

    Row(
        modifier = rowModifier.padding(horizontal = OlleeSpacing.lg, vertical = OlleeSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.md)
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(OlleeColors.Background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = leadingIcon, contentDescription = null, tint = titleColor)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = titleColor)
            Text(text = subtitle, style = OlleeTextStyles.Caption, color = subtitleColor)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            content = trailingActions
        )
    }
}

/** Small circular icon-only action button used inside [RichSelectorRow] trailing slots. */
@Composable
fun RowActionIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = LocalContentColor.current
) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}
