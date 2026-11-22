package es.pedrazamiguez.splittrip.features.balance.presentation.component.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SubcategoryInsightUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun CategorySubcategorySection(
    subcategories: ImmutableList<SubcategoryInsightUiModel>,
    categoryColor: Color,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    if (subcategories.isEmpty()) return

    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.ExtraSmall),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            subcategories.forEach { subcategory ->
                SubcategoryInsightRow(
                    item = subcategory,
                    categoryColor = categoryColor
                )
            }
        }
    }
}
