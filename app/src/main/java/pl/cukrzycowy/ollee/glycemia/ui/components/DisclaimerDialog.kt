package pl.cukrzycowy.ollee.glycemia.ui.components

import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import org.commonmark.node.StrongEmphasis
import pl.cukrzycowy.ollee.glycemia.DisclaimerStore
import pl.cukrzycowy.ollee.glycemia.R

@Composable
fun DisclaimerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isChecked = remember { mutableStateOf(false) }
    val markdownText = stringResource(R.string.disclaimer_body)
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    
    val markwon = remember(primaryColor) {
        Markwon.builder(context)
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                    builder.setFactory(StrongEmphasis::class.java) { _, _ ->
                        arrayOf<Any>(
                            StyleSpan(Typeface.BOLD),
                            ForegroundColorSpan(primaryColor)
                        )
                    }
                }
            })
            .build()
    }

    AlertDialog(
        onDismissRequest = {
            // Don't allow dismissing without accepting
        },
        modifier = Modifier.fillMaxWidth(0.95f),
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Custom title with underline (matching help_popup.png style)
                Text(
                    text = stringResource(R.string.disclaimer_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )

                // Markdown body text
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 16.dp, bottom = 16.dp),
                    factory = { ctx ->
                        TextView(ctx).apply {
                            textSize = 13f // Smaller font
                        }
                    },
                    update = { textView ->
                        markwon.setMarkdown(textView, markdownText)
                    }
                )

                // Checkbox with label - label clickable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { isChecked.value = !isChecked.value },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked.value,
                        onCheckedChange = { isChecked.value = it },
                        modifier = Modifier.clickable(enabled = false) {
                            // This is handled by the Row clickable
                        }
                    )
                    Text(
                        text = stringResource(R.string.disclaimer_checkbox),
                        modifier = Modifier.padding(start = 8.dp),
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    DisclaimerStore.setUserAcceptedDisclaimer(context, true)
                    onDismiss()
                },
                enabled = isChecked.value,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.disclaimer_button))
            }
        }
    )
}
