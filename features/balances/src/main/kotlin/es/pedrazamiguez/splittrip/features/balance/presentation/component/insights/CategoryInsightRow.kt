package es.pedrazamiguez.splittrip.features.balance.presentation.component.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart.AnimatedProgressBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CategoryInsightUiModel

private const val CATEGORY_ICON_BG_ALPHA = 0.14f
private val CATEGORY_ICON_CONTAINER_SIZE = 36.dp
private val CATEGORY_ICON_SIZE = 18.dp
private val CATEGORY_BAR_HEIGHT = 8.dp

@Composable
internal fun CategoryInsightRow(
    item: CategoryInsightUiModel,
    animationKey: Any,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(CATEGORY_ICON_CONTAINER_SIZE)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = CATEGORY_ICON_BG_ALPHA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.categoryIcon,
                    contentDescription = null,
                    modifier = Modifier.size(CATEGORY_ICON_SIZE),
                    tint = item.color
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                CardTitleText(text = item.categoryName)
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

        AnimatedProgressBar(
            progress = item.progress,
            fillColor = item.color,
            barHeight = CATEGORY_BAR_HEIGHT,
            animationKey = animationKey
        )
    }
}
