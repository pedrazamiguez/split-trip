package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.ShareAllocationInputRow
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel

@Suppress("LongParameterList")
@Composable
internal fun IntraSubunitMemberRow(
    member: SplitUiModel,
    isEqualMode: Boolean,
    isPercentMode: Boolean,
    currencySymbol: String?,
    onAmountChanged: (String) -> Unit,
    onPercentageChanged: (String) -> Unit,
    onShareLockToggled: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            Text(
                text = member.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (member.formattedAmount.isNotBlank()) {
                Text(
                    text = member.formattedAmount,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!isEqualMode) {
            val inputConfig = resolveSplitInputConfig(
                split = member,
                isPercentMode = isPercentMode,
                currencySymbol = currencySymbol,
                onAmountChanged = onAmountChanged,
                onPercentageChanged = onPercentageChanged
            )
            ShareAllocationInputRow(
                value = inputConfig.value,
                onValueChange = inputConfig.onValueChange,
                unitText = inputConfig.unitRes?.let { stringResource(it) } ?: inputConfig.unitLiteral,
                isLocked = member.isShareLocked,
                lockContentDescription = stringResource(splitLockDescriptionRes(member.isShareLocked)),
                onLockToggle = onShareLockToggled,
                fieldContentDescription = stringResource(inputConfig.fieldDescriptionRes),
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { onDone() })
            )
        }
    }
}
