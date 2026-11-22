package es.pedrazamiguez.splittrip.features.balance.presentation.component.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.AddOnBreakdownUiModel

private val TYPE_ICON_SIZE = 14.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddOnsSummaryTile(
    addOnsBreakdown: AddOnBreakdownUiModel,
    modifier: Modifier = Modifier
) {
    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CaptionText(text = stringResource(R.string.balances_insights_add_ons_title))
                addOnsBreakdown.formattedPercentageOfSpending?.let { shareLabel ->
                    PassportTag(
                        text = shareLabel,
                        style = PassportTagStyle.NEUTRAL
                    )
                }
            }

            AmountText(text = addOnsBreakdown.formattedTotalExtras)

            if (addOnsBreakdown.typeBreakdown.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
                ) {
                    addOnsBreakdown.typeBreakdown.forEach { typeItem ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = typeItem.icon,
                                contentDescription = null,
                                modifier = Modifier.size(TYPE_ICON_SIZE),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SecondaryBodyText(text = "${typeItem.label}: ${typeItem.formattedAmount}")
                        }
                    }
                }
            }

            addOnsBreakdown.formattedWithdrawalFeesNote?.let { withdrawalNote ->
                CaptionText(text = withdrawalNote)
            }

            addOnsBreakdown.formattedDiscountsSavedNote?.let { discountsNote ->
                CaptionText(text = discountsNote)
            }
        }
    }
}
