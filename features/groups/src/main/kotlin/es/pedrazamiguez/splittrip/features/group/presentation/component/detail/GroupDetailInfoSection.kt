package es.pedrazamiguez.splittrip.features.group.presentation.component.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Clock
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.component.GroupExtraCurrencyChip
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupCurrencyRateUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val METADATA_PILL_HORIZONTAL_PADDING = 10.dp
private val METADATA_PILL_VERTICAL_PADDING = 4.dp
private val METADATA_ICON_SIZE = 14.dp

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
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(
                                horizontal = METADATA_PILL_HORIZONTAL_PADDING,
                                vertical = METADATA_PILL_VERTICAL_PADDING
                            ),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = TablerIcons.Outline.Calendar,
                            contentDescription = null,
                            modifier = Modifier.size(METADATA_ICON_SIZE),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.group_detail_created_at, group.dateText),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (group.lastUpdatedText.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(
                                horizontal = METADATA_PILL_HORIZONTAL_PADDING,
                                vertical = METADATA_PILL_VERTICAL_PADDING
                            ),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = TablerIcons.Outline.Clock,
                            contentDescription = null,
                            modifier = Modifier.size(METADATA_ICON_SIZE),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.group_detail_updated_at, group.lastUpdatedText),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
