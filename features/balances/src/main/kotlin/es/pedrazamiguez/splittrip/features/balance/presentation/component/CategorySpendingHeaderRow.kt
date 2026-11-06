package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronDown
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronUp
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CategorySpendingUiModel

@Suppress("LongMethod")
@Composable
internal fun CategorySpendingHeaderRow(
    item: CategorySpendingUiModel,
    isExpanded: Boolean,
    hasSubcategories: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val rowModifier = if (hasSubcategories) {
        modifier
            .fillMaxWidth()
            .debouncedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            )
    } else {
        modifier.fillMaxWidth()
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(item.color, RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.Medium))

        Icon(
            imageVector = item.categoryIcon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.Medium))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.categoryName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.Small))

            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = item.formattedPercentage,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.Small,
                        vertical = 2.dp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.Small))

        Text(
            text = item.formattedAmount,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (hasSubcategories) {
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.Small))
            val chevronIcon = if (isExpanded) {
                TablerIcons.Outline.ChevronUp
            } else {
                TablerIcons.Outline.ChevronDown
            }
            val chevronContentDesc = stringResource(
                if (isExpanded) R.string.balances_category_collapse else R.string.balances_category_expand,
                item.categoryName
            )
            Icon(
                imageVector = chevronIcon,
                contentDescription = chevronContentDesc,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
