package pl.cukrzycowy.ollee.glycemia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeSpacing
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeTheme

/**
 * Design-time showcase of every Phase 0 component, for visual review in
 * Android Studio's preview pane. Not part of the app's runtime UI.
 */
@Preview(showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun ComponentShowcasePreview() {
    var expanded by remember { mutableStateOf(true) }
    var toggled by remember { mutableStateOf(true) }

    OlleeTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(OlleeSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(OlleeSpacing.lg)
            ) {
                OlleeHeader(permissionsOk = true, onSettingsClick = {})
                OlleeHeader(permissionsOk = false, onSettingsClick = {})

                StatusBanner(text = "Connecting... 23", tone = StatusBannerTone.POSITIVE)
                StatusBanner(text = "Timed out. Check if Bluetooth is active on watch!", tone = StatusBannerTone.NEGATIVE)

                SectionLabel(text = "Watches")

                RichSelectorRow(
                    title = "Ollee Watch #1",
                    subtitle = "Synced  ·  00:80:E1:26:8C:D3",
                    leadingIcon = Icons.Filled.Watch,
                    highlighted = true,
                    trailingActions = {
                        RowActionIcon(icon = Icons.Filled.Edit, contentDescription = "Rename", onClick = {})
                        RowActionIcon(icon = Icons.Filled.Delete, contentDescription = "Remove", onClick = {})
                    }
                )
                RichSelectorRow(
                    title = "Ollee Watch #2",
                    subtitle = "Offline  ·  00:80:E1:26:BD:2D",
                    leadingIcon = Icons.Filled.Watch,
                    trailingActions = {
                        RowActionIcon(icon = Icons.Filled.Edit, contentDescription = "Rename", onClick = {})
                        RowActionIcon(icon = Icons.Filled.Delete, contentDescription = "Remove", onClick = {})
                    }
                )

                PillButton(text = "Pair Ollee Watch", style = PillButtonStyle.SUCCESS, onClick = {})
                PillButton(text = "Send to watch", style = PillButtonStyle.PRIMARY, onClick = {})
                PillButton(text = "Save Current Profile", style = PillButtonStyle.SECONDARY_DARK, onClick = {})
                PillButton(text = "Load Profile", style = PillButtonStyle.DISABLED, enabled = false, onClick = {})

                ToggleRow(label = "Hourly Chime on", checked = toggled, onCheckedChange = { toggled = it })

                FoldableSection(title = "Charts", expanded = expanded, onToggle = { expanded = it }) {
                    Surface(modifier = Modifier.fillMaxWidth().padding(top = OlleeSpacing.sm)) {
                        androidx.compose.material3.Text(text = "Graph card placeholder", modifier = Modifier.padding(OlleeSpacing.md))
                    }
                }
            }
        }
    }
}
