package es.pedrazamiguez.splittrip.features.group.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Clock
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.component.GroupExtraCurrencyChip
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupCurrencyRateUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongMethod", "CognitiveComplexMethod")
@Composable
fun GroupDetailInfoSection(
    group: GroupUiModel,
    modifier: Modifier = Modifier,
    currencyRates: ImmutableList<GroupCurrencyRateUiModel> = persistentListOf()
) {
    SectionCard(
        title = stringResource(R.string.group_detail_section_details),
        modifier = modifier
    ) {
        if (group.description.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)) {
                CaptionText(
                    text = stringResource(R.string.group_detail_description_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BodyText(
                    text = group.description,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryBodyText(
                text = stringResource(R.string.group_detail_context_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PassportTag(
                text = stringResource(group.context.toStringRes()),
                icon = group.context.toIconVector(),
                style = PassportTagStyle.NEUTRAL
            )
        }

        if (group.currency.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecondaryBodyText(
                    text = stringResource(R.string.group_detail_currency_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BodyText(
                        text = group.currency,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (currencyRates.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
                SecondaryBodyText(
                    text = stringResource(R.string.group_detail_extra_currencies_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                currencyRates.forEach { rateItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GroupExtraCurrencyChip(currency = rateItem.currency)
                        if (rateItem.formattedRate != null) {
                            CaptionText(
                                text = rateItem.formattedRate,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (group.dateText.isNotBlank() || group.lastUpdatedText.isNotBlank()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                if (group.dateText.isNotBlank()) {
                    PassportTag(
                        text = stringResource(R.string.group_detail_created_at, group.dateText),
                        icon = TablerIcons.Outline.Calendar,
                        style = PassportTagStyle.NEUTRAL
                    )
                }
                if (group.lastUpdatedText.isNotBlank()) {
                    PassportTag(
                        text = stringResource(R.string.group_detail_updated_at, group.lastUpdatedText),
                        icon = TablerIcons.Outline.Clock,
                        style = PassportTagStyle.NEUTRAL
                    )
                }
            }
        }
    }
}
