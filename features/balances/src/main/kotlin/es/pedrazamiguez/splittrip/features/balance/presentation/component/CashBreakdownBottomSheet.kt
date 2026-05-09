package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashBreakdownUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Bottom sheet displaying a per-withdrawal cash attribution breakdown for a single member.
 *
 * Items are grouped by scope via section headers: GROUP-scoped withdrawals are shown first
 * (with an "estimated share" disclaimer), followed by USER-scoped personal cash, and then
 * SUBUNIT-scoped entries. The scope header re-renders whenever the [CashBreakdownUiModel.scopeLabel]
 * changes from the previous item, producing implicit grouping without a nested data structure.
 *
 * This is intentionally separate from [MemberBalanceItem] to keep that file under the 600-line limit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBreakdownBottomSheet(
    breakdown: ImmutableList<CashBreakdownUiModel>,
    formattedTotal: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.balances_cash_breakdown_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (breakdown.isEmpty()) {
                Text(
                    text = stringResource(R.string.balances_cash_breakdown_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CashBreakdownItemList(breakdown = breakdown)

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.balances_cash_breakdown_total_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedTotal,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CashBreakdownItemList(breakdown: ImmutableList<CashBreakdownUiModel>) {
    var lastScopeLabel = ""
    breakdown.forEach { item ->
        // Render a section header whenever the scope group changes
        if (item.scopeLabel != lastScopeLabel) {
            if (lastScopeLabel.isNotEmpty()) Spacer(Modifier.height(16.dp))
            Text(
                text = item.scopeLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            if (item.isEstimatedShare) {
                Text(
                    text = stringResource(R.string.balances_cash_breakdown_estimated_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            lastScopeLabel = item.scopeLabel
        }
        CashBreakdownEntryRow(item = item)
    }
}

@Composable
private fun CashBreakdownEntryRow(item: CashBreakdownUiModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Title row: withdrawal label · date · rate
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.withdrawalLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.dateText.isNotBlank()) {
                Text(
                    text = "·  ${item.dateText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.formattedRate.isNotBlank()) {
                Text(
                    text = "·  ${item.formattedRate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Amount row: native remaining [≈ equivalent]
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        ) {
            Text(
                text = item.formattedNativeRemaining,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.formattedEquivalent.isNotBlank()) {
                Text(
                    text = "≈  ${item.formattedEquivalent}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
