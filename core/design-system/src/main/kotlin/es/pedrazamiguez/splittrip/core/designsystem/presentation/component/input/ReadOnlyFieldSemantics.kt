package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

/**
 * Builds the accessibility content description for a read-only clickable field overlay
 * (e.g., dropdown or picker trigger) by combining its [label] with its current [value]
 * (or [placeholder] fallback when [value] is blank).
 *
 * Examples:
 * - `label = "Currency"`, `value = "EUR (€)"` -> `"Currency, EUR (€)"`
 * - `label = "Currency"`, `value = ""`, `placeholder = null` -> `"Currency"`
 * - `label = "Due date"`, `value = ""`, `placeholder = "Select date"` -> `"Due date, Select date"`
 * - `label = null`, `value = "EUR (€)"` -> `"EUR (€)"`
 * - `label = null`, `value = ""`, `placeholder = null` -> `null`
 */
internal fun buildReadOnlyFieldContentDescription(
    label: String?,
    value: String,
    placeholder: String? = null
): String? {
    val effectiveLabel = label?.trim()?.takeIf { it.isNotEmpty() }
    val effectiveValue = value.trim().ifEmpty { placeholder?.trim().orEmpty() }
    return when {
        effectiveLabel != null && effectiveValue.isNotEmpty() -> "$effectiveLabel, $effectiveValue"
        effectiveLabel != null -> effectiveLabel
        effectiveValue.isNotEmpty() -> effectiveValue
        else -> null
    }
}
