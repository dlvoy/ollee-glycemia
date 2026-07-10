package pl.cukrzycowy.ollee.glycemia.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.cukrzycowy.ollee.glycemia.R
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeColors
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeSpacing

/**
 * Persistent app header: logo (in a rounded, accent-outlined frame) on the
 * left, a single status-aware action icon on the right. The icon is a plain
 * gear when Bluetooth permissions are granted, or a red "Bluetooth disabled"
 * glyph with an exclamation badge when they are not (spec 2.1).
 */
@Composable
fun OlleeHeader(
    permissionsOk: Boolean,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = OlleeSpacing.lg, vertical = OlleeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.md)
    ) {
        Image(
            painter = painterResource(R.drawable.olleexdrip),
            contentDescription = "Ollee",
            modifier = Modifier
                .height(36.dp)
                .width(64.dp)
        )

        Text(
            text = "Ollee Glycemia",
            fontSize = 18.sp,
            color = OlleeColors.TextPrimary,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        IconButton(onClick = onSettingsClick) {
            if (permissionsOk) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = OlleeColors.TextPrimary
                )
            } else {
                Box {
                    Icon(
                        imageVector = Icons.Filled.BluetoothDisabled,
                        contentDescription = "Settings - permissions required",
                        tint = OlleeColors.StatusNegative
                    )
                    Text(
                        text = "!",
                        color = OlleeColors.StatusNegative,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}
