package pl.cukrzycowy.ollee.glycemia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import kotlinx.coroutines.launch
import pl.cukrzycowy.ollee.glycemia.NightscoutConfigValidationResult
import pl.cukrzycowy.ollee.glycemia.NightscoutFetchStateStore
import pl.cukrzycowy.ollee.glycemia.NightscoutHttpClient
import pl.cukrzycowy.ollee.glycemia.NightscoutParseResult
import pl.cukrzycowy.ollee.glycemia.NightscoutProvider
import pl.cukrzycowy.ollee.glycemia.NightscoutUrlNormalizer
import pl.cukrzycowy.ollee.glycemia.R
import pl.cukrzycowy.ollee.glycemia.ui.components.FullScreenScaffold
import pl.cukrzycowy.ollee.glycemia.ui.components.PillButton
import pl.cukrzycowy.ollee.glycemia.ui.components.PillButtonStyle
import pl.cukrzycowy.ollee.glycemia.ui.components.SectionLabel
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeSpacing

@Composable
fun NightscoutProviderConfigScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val provider = remember { NightscoutProvider() }
    val spec = remember { provider.getConfigSpec(context) }
    val savedValues = remember { provider.getSavedConfig(context).toMutableMap() }
    
    val fieldValues = remember {
        mutableMapOf<String, String>().apply {
            spec.fields.forEach { field ->
                this[field.key] = savedValues[field.key] ?: field.defaultValue
            }
        }
    }

    var testingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val lastFetchState = remember { NightscoutFetchStateStore.read(context) }

    FullScreenScaffold(title = spec.title, onBack = onBack) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = OlleeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OlleeSpacing.lg)
        ) {
            // URL field
            SectionLabel(text = stringResource(R.string.provider_nightscout_url_label))
            OutlinedTextField(
                value = fieldValues["base_url"] ?: "",
                onValueChange = { fieldValues["base_url"] = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://example-nightscout.org") }
            )
            Text(
                text = stringResource(R.string.provider_nightscout_url_helper),
                style = MaterialTheme.typography.labelSmall
            )

            // Token field
            SectionLabel(text = stringResource(R.string.provider_nightscout_token_label))
            OutlinedTextField(
                value = fieldValues["token"] ?: "",
                onValueChange = { fieldValues["token"] = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("sync-xxxxxxxxxxxxxxxx") }
            )
            Text(
                text = stringResource(R.string.provider_nightscout_token_helper),
                style = MaterialTheme.typography.labelSmall
            )

            // Test connection button
            PillButton(
                text = if (testingConnection)
                    stringResource(R.string.provider_nightscout_testing)
                else
                    stringResource(R.string.provider_nightscout_test_connection),
                style = PillButtonStyle.PRIMARY,
                enabled = !testingConnection && (fieldValues["base_url"]?.isNotBlank() == true),
                onClick = {
                    testingConnection = true
                    testResult = null
                    testError = null

                    coroutineScope.launch {
                        try {
                            val baseUrl = fieldValues["base_url"] ?: ""
                            val token = fieldValues["token"] ?: ""

                            // Normalize URL
                            val normalizedResult = NightscoutUrlNormalizer.normalize(baseUrl)
                            val normalizedConfig = when (normalizedResult) {
                                is NightscoutConfigValidationResult.Valid -> normalizedResult.normalized
                                is NightscoutConfigValidationResult.Invalid -> {
                                    testError = normalizedResult.reason
                                    testingConnection = false
                                    return@launch
                                }
                            }

                            // Create HTTP client
                            val httpClient = NightscoutHttpClient(normalizedConfig.baseUrl, token)

                            // Test connection via status endpoint
                            val statusResult = httpClient.status()
                            when (statusResult) {
                                is NightscoutParseResult.Success -> {
                                    val status = statusResult.value
                                    testResult = context.getString(
                                        R.string.provider_nightscout_status_ok,
                                        status.version
                                    )
                                }
                                is NightscoutParseResult.Failure -> {
                                    testError = statusResult.reason
                                }
                            }
                        } catch (e: Exception) {
                            testError = context.getString(
                                R.string.provider_nightscout_connection_error,
                                e.message ?: "Unknown error"
                            )
                        } finally {
                            testingConnection = false
                        }
                    }
                }
            )

            // Test results
            if (testingConnection) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(OlleeSpacing.md)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(OlleeSpacing.md),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = OlleeSpacing.md))
                    Text(stringResource(R.string.provider_nightscout_testing))
                }
            }

            testResult?.let { result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(OlleeSpacing.md)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(OlleeSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.md)
                ) {
                    Text("✓", style = MaterialTheme.typography.labelLarge)
                    Text(result, style = MaterialTheme.typography.bodySmall)
                }
            }

            testError?.let { error ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(OlleeSpacing.md)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(OlleeSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.md)
                ) {
                    Text("✗", style = MaterialTheme.typography.labelLarge)
                    Text(error, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Last fetch diagnostics
            if (lastFetchState != null) {
                SectionLabel(text = stringResource(R.string.provider_nightscout_last_fetch_title))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(OlleeSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)
                ) {
                    DiagnosticRow("Status", lastFetchState.status.name)
                    lastFetchState.detail?.let {
                        DiagnosticRow("Detail", it)
                    }
                    lastFetchState.bg?.let {
                        DiagnosticRow("BG", it)
                    }
                    lastFetchState.httpCode?.let {
                        DiagnosticRow("HTTP Code", it.toString())
                    }
                }
            }

            // Save button
            PillButton(
                text = stringResource(R.string.provider_config_save),
                style = PillButtonStyle.PRIMARY,
                onClick = {
                    // Normalize and save URL
                    val baseUrl = fieldValues["base_url"] ?: ""
                    val normalizedResult = NightscoutUrlNormalizer.normalize(baseUrl)
                    
                    if (normalizedResult is NightscoutConfigValidationResult.Valid) {
                        fieldValues["base_url"] = normalizedResult.normalized.baseUrl
                    }

                    provider.saveConfig(context, fieldValues)
                    onBack()
                }
            )
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontStyle = FontStyle.Italic
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            maxLines = 2
        )
    }
}
