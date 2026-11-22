package es.pedrazamiguez.splittrip.features.balance.presentation.component.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SheetTitleText
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingBurnRateUiModel

@Suppress("LongMethod")
@Composable
internal fun SpendingBurnRateSection(
    burnRate: SpendingBurnRateUiModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.Small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeadingText(text = stringResource(R.string.balances_insights_burn_rate_title))
            PassportTag(
                text = burnRate.activeDaysLabel,
                style = PassportTagStyle.INFO
            )
        }

        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                SecondaryBodyText(text = stringResource(R.string.balances_insights_total_spent_label))
                SheetTitleText(text = burnRate.formattedTotalSpent)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                FlatCard(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.Default),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
                    ) {
                        CaptionText(text = stringResource(R.string.balances_insights_daily_average_label))
                        AmountText(text = burnRate.formattedDailyAverage)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                FlatCard(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.Default),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
                    ) {
                        CaptionText(text = stringResource(R.string.balances_insights_peak_day_label))
                        AmountText(text = burnRate.formattedPeakDayAmount.orEmpty())
                        burnRate.formattedPeakDayDate?.let { peakDate ->
                            SecondaryBodyText(text = peakDate)
                        }
                    }
                }
            }
        }

        burnRate.addOnsBreakdown?.let { addOnsBreakdown ->
            AddOnsSummaryTile(addOnsBreakdown = addOnsBreakdown)
        }

        val monthlyPaceLabel = burnRate.monthlyPaceLabel
        val formattedMonthlyPace = burnRate.formattedMonthlyPace
        if (monthlyPaceLabel != null && formattedMonthlyPace != null) {
            FlatCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.Default),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CardTitleText(
                        text = monthlyPaceLabel,
                        modifier = Modifier.weight(1f)
                    )
                    AmountText(
                        text = formattedMonthlyPace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
