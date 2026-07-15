package pl.cukrzycowy.ollee.glycemia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.cukrzycowy.ollee.glycemia.ConfigurableGlycemiaProvider
import pl.cukrzycowy.ollee.glycemia.R
import pl.cukrzycowy.ollee.glycemia.GlycemiaProviderManager
import pl.cukrzycowy.ollee.glycemia.ProviderConfigField
import pl.cukrzycowy.ollee.glycemia.ui.components.FullScreenScaffold
import pl.cukrzycowy.ollee.glycemia.ui.components.PillButton
import pl.cukrzycowy.ollee.glycemia.ui.components.PillButtonStyle
import pl.cukrzycowy.ollee.glycemia.ui.components.SectionLabel
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeColors
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeSpacing

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
    var fieldValues by remember {
        mutableStateOf(mutableMapOf<String, String>().apply {
            spec.fields.forEach { field ->
                this[field.key] = savedValues[field.key] ?: field.defaultValue
            }
        })
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
                            onValueChange = { 
                                fieldValues = fieldValues.toMutableMap().apply {
                                    this[field.key] = it
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(field.label) }
                        )
                        field.helperText?.let {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.fillMaxWidth().padding(top = OlleeSpacing.xs),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(OlleeSpacing.xs),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = OlleeColors.HelpText
                                )
                                Text(
                                    it,
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    color = OlleeColors.HelpText
                                )
                            }
                        }
                    }
                }
            }

            PillButton(
                text = stringResource(R.string.provider_config_save),
                icon = Icons.Default.Save,
                style = PillButtonStyle.PRIMARY,
                onClick = {
                    provider.saveConfig(context, fieldValues)
                    onBack()
                }
            )
        }
    }
}
