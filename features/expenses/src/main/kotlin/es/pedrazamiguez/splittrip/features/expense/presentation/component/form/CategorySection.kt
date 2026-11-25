package es.pedrazamiguez.splittrip.features.expense.presentation.component.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.CategoryCarousel
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.CategoryCarouselItem
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.ShowAllCategoriesChip
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubcategoryUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Category selection using a horizontal category carousel and contextual subcategory stamp tray.
 */
@Composable
internal fun CategorySection(
    availableCategories: ImmutableList<CategoryUiModel>,
    selectedCategory: CategoryUiModel?,
    onCategorySelected: (String) -> Unit,
    availableSubcategories: ImmutableList<SubcategoryUiModel>,
    selectedSubcategory: SubcategoryUiModel?,
    onSubcategorySelected: (String?) -> Unit,
    showAllCategories: Boolean = false,
    onToggleShowAllCategories: () -> Unit = {},
    carouselEdgePadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val carouselItems = remember(availableCategories, selectedCategory) {
        availableCategories.map { category ->
            val isSelected = category.id == selectedCategory?.id
            CategoryCarouselItem(
                id = category.id,
                label = category.displayText,
                icon = category.icon,
                isSelected = isSelected,
                isFocused = isSelected,
                activeSubcategoryCount = 0
            )
        }.toImmutableList()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeadingText(text = stringResource(R.string.add_expense_category_title))
                ShowAllCategoriesChip(
                    showAllCategories = showAllCategories,
                    onToggleShowAllCategories = onToggleShowAllCategories
                )
            }

            CategoryCarousel(
                items = carouselItems,
                focusedItemId = selectedCategory?.id,
                onItemClicked = { categoryId ->
                    onCategorySelected(categoryId)
                    focusManager.clearFocus()
                },
                edgePadding = carouselEdgePadding,
                selectionRole = Role.RadioButton
            )
        }

        SubcategorySelectionSection(
            selectedCategoryId = selectedCategory?.id,
            selectedCategoryLabel = selectedCategory?.displayText,
            availableSubcategories = availableSubcategories,
            selectedSubcategory = selectedSubcategory,
            onSubcategorySelected = { subcategoryId ->
                onSubcategorySelected(subcategoryId)
                focusManager.clearFocus()
            }
        )
    }
}
