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
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SheetTitleText
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashBreakdownUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Bottom sheet displaying a per-withdrawal cash attribution breakdown for a single member.
 *
 * Items are grouped by scope via section headers: GROUP-scoped withdrawals are shown first
 * (with an "estimated share" disclaimer), followed by SUBUNIT-scoped entries, then
 * USER-scoped personal cash. The scope header re-renders whenever the [CashBreakdownUiModel.scopeLabel]
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
                .padding(
                    start = MaterialTheme.spacing.Large,
                    end = MaterialTheme.spacing.Large,
                    top = MaterialTheme.spacing.ExtraLarge,
                    bottom = MaterialTheme.spacing.Section
                )
        ) {
            SheetTitleText(
                text = stringResource(R.string.balances_cash_breakdown_title),
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.Default)
            )

            if (breakdown.isEmpty()) {
                BodyText(
                    text = stringResource(R.string.balances_cash_breakdown_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CashBreakdownItemList(breakdown = breakdown)

                Spacer(Modifier.height(MaterialTheme.spacing.Large))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelText(
                        text = stringResource(R.string.balances_cash_breakdown_total_label),
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
            if (lastScopeLabel.isNotEmpty()) Spacer(Modifier.height(MaterialTheme.spacing.Default))
            LabelText(
                text = item.scopeLabel,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.ExtraSmall)
            )
            if (item.isEstimatedShare) {
                CaptionText(
                    text = stringResource(R.string.balances_cash_breakdown_estimated_hint),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.ExtraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left column: label stacked above date, then rate on its own line for easy scanning
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.withdrawalLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.dateText.isNotBlank()) {
                CaptionText(
                    text = item.dateText,
                    maxLines = Int.MAX_VALUE
                )
            }
            if (item.formattedRate.isNotBlank()) {
                CaptionText(
                    text = item.formattedRate,
                    maxLines = Int.MAX_VALUE
                )
            }
        }
        // Right column: native amount on top, group-currency equivalent below — visually secondary
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(start = MaterialTheme.spacing.Small)
        ) {
            Text(
                text = item.formattedNativeRemaining,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.formattedEquivalent.isNotBlank()) {
                CaptionText(
                    text = item.formattedEquivalent,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
