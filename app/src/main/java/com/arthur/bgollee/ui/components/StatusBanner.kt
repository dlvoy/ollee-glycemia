package com.arthur.bgollee.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.arthur.bgollee.ui.theme.OlleeColors
import com.arthur.bgollee.ui.theme.OlleeSpacing

enum class StatusBannerTone { POSITIVE, NEGATIVE, WARNING, NEUTRAL }

/**
 * Full-width colored bar under the header, e.g. "Connecting... 23" (green)
 * or "Timed out. Check if Bluetooth is active on watch!" (crimson).
 */
@Composable
fun StatusBanner(
    text: String,
    tone: StatusBannerTone,
    modifier: Modifier = Modifier
) {
    val visible = text.isNotBlank()
    AnimatedVisibility(visible = visible) {
        val background = when (tone) {
            StatusBannerTone.POSITIVE -> OlleeColors.StatusPositive
            StatusBannerTone.NEGATIVE -> OlleeColors.StatusNegative
            StatusBannerTone.WARNING -> OlleeColors.StatusWarning
            StatusBannerTone.NEUTRAL -> OlleeColors.SurfaceDisabled
        }
        val contentColor: Color = if (tone == StatusBannerTone.NEUTRAL) OlleeColors.TextPrimary else OlleeColors.OnStatusColor

        Text(
            text = text,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .background(background)
                .padding(vertical = OlleeSpacing.sm, horizontal = OlleeSpacing.lg)
        )
    }
}
