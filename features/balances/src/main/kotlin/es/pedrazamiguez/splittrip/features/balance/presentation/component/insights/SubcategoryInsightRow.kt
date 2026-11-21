package es.pedrazamiguez.splittrip.features.balance.presentation.component.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SubcategoryInsightUiModel

private const val SUBCATEGORY_ICON_BG_ALPHA = 0.10f
private val SUBCATEGORY_ICON_CONTAINER_SIZE = 28.dp
private val SUBCATEGORY_ICON_SIZE = 16.dp

@Composable
internal fun SubcategoryInsightRow(
    item: SubcategoryInsightUiModel,
    categoryColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = MaterialTheme.spacing.Default,
                top = MaterialTheme.spacing.ExtraSmall,
                bottom = MaterialTheme.spacing.ExtraSmall
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(SUBCATEGORY_ICON_CONTAINER_SIZE)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = SUBCATEGORY_ICON_BG_ALPHA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.subcategoryIcon,
                contentDescription = null,
                modifier = Modifier.size(SUBCATEGORY_ICON_SIZE),
                tint = categoryColor
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
        ) {
            SecondaryBodyText(
                text = item.subcategoryName,
                color = MaterialTheme.colorScheme.onSurface
            )
            CaptionText(text = item.expenseCountLabel)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AmountText(text = item.formattedAmount)
            PassportTag(
                text = item.formattedPercentage,
                style = PassportTagStyle.NEUTRAL
            )
        }
    }
}
