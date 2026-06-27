package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.User
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ExtraItemUiModel

@Composable
internal fun ExtrasBreakdownEntryRow(
    item: ExtraItemUiModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.ExtraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
        ) {
            Icon(
                imageVector = when (item.scopeType) {
                    PayerType.GROUP -> TablerIcons.Outline.UsersGroup
                    PayerType.SUBUNIT -> TablerIcons.Outline.Sitemap
                    PayerType.USER -> TablerIcons.Outline.User
                },
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.parentTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = item.formattedAmount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = MaterialTheme.spacing.Small)
        )
    }
}
