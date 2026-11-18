package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardSectionLabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.CategoryGridSelector
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.subcategoryGridEnterTransition
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.subcategoryGridExitTransition
import kotlinx.collections.immutable.toImmutableList

private val ALL_CATEGORIES = ExpenseCategory.entries.toImmutableList()

@Composable
fun CategoryFilterSection(
    criteria: ExpenseFilterCriteria,
    onCriteriaChange: (ExpenseFilterCriteria) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            CardSectionLabelText(text = stringResource(R.string.expenses_filter_section_category))

            CategoryGridSelector(
                items = ALL_CATEGORIES,
                isItemSelected = { it in criteria.selectedCategories },
                onItemClicked = { category -> onCriteriaChange(toggleCategory(criteria, category)) },
                itemId = { it.name },
                itemLabel = { stringResource(it.toStringRes()) },
                itemIcon = { it.toIconVector() }
            )
        }

        ALL_CATEGORIES.forEach { category ->
            val subcategories = remember(category) { ExpenseSubcategory.forCategory(category).toImmutableList() }

            AnimatedVisibility(
                visible = category in criteria.selectedCategories && subcategories.isNotEmpty(),
                enter = subcategoryGridEnterTransition(),
                exit = subcategoryGridExitTransition()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
                ) {
                    CardTitleText(
                        text = stringResource(category.toStringRes()),
                        color = MaterialTheme.colorScheme.primary
                    )

                    CategoryGridSelector(
                        items = subcategories,
                        isItemSelected = { it in criteria.selectedSubcategories },
                        onItemClicked = { subcategory -> onCriteriaChange(toggleSubcategory(criteria, subcategory)) },
                        itemId = { it.name },
                        itemLabel = { stringResource(it.toStringRes()) },
                        itemIcon = { it.toIconVector() }
                    )
                }
            }
        }
    }
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
