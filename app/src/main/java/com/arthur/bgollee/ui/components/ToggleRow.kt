package com.arthur.bgollee.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arthur.bgollee.ui.theme.OlleeColors
import com.arthur.bgollee.ui.theme.OlleeShapes
import com.arthur.bgollee.ui.theme.OlleeSpacing

/** A labeled row with a trailing toggle switch, styled per the reference UI. */
@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(OlleeColors.SurfaceCard, OlleeShapes.Card)
            .padding(horizontal = OlleeSpacing.lg, vertical = OlleeSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = OlleeColors.TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OlleeColors.AccentPrimary,
                checkedTrackColor = OlleeColors.Background,
                checkedBorderColor = OlleeColors.AccentPrimary
            )
        )
    }
}
