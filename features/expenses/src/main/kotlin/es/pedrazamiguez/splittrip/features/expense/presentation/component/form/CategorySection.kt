package es.pedrazamiguez.splittrip.features.expense.presentation.component.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.CategoryGridSelector
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.ShowAllCategoriesChip
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubcategoryUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Category selection using a fixed-position grid with optional subcategory selection.
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
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

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

            CategoryGridSelector(
                items = availableCategories,
                isItemSelected = { it.id == selectedCategory?.id },
                onItemClicked = { category ->
                    onCategorySelected(category.id)
                    focusManager.clearFocus()
                },
                itemId = { it.id },
                itemLabel = { it.displayText },
                itemIcon = { it.icon }
            )
        }

        SubcategorySelectionSection(
            selectedCategoryId = selectedCategory?.id,
            showAllCategories = showAllCategories,
            availableSubcategories = availableSubcategories,
            selectedSubcategory = selectedSubcategory,
            onSubcategorySelected = { subcategoryId ->
                onSubcategorySelected(subcategoryId)
                focusManager.clearFocus()
            }
        )
    }
}
