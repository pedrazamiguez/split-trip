package es.pedrazamiguez.splittrip.features.expense.presentation.component.spending

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategorySpendingUiModel

@Composable
fun CategorySpendingItemRow(
    item: CategorySpendingUiModel,
    modifier: Modifier = Modifier
) {
    val hasSubcategories = item.subcategories.isNotEmpty()
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        CategorySpendingHeaderRow(
            item = item,
            isExpanded = isExpanded,
            hasSubcategories = hasSubcategories,
            onToggle = { isExpanded = !isExpanded }
        )

        if (hasSubcategories) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.Small)
                ) {
                    item.subcategories.forEach { subcategory ->
                        SubcategorySpendingItemRow(
                            item = subcategory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MaterialTheme.spacing.Small)
                        )
                    }
                }
            }
        }
    }
}
