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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnDetailUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun AddOnsSummarySection(
    addOns: ImmutableList<AddOnDetailUiModel>,
    formattedEffectiveTotal: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionIcon(TablerIcons.Outline.Receipt)
            LabelText(text = stringResource(R.string.expense_detail_section_addons))
        }
        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                addOns.forEach { addOn -> AddOnRow(addOn) }
                if (formattedEffectiveTotal != null) {
                    DetailRow(
                        label = stringResource(R.string.expense_review_effective_total),
                        value = formattedEffectiveTotal
                    )
                }
            }
        }
    }
}

@Composable
private fun AddOnRow(addOn: AddOnDetailUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            BodyText(text = addOn.labelText)
            CaptionText(
                text = addOn.modeText.uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        BodyText(
            text = if (addOn.isDiscount) "- ${addOn.formattedAmount}" else addOn.formattedAmount,
            color = if (addOn.isDiscount) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
