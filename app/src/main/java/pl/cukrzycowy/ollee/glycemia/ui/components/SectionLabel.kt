package pl.cukrzycowy.ollee.glycemia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeColors
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeTextStyles

/** Bold, all-caps section header (e.g. "GLYCEMIA SOURCES", "WATCHES"). */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = OlleeTextStyles.SectionLabel,
            color = OlleeColors.TextPrimary
        )
        trailingContent()
    }
}
