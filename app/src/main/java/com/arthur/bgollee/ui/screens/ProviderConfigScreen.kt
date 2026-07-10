package com.arthur.bgollee.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.arthur.bgollee.ConfigurableGlycemiaProvider
import com.arthur.bgollee.R
import com.arthur.bgollee.GlycemiaProviderManager
import com.arthur.bgollee.ProviderConfigField
import com.arthur.bgollee.ui.components.FullScreenScaffold
import com.arthur.bgollee.ui.components.PillButton
import com.arthur.bgollee.ui.components.PillButtonStyle
import com.arthur.bgollee.ui.components.SectionLabel
import com.arthur.bgollee.ui.theme.OlleeSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderConfigScreen(providerId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val provider = GlycemiaProviderManager.allProviders.find { it.id == providerId }

    if (provider !is ConfigurableGlycemiaProvider) {
        FullScreenScaffold(title = stringResource(R.string.provider_config_label), onBack = onBack) {
            Text(stringResource(R.string.provider_not_configurable))
        }
        return
    }

    val spec = remember { provider.getConfigSpec(context) }
    val savedValues = remember { provider.getSavedConfig(context).toMutableMap() }
    val fieldValues = remember { mutableMapOf<String, String>() }
    spec.fields.forEach { field ->
        fieldValues[field.key] = savedValues[field.key] ?: field.defaultValue
    }

    FullScreenScaffold(title = spec.title, onBack = onBack) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = OlleeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OlleeSpacing.lg)
        ) {
            spec.fields.forEach { field ->
                SectionLabel(text = field.label)

                when (field.type) {
                    ProviderConfigField.FieldType.CHOICE -> {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = field.options.find { it.value == fieldValues[field.key] }?.label ?: "",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                field.options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            fieldValues[field.key] = option.value
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    ProviderConfigField.FieldType.INTEGER,
                    ProviderConfigField.FieldType.LONG,
                    ProviderConfigField.FieldType.TEXT -> {
                        OutlinedTextField(
                            value = fieldValues[field.key] ?: "",
                            onValueChange = { fieldValues[field.key] = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(field.label) }
                        )
                        field.helperText?.let {
                            Text(it, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            PillButton(
                text = stringResource(R.string.provider_config_save),
                style = PillButtonStyle.PRIMARY,
                onClick = {
                    provider.saveConfig(context, fieldValues)
                    onBack()
                }
            )
        }
    }
}
