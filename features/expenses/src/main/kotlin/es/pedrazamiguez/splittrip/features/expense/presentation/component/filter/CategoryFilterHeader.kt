package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.ShowAllCategoriesChip

@Composable
internal fun CategoryFilterHeader(
    selectedCount: Int,
    showAllCategories: Boolean,
    onToggleShowAllCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterSectionHeader(
        title = stringResource(R.string.expenses_filter_section_category),
        selectedCount = selectedCount,
        modifier = modifier,
        trailingContent = {
            ShowAllCategoriesChip(showAllCategories, onToggleShowAllCategories)
        }
    )
}
