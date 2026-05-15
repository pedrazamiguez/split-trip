package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Level 2 intra-subunit split editor, shown inside the subunit accordion.
 *
 * Displays a per-subunit split type selector followed by member rows.
 * Each member row shows the member's share based on the subunit's split type.
 *
 * @param members The nested member splits within this subunit.
 * @param entitySplitType The current Level 2 split type for this subunit (may differ from Level 1).
 * @param availableSplitTypes The pre-built list of available split types (passed from state to avoid recomposition allocations).
 * @param onSplitTypeChanged Callback when the per-subunit split type changes.
 * @param onAmountChanged Callback when a member's EXACT amount changes.
 * @param onPercentageChanged Callback when a member's PERCENT percentage changes.
 * @param onShareLockToggled Callback when a member's share lock is toggled.
 */
@Composable
fun IntraSubunitSplitEditor(
    members: ImmutableList<SplitUiModel>,
    entitySplitType: SplitTypeUiModel?,
    availableSplitTypes: ImmutableList<SplitTypeUiModel>,
    onSplitTypeChanged: (String) -> Unit,
    onAmountChanged: (userId: String, amount: String) -> Unit,
    onPercentageChanged: (userId: String, percentage: String) -> Unit,
    onShareLockToggled: (userId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val isEqualMode = entitySplitType?.id == SplitType.EQUAL.name
    val isPercentMode = entitySplitType?.id == SplitType.PERCENT.name

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        // ── Level 2 split type label ──────────────────────────────
        Text(
            text = stringResource(R.string.add_expense_split_subunit_distribution),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ── Level 2 split type selector (reuses SplitTypeSelector) ─
        SplitTypeSelector(
            splitTypes = availableSplitTypes,
            selectedSplitType = entitySplitType,
            onSplitTypeSelected = onSplitTypeChanged
        )

        // ── Member rows ───────────────────────────────────────────
        members.forEach { member ->
            IntraSubunitMemberRow(
                member = member,
                isEqualMode = isEqualMode,
                isPercentMode = isPercentMode,
                onAmountChanged = { amount -> onAmountChanged(member.userId, amount) },
                onPercentageChanged = { pct -> onPercentageChanged(member.userId, pct) },
                onShareLockToggled = { onShareLockToggled(member.userId) },
                onDone = { focusManager.clearFocus() }
            )
        }
    }
}

/**
 * A single member row within the intra-subunit editor.
 * Similar to [SplitMemberRow] but without the exclude toggle (subunit members cannot be
 * individually excluded — the entire subunit is excluded at entity level).
 */
@Suppress("LongMethod") // Compose UI builder DSL
@Composable
private fun IntraSubunitMemberRow(
    member: SplitUiModel,
    isEqualMode: Boolean,
    isPercentMode: Boolean,
    onAmountChanged: (String) -> Unit,
    onPercentageChanged: (String) -> Unit,
    onShareLockToggled: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        // Member name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Show currency amount as secondary text for EXACT and PERCENT modes
            if (!isEqualMode && member.formattedAmount.isNotBlank()) {
                Text(
                    text = member.formattedAmount,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Amount / percentage display or input
        if (isEqualMode) {
            Text(
                text = member.formattedAmount,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (isPercentMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                StyledOutlinedTextField(
                    value = member.percentageInput,
                    onValueChange = onPercentageChanged,
                    label = stringResource(R.string.add_expense_split_percentage_label),
                    modifier = Modifier.widthIn(max = 90.dp),
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(onNext = { onDone() })
                )
                ShareLockIcon(isLocked = member.isShareLocked, onClick = onShareLockToggled)
            }
        } else {
            // EXACT mode
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                StyledOutlinedTextField(
                    value = member.amountInput,
                    onValueChange = onAmountChanged,
                    label = stringResource(R.string.add_expense_split_amount_label),
                    modifier = Modifier.widthIn(max = 110.dp),
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(onNext = { onDone() })
                )
                ShareLockIcon(isLocked = member.isShareLocked, onClick = onShareLockToggled)
            }
        }
    }
}
