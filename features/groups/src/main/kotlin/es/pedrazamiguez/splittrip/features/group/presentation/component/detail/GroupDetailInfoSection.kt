package es.pedrazamiguez.splittrip.features.group.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel

@Suppress("LongMethod", "CognitiveComplexMethod")
@Composable
fun GroupDetailInfoSection(
    group: GroupUiModel,
    modifier: Modifier = Modifier
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

        if (group.dateText.isNotBlank() || group.lastUpdatedText.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)) {
                if (group.dateText.isNotBlank()) {
                    CaptionText(
                        text = stringResource(R.string.group_detail_created_at, group.dateText),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (group.lastUpdatedText.isNotBlank()) {
                    CaptionText(
                        text = stringResource(R.string.group_detail_updated_at, group.lastUpdatedText),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
