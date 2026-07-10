package pl.cukrzycowy.ollee.glycemia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeColors
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeSpacing

/**
 * Full-screen sub-page scaffold with a back button + title, used by
 * Settings, Provider config, and Watch pairing (spec 2.1/2.2/3 - all
 * described as "full screen / with back button").
 */
@Composable
fun FullScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Scaffold(containerColor = OlleeColors.Background) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OlleeSpacing.sm, vertical = OlleeSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Column(
                modifier = Modifier.padding(horizontal = OlleeSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(OlleeSpacing.lg),
                content = content
            )
        }
    }
}
