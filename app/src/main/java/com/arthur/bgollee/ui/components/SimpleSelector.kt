package com.arthur.bgollee.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arthur.bgollee.ui.theme.OlleeColors
import com.arthur.bgollee.ui.theme.OlleeShapes
import com.arthur.bgollee.ui.theme.OlleeSpacing

@Composable
fun SimpleSelector(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (selected) OlleeColors.AccentPrimary else OlleeColors.SurfaceCardBorder,
                shape = OlleeShapes.Pill
            )
            .background(OlleeColors.Background, OlleeShapes.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = OlleeSpacing.lg, vertical = OlleeSpacing.md),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = OlleeColors.TextPrimary
        )
    }
}
