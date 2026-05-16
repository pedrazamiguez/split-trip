package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnDetailUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Progressive-disclosure breakdown of the derivation chain. Only ever rendered
 * when the expense actually has add-ons or carries an INCLUDED-decomposed base
 * cost — vanilla expenses skip this section entirely (issue #1077 Tier 2).
 */
@Composable
internal fun BreakdownCardSection(
    addOns: ImmutableList<AddOnDetailUiModel>,
    formattedEffectiveTotal: String?,
    formattedIncludedBaseCost: String?,
    formattedOriginalEnteredTotal: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionIcon(TablerIcons.Outline.Receipt)
            LabelText(text = stringResource(R.string.expense_detail_section_breakdown))
        }
        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                if (formattedIncludedBaseCost != null) {
                    BaseCostRow(formattedIncludedBaseCost)
                }
                addOns.forEach { addOn -> AddOnRow(addOn) }
                val summaryLabelRes = when {
                    formattedOriginalEnteredTotal != null -> R.string.expense_detail_breakdown_original_total
                    else -> R.string.expense_review_effective_total
                }
                val summaryValue = formattedOriginalEnteredTotal ?: formattedEffectiveTotal
                if (summaryValue != null) {
                    SummaryRow(label = stringResource(summaryLabelRes), value = summaryValue)
                }
            }
        }
    }
}

@Composable
private fun BaseCostRow(formattedBaseCost: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SecondaryBodyText(
            text = stringResource(R.string.expense_detail_breakdown_base_cost),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AmountText(text = formattedBaseCost)
    }
}

@Composable
private fun AddOnRow(addOn: AddOnDetailUiModel) {
    // Discount tone deliberately uses `tertiary` from the theme palette — Horizon
    // Narrative §3.2 keeps the success/positive accent under the secondary scale.
    val valueColor = if (addOn.isDiscount) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val signedAmount = if (addOn.isDiscount) "− ${addOn.formattedAmount}" else addOn.formattedAmount
    val modeLabelRes = if (addOn.isIncluded) {
        R.string.expense_detail_addon_mode_included
    } else {
        R.string.expense_detail_addon_mode_on_top
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SecondaryBodyText(text = addOn.labelText)
            CaptionText(
                text = stringResource(modeLabelRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            AmountText(text = signedAmount, color = valueColor)
            if (addOn.isForeignCurrency && addOn.formattedSourceAmount != null) {
                CaptionText(
                    text = addOn.formattedSourceAmount,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (addOn.formattedRate != null) {
                CaptionText(
                    text = addOn.formattedRate,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SecondaryBodyText(
            text = label,
            color = MaterialTheme.colorScheme.onSurface
        )
        AmountText(
            text = value,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
