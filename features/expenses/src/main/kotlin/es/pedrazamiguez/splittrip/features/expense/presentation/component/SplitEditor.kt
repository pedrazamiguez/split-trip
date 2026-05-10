package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.LockFilled
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.LockOpen
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Displays per-user split entries.
 *
 * - EQUAL mode: shows read-only calculated amounts with currency symbol.
 * - EXACT mode: editable amount input + currency display. Remainder auto-distributes.
 * - PERCENT mode: editable percentage input + currency display. Remainder auto-distributes.
 */
@Suppress("LongParameterList") // Compose UI — params are inherent to the split editor surface
@Composable
fun SplitEditor(
    splits: ImmutableList<SplitUiModel>,
    isEqualMode: Boolean,
    isPercentMode: Boolean,
    onAmountChanged: (userId: String, amount: String) -> Unit,
    onPercentageChanged: (userId: String, percentage: String) -> Unit,
    onExcludedToggled: (userId: String) -> Unit,
    onShareLockToggled: (userId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        splits.forEach { split ->
            SplitMemberRow(
                split = split,
                isEqualMode = isEqualMode,
                isPercentMode = isPercentMode,
                onAmountChanged = { amount -> onAmountChanged(split.userId, amount) },
                onPercentageChanged = { pct -> onPercentageChanged(split.userId, pct) },
                onExcludedToggled = { onExcludedToggled(split.userId) },
                onShareLockToggled = { onShareLockToggled(split.userId) },
                onDone = { focusManager.clearFocus() }
            )
        }
    }
}

@Suppress("LongParameterList", "LongMethod") // Compose UI builder DSL
@Composable
private fun SplitMemberRow(
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
        SplitMemberNameColumn(split = split, isEqualMode = isEqualMode, modifier = Modifier.weight(1f))
        SplitMemberInputField(
            split = split,
            isEqualMode = isEqualMode,
            isPercentMode = isPercentMode,
            onAmountChanged = onAmountChanged,
            onPercentageChanged = onPercentageChanged,
            onShareLockToggled = onShareLockToggled,
            onDone = onDone
        )
        Switch(
            checked = !split.isExcluded,
            onCheckedChange = { onExcludedToggled() }
        )
    }
}

@Composable
private fun SplitMemberNameColumn(
    split: SplitUiModel,
    isEqualMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
}

@Composable
private fun SplitMemberInputField(
    split: SplitUiModel,
    isEqualMode: Boolean,
    isPercentMode: Boolean,
    onAmountChanged: (String) -> Unit,
    onPercentageChanged: (String) -> Unit,
    onShareLockToggled: () -> Unit,
    onDone: () -> Unit
) {
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
}

/**
 * Padlock icon toggle indicating whether a share value is locked (user-set)
 * and should be preserved during redistribution.
 */
@Composable
internal fun ShareLockIcon(
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(32.dp)
    ) {
        Icon(
            imageVector = if (isLocked) TablerIcons.Filled.LockFilled else TablerIcons.Outline.LockOpen,
            contentDescription = stringResource(
                if (isLocked) {
                    R.string.add_expense_split_share_unlock
                } else {
                    R.string.add_expense_split_share_lock
                }
            ),
            tint = if (isLocked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            modifier = Modifier.size(18.dp)
        )
    }
}
