package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
internal fun SplitMemberRow(
    split: SplitUiModel,
    isEqualMode: Boolean,
    isPercentMode: Boolean,
    onAmountChanged: (String) -> Unit,
    onPercentageChanged: (String) -> Unit,
    onExcludedToggled: () -> Unit,
    onShareLockToggled: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        // Name column (previously SplitMemberNameColumn)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = split.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (split.isExcluded) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (!split.isExcluded && !isEqualMode && split.formattedAmount.isNotBlank()) {
                SecondaryBodyText(
                    text = split.formattedAmount,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = Int.MAX_VALUE
                )
            }
        }

        // Input field (previously SplitMemberInputField)
        AnimatedVisibility(visible = !split.isExcluded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                if (isEqualMode) {
                    Text(
                        text = split.formattedAmount,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isPercentMode) {
                    StyledOutlinedTextField(
                        value = split.percentageInput,
                        onValueChange = onPercentageChanged,
                        label = stringResource(R.string.add_expense_split_percentage_label),
                        modifier = Modifier.widthIn(max = 100.dp),
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { onDone() })
                    )
                    ShareLockIcon(isLocked = split.isShareLocked, onClick = onShareLockToggled)
                } else {
                    StyledOutlinedTextField(
                        value = split.amountInput,
                        onValueChange = onAmountChanged,
                        label = stringResource(R.string.add_expense_split_amount_label),
                        modifier = Modifier.widthIn(max = 120.dp),
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { onDone() })
                    )
                    ShareLockIcon(isLocked = split.isShareLocked, onClick = onShareLockToggled)
                }
            }
        }

        Switch(
            checked = !split.isExcluded,
            onCheckedChange = { onExcludedToggled() }
        )
    }
}
