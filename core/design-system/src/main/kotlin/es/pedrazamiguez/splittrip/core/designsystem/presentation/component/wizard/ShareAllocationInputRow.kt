package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField

/**
 * Shared single-line allocation input row pairing a full-width soft [StyledOutlinedTextField]
 * (with an always-visible inline unit [unitText] such as `"%"` or `"€"` and no external header)
 * with [ShareLockButton].
 *
 * Used across `:features:expenses` (`SplitMemberRow`, `EntitySplitRow`, `IntraSubunitSplitEditor`)
 * and `:features:subunits` (`SubunitSharesStep`) so all share-allocation inputs align cleanly on
 * both standard and narrow screens.
 */
@Suppress("LongParameterList")
@Composable
fun ShareAllocationInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    unitText: String?,
    isLocked: Boolean,
    lockContentDescription: String,
    onLockToggle: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
    fieldContentDescription: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        val fieldModifier = Modifier
            .weight(1f)
            .then(
                if (fieldContentDescription != null) {
                    Modifier.semantics { contentDescription = fieldContentDescription }
                } else {
                    Modifier
                }
            )
        StyledOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = null,
            placeholder = placeholder,
            trailingIcon = unitText?.takeIf { it.isNotBlank() }?.let { unit ->
                {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            modifier = fieldModifier,
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction,
            keyboardActions = keyboardActions
        )
        ShareLockButton(
            isLocked = isLocked,
            contentDescription = lockContentDescription,
            onClick = onLockToggle
        )
    }
}
