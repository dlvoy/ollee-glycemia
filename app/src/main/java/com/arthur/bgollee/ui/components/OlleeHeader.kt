package com.arthur.bgollee.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arthur.bgollee.R
import com.arthur.bgollee.ui.theme.OlleeColors
import com.arthur.bgollee.ui.theme.OlleeSpacing

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
            .padding(horizontal = OlleeSpacing.lg, vertical = OlleeSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .border(1.5.dp, OlleeColors.AccentPrimary, RoundedCornerShape(12.dp))
                .padding(horizontal = OlleeSpacing.md, vertical = OlleeSpacing.sm)
        ) {
            Image(
                painter = painterResource(R.drawable.olleexdrip),
                contentDescription = "Ollee",
                modifier = Modifier
                    .height(28.dp)
                    .width(52.dp)
            )
        }

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
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(14.dp)
                            .background(OlleeColors.StatusNegative, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "!", color = OlleeColors.OnStatusColor, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}
