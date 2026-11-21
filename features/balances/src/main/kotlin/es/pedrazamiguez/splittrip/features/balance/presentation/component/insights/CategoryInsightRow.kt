package es.pedrazamiguez.splittrip.features.balance.presentation.component.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart.AnimatedProgressBar
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CategoryInsightUiModel

private val CATEGORY_BAR_HEIGHT = 8.dp

@Composable
internal fun CategoryInsightRow(
    item: CategoryInsightUiModel,
    animationKey: Any,
    modifier: Modifier = Modifier
) {
    val hasSubcategories = item.subcategories.isNotEmpty()
    var isExpanded by rememberSaveable(item.category.name) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        CategoryInsightHeaderRow(
            item = item,
            hasSubcategories = hasSubcategories,
            isExpanded = isExpanded,
            onToggle = { isExpanded = !isExpanded }
        )

        AnimatedProgressBar(
            progress = item.progress,
            fillColor = item.color,
            barHeight = CATEGORY_BAR_HEIGHT,
            animationKey = animationKey
        )

        CategorySubcategorySection(
            subcategories = item.subcategories,
            categoryColor = item.color,
            isExpanded = isExpanded
        )
    }
}
