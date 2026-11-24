package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal object CategoryFilterLogic {

    data class CategoryClickResult(
        val updatedCriteria: ExpenseFilterCriteria,
        val nextFocusedCategory: ExpenseCategory?
    )

    fun resolveVisibleCategories(
        groupContext: GroupContext,
        showAllCategories: Boolean,
        selectedCategories: Set<ExpenseCategory>
    ): ImmutableList<ExpenseCategory> {
        if (showAllCategories) return ExpenseCategory.entries.toImmutableList()
        val base = ExpenseCategory.forContext(groupContext)
        val missingSelected = selectedCategories.filter { it !in base }
        return (base + missingSelected).toImmutableList()
    }

    fun resolveVisibleSubcategories(
        category: ExpenseCategory,
        selectedSubcategories: Set<ExpenseSubcategory>,
        groupContext: GroupContext,
        showAllCategories: Boolean
    ): ImmutableList<ExpenseSubcategory> {
        if (showAllCategories || !category.appliesTo(groupContext)) {
            return ExpenseSubcategory.forCategory(category).toImmutableList()
        }
        val base = ExpenseSubcategory.forCategory(category, groupContext)
        val selectedForCategory = selectedSubcategories.filter {
            it.parentCategory == category && it !in base
        }
        return (base + selectedForCategory).distinct().toImmutableList()
    }

    fun resolveFocusedCategory(
        currentFocused: ExpenseCategory?,
        selectedCategories: Set<ExpenseCategory>,
        visibleCategories: List<ExpenseCategory>
    ): ExpenseCategory? {
        if (selectedCategories.isEmpty()) return null
        if (currentFocused != null &&
            currentFocused in selectedCategories &&
            currentFocused in visibleCategories
        ) {
            return currentFocused
        }
        return visibleCategories.firstOrNull { it in selectedCategories }
            ?: selectedCategories.firstOrNull()
    }

    fun handleCategoryClick(
        criteria: ExpenseFilterCriteria,
        clickedCategory: ExpenseCategory,
        focusedCategory: ExpenseCategory?,
        visibleCategories: List<ExpenseCategory>
    ): CategoryClickResult {
        if (clickedCategory !in criteria.selectedCategories) {
            return CategoryClickResult(
                updatedCriteria = criteria.copy(
                    selectedCategories = criteria.selectedCategories + clickedCategory
                ),
                nextFocusedCategory = clickedCategory
            )
        }
        if (clickedCategory != focusedCategory) {
            return CategoryClickResult(
                updatedCriteria = criteria,
                nextFocusedCategory = clickedCategory
            )
        }
        val updatedCategories = criteria.selectedCategories - clickedCategory
        val updatedSubcategories = criteria.selectedSubcategories
            .filterNot { it.parentCategory == clickedCategory }
            .toSet()
        val updatedCriteria = criteria.copy(
            selectedCategories = updatedCategories,
            selectedSubcategories = updatedSubcategories
        )
        val nextFocused = resolveFocusedCategory(
            currentFocused = null,
            selectedCategories = updatedCategories,
            visibleCategories = visibleCategories
        )
        return CategoryClickResult(
            updatedCriteria = updatedCriteria,
            nextFocusedCategory = nextFocused
        )
    }

    fun toggleSubcategory(
        criteria: ExpenseFilterCriteria,
        subcategory: ExpenseSubcategory
    ): ExpenseFilterCriteria {
        val updatedSubcategories = if (subcategory in criteria.selectedSubcategories) {
            criteria.selectedSubcategories - subcategory
        } else {
            criteria.selectedSubcategories + subcategory
        }
        return criteria.copy(selectedSubcategories = updatedSubcategories)
    }

    fun countSelectedSubcategories(
        criteria: ExpenseFilterCriteria,
        category: ExpenseCategory
    ): Int = criteria.selectedSubcategories.count { it.parentCategory == category }
}
