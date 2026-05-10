package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardSectionLabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CashTranchePreviewUiModel

/**
 * Displays the "Funded from" ATM withdrawal tranche breakdown.
 *
 * The section title ("Funded from") is rendered **outside** the [FlatCard] to match
 * the visual language of "Currency conversion" and other wizard step headers.
 *
 * - Single tranche: shows one row directly.
 * - Multiple tranches: shows a collapsed count row that expands on tap to reveal all tranches.
 *
 * A disclaimer is always shown below the card as this is a simulated preview —
 * actual tranches are confirmed at save time.
 *
 * @param tranches  Non-empty list of tranche UI models in FIFO order.
 * @param modifier  Modifier applied to the root [Column].
 */
@Composable
fun CashTrancheFundedFromSection(
    tranches: List<CashTranchePreviewUiModel>,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val isMultiTranche = tranches.size > 1

    Column(modifier = modifier.fillMaxWidth()) {
        // Section title — same style as "Currency conversion" (titleSmall + Bold + onSurfaceVariant)
        CardSectionLabelText(text = stringResource(R.string.add_expense_cash_tranche_funded_from))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.Medium)) {
                if (isMultiTranche) {
                    CashTrancheHeader(
                        trancheCount = tranches.size,
                        isExpanded = isExpanded,
                        onToggle = { isExpanded = !isExpanded }
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))
                }

                CashTrancheList(
                    tranches = if (isMultiTranche && !isExpanded) listOf(tranches.first()) else tranches
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))
                SecondaryBodyText(
                    text = stringResource(R.string.add_expense_cash_tranche_disclaimer),
                    maxLines = Int.MAX_VALUE
                )
            }
        }
    }
}

/** Clickable header row shown when there are multiple tranches. */
@Composable
private fun CashTrancheHeader(
    trancheCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expandedLabel = stringResource(R.string.add_expense_cash_tranche_collapse)
    val collapsedLabel = pluralStringResource(R.plurals.add_expense_cash_tranche_count, trancheCount, trancheCount)
    val stateDesc = if (isExpanded) expandedLabel else collapsedLabel
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                stateDescription = stateDesc
            }
            .clickable(
                onClick = onToggle,
                onClickLabel = if (isExpanded) {
                    expandedLabel
                } else {
                    collapsedLabel
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (isExpanded) expandedLabel else collapsedLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Renders a vertical list of [CashTrancheRow]s with spacing between them. */
@Composable
private fun CashTrancheList(
    tranches: List<CashTranchePreviewUiModel>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        tranches.forEachIndexed { index, tranche ->
            CashTrancheRow(tranche = tranche)
            if (index < tranches.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
internal fun CashTrancheRow(
    tranche: CashTranchePreviewUiModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = tranche.withdrawalLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tranche.formattedAmountConsumed,
                style = MaterialTheme.typography.bodySmall
            )
            SecondaryBodyText(
                text = tranche.formattedRate.let { stringResource(R.string.add_expense_cash_tranche_rate_label, it) },
                maxLines = Int.MAX_VALUE
            )
        }
        SecondaryBodyText(
            text = stringResource(R.string.add_expense_cash_tranche_remaining, tranche.formattedRemainingAfter),
            maxLines = Int.MAX_VALUE
        )
    }
}
