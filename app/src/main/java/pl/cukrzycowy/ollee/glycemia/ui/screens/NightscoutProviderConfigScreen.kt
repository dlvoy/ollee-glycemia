package pl.cukrzycowy.ollee.glycemia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.cukrzycowy.ollee.glycemia.NightscoutConfigValidationResult
import pl.cukrzycowy.ollee.glycemia.NightscoutFetchStateStore
import pl.cukrzycowy.ollee.glycemia.NightscoutFetchStatus
import pl.cukrzycowy.ollee.glycemia.NightscoutHttpClient
import pl.cukrzycowy.ollee.glycemia.NightscoutParseResult
import pl.cukrzycowy.ollee.glycemia.NightscoutProvider
import pl.cukrzycowy.ollee.glycemia.NightscoutUrlNormalizer
import pl.cukrzycowy.ollee.glycemia.R
import pl.cukrzycowy.ollee.glycemia.ui.components.FullScreenScaffold
import pl.cukrzycowy.ollee.glycemia.ui.components.PillButton
import pl.cukrzycowy.ollee.glycemia.ui.components.PillButtonStyle
import pl.cukrzycowy.ollee.glycemia.ui.components.SectionLabel
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeColors
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeSpacing

@Composable
fun NightscoutProviderConfigScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val provider = remember { NightscoutProvider() }
    val spec = remember { provider.getConfigSpec(context) }
    val savedValues = remember { provider.getSavedConfig(context).toMutableMap() }
    
    var fieldValues by remember {
        mutableStateOf(mutableMapOf<String, String>().apply {
            spec.fields.forEach { field ->
                this[field.key] = savedValues[field.key] ?: field.defaultValue
            }
        })
    }

    var testingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testError by remember { mutableStateOf<String?>(null) }
    var urlFieldFocused by remember { mutableStateOf(false) }
    var tokenFieldFocused by remember { mutableStateOf(false) }
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
                onValueChange = { 
                    fieldValues = fieldValues.toMutableMap().apply {
                        this["base_url"] = it
                    }
                },
                modifier = Modifier.fillMaxWidth().onFocusChanged { urlFieldFocused = it.isFocused },
                placeholder = { Text("https://user.ns.example.com") }
            )
            if (urlFieldFocused) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = OlleeSpacing.xs, start = OlleeSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = OlleeColors.HelpText
                    )
                    Text(
                        text = stringResource(R.string.provider_nightscout_url_helper),
                        style = MaterialTheme.typography.labelSmall,
                        color = OlleeColors.HelpText
                    )
                }
            }

            // URL validation feedback
            val baseUrl = fieldValues["base_url"] ?: ""
            if (baseUrl.isNotEmpty()) {
                when (val result = NightscoutUrlNormalizer.normalize(baseUrl)) {
                    is NightscoutConfigValidationResult.Valid -> {
                        if (result.normalized.warning != null) {
                            Text(
                                text = result.normalized.warning,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = OlleeSpacing.sm)
                            )
                        }
                    }
                    is NightscoutConfigValidationResult.Invalid -> {
                        Text(
                            text = "⚠ ${translateValidationError(context, result.reason)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = OlleeSpacing.sm)
                        )
                    }
                }
            }

            // Token field
            SectionLabel(text = stringResource(R.string.provider_nightscout_token_label))
            OutlinedTextField(
                value = fieldValues["token"] ?: "",
                onValueChange = { 
                    fieldValues = fieldValues.toMutableMap().apply {
                        this["token"] = it
                    }
                },
                modifier = Modifier.fillMaxWidth().onFocusChanged { tokenFieldFocused = it.isFocused },
                placeholder = { Text("ollee-xxxxxxxxxxxxxxxx") }
            )
            if (tokenFieldFocused) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = OlleeSpacing.xs, start = OlleeSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = OlleeColors.HelpText
                    )
                    Text(
                        text = stringResource(R.string.provider_nightscout_token_helper),
                        style = MaterialTheme.typography.labelSmall,
                        color = OlleeColors.HelpText
                    )
                }
            }

            // Token validation warning
            val token = fieldValues["token"] ?: ""
            val tokenWarning = if (token.isNotEmpty()) {
                NightscoutUrlNormalizer.validateTokenFormat(token)
            } else {
                null
            }
            if (tokenWarning != null) {
                Text(
                    text = tokenWarning,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Test connection button
            PillButton(
                text = if (testingConnection)
                    stringResource(R.string.provider_nightscout_testing)
                else
                    stringResource(R.string.provider_nightscout_test_connection),
                icon = Icons.Default.Check,
                style = PillButtonStyle.PRIMARY,
                enabled = !testingConnection && (fieldValues["base_url"]?.isNotBlank() == true),
                onClick = {
                    testingConnection = true
                    testResult = null
                    testError = null

                    coroutineScope.launch {
                        try {
                            val baseUrl = fieldValues["base_url"] ?: ""
                            var token = fieldValues["token"] ?: ""

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

                            // Update URL and extract token if present in URL
                            fieldValues = fieldValues.toMutableMap().apply {
                                this["base_url"] = normalizedConfig.baseUrl
                                if (normalizedConfig.extractedToken != null) {
                                    this["token"] = normalizedConfig.extractedToken
                                    token = normalizedConfig.extractedToken
                                }
                            }

                            // Create HTTP client with normalized URL and token
                            val httpClient = NightscoutHttpClient(normalizedConfig.baseUrl, token)

                            // Test connection via status endpoint (on IO thread)
                            val statusResult = withContext(Dispatchers.IO) {
                                httpClient.status()
                            }
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
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(OlleeSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.provider_nightscout_testing), style = MaterialTheme.typography.bodySmall)
                }
            }

            testResult?.let { result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                    DiagnosticRow("Status", getStatusString(context, lastFetchState.status))
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
                icon = Icons.Default.Save,
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

private fun getStatusString(context: android.content.Context, status: NightscoutFetchStatus): String {
    return when (status) {
        NightscoutFetchStatus.NEVER_RUN -> context.getString(R.string.provider_nightscout_fetch_status_never_run)
        NightscoutFetchStatus.OK -> context.getString(R.string.provider_nightscout_fetch_status_ok)
        NightscoutFetchStatus.INVALID_CONFIG -> context.getString(R.string.provider_nightscout_fetch_status_invalid_config)
        NightscoutFetchStatus.AUTH_ERROR -> context.getString(R.string.provider_nightscout_fetch_status_auth_error)
        NightscoutFetchStatus.CONNECTION_ERROR -> context.getString(R.string.provider_nightscout_fetch_status_connection_error)
        NightscoutFetchStatus.NO_DATA -> context.getString(R.string.provider_nightscout_fetch_status_no_data)
        NightscoutFetchStatus.INVALID_RESPONSE -> context.getString(R.string.provider_nightscout_fetch_status_invalid_response)
        NightscoutFetchStatus.DELAYED_DATA -> context.getString(R.string.provider_nightscout_fetch_status_delayed_data)
        NightscoutFetchStatus.SERVER_ERROR -> context.getString(R.string.provider_nightscout_fetch_status_server_error)
    }
}

private fun translateValidationError(context: android.content.Context, errorMessage: String): String {
    return when {
        errorMessage.contains("URL cannot be empty") -> context.getString(R.string.provider_nightscout_error_url_empty)
        errorMessage.contains("Malformed URL") -> context.getString(R.string.provider_nightscout_error_malformed_url)
        errorMessage.contains("Invalid scheme") -> context.getString(R.string.provider_nightscout_error_invalid_scheme)
        errorMessage.contains("URL must contain a host") -> context.getString(R.string.provider_nightscout_error_no_host)
        else -> errorMessage
    }
}
