package pl.cukrzycowy.ollee.glycemia

data class ProviderConfigOption(
    val value: String,
    val label: String
)

data class ProviderConfigField(
    val key: String,
    val label: String,
    val type: FieldType,
    val defaultValue: String,
    val options: List<ProviderConfigOption> = emptyList(),
    val optional: Boolean = false,
    val helperText: String? = null
) {
    enum class FieldType {
        INTEGER,
        LONG,
        TEXT,
        CHOICE
    }
}

data class ProviderConfigSpec(
    val title: String,
    val fields: List<ProviderConfigField>
)