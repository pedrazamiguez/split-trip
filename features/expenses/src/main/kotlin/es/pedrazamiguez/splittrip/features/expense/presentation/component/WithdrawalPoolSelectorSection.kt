package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.WithdrawalPoolOptionUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Displays a pool-selection widget when multiple cash withdrawal pools are available for the
 * current expense's currency and scope.
 *
 * Shown in the Amount step (same-currency CASH) or Exchange Rate step (foreign-currency CASH),
 * above the "Funded from" tranche breakdown, so the user can pick which pool to draw from.
 *
 * Layout:
 * - Section title ("Draw cash from") styled like other wizard step headers.
 * - [FlowRow] of [PassportChip]s — one per available pool, consistent with the payment step.
 *
 * @param pools         Non-empty list of available pool options (must have at least 2 entries,
 *                      caller is responsible for guarding on size > 1).
 * @param selectedPool  Currently selected pool. GROUP pool is pre-selected by default; the user
 *                      can tap another chip to override.
 * @param onPoolSelected Callback invoked with the selected pool's [PayerType] and scope owner ID
 *                       (userId for USER scope, subunitId for SUBUNIT scope, null for GROUP scope)
 *                       when the user taps a chip.
 * @param modifier      Modifier applied to the root [Column].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WithdrawalPoolSelectorSection(
    pools: ImmutableList<WithdrawalPoolOptionUiModel>,
    selectedPool: WithdrawalPoolOptionUiModel?,
    onPoolSelected: (scope: PayerType, scopeOwnerId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeadingText(text = stringResource(R.string.add_expense_cash_pool_selection_title))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            pools.forEach { pool ->
                val isSelected = pool == selectedPool
                PassportChip(
                    label = pool.displayLabel,
                    selected = isSelected,
                    onClick = { onPoolSelected(pool.scope, pool.ownerId) },
                    leadingIcon = if (isSelected) {
                        { Icon(TablerIcons.Outline.Check, contentDescription = null) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}
