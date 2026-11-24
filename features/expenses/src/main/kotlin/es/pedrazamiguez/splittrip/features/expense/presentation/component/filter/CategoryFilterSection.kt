package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardSectionLabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.CategoryGridSelector
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.ShowAllCategoriesChip
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun CategoryFilterSection(
    criteria: ExpenseFilterCriteria,
    onCriteriaChange: (ExpenseFilterCriteria) -> Unit,
    modifier: Modifier = Modifier,
    groupContext: GroupContext = GroupContext.TRAVEL,
    showAllCategories: Boolean = false,
    onToggleShowAllCategories: () -> Unit = {}
) {
    val visibleCategories = remember(groupContext, showAllCategories, criteria.selectedCategories) {
        resolveVisibleCategories(groupContext, showAllCategories, criteria.selectedCategories)
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
                CardSectionLabelText(text = stringResource(R.string.expenses_filter_section_category))
                ShowAllCategoriesChip(
                    showAllCategories = showAllCategories,
                    onToggleShowAllCategories = onToggleShowAllCategories
                )
            }

            CategoryGridSelector(
                items = visibleCategories,
                isItemSelected = { it in criteria.selectedCategories },
                onItemClicked = { category -> onCriteriaChange(toggleCategory(criteria, category)) },
                itemId = { it.name },
                itemLabel = { stringResource(it.toStringRes()) },
                itemIcon = { it.toIconVector() }
            )
        }

        visibleCategories.forEach { category ->
            CategoryFilterSubcategorySection(
                category = category,
                criteria = criteria,
                groupContext = groupContext,
                showAllCategories = showAllCategories,
                onSubcategoryToggled = { subcategory -> onCriteriaChange(toggleSubcategory(criteria, subcategory)) }
            )
        }
    }
}

private fun resolveVisibleCategories(
    groupContext: GroupContext,
    showAllCategories: Boolean,
    selectedCategories: Set<ExpenseCategory>
): ImmutableList<ExpenseCategory> {
    if (showAllCategories) return ExpenseCategory.entries.toImmutableList()
    val base = ExpenseCategory.forContext(groupContext)
    val missingSelected = selectedCategories.filter { it !in base }
    return (base + missingSelected).toImmutableList()
}

private fun toggleCategory(
    criteria: ExpenseFilterCriteria,
    category: ExpenseCategory
): ExpenseFilterCriteria {
    val isSelected = category in criteria.selectedCategories
    val updatedCategories = if (isSelected) {
        criteria.selectedCategories - category
    } else {
        criteria.selectedCategories + category
    }
    val updatedSubcategories = if (isSelected) {
        criteria.selectedSubcategories.filterNot { it.parentCategory == category }.toSet()
    } else {
        criteria.selectedSubcategories
    }
    return criteria.copy(
        selectedCategories = updatedCategories,
        selectedSubcategories = updatedSubcategories
    )
}

private fun toggleSubcategory(
    criteria: ExpenseFilterCriteria,
    subcategory: ExpenseSubcategory
): ExpenseFilterCriteria {
    val isSelected = subcategory in criteria.selectedSubcategories
    val updatedSubcategories = if (isSelected) {
        criteria.selectedSubcategories - subcategory
    } else {
        criteria.selectedSubcategories + subcategory
    }
    return criteria.copy(selectedSubcategories = updatedSubcategories)
}
