package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal object CategoryFilterLogic {

    data class CategoryClickResult(
        val updatedCriteria: ExpenseFilterCriteria,
        val nextFocusedCategory: ExpenseCategory?
    )

    fun resolveVisibleCategories(
        availableCategories: List<ExpenseCategory>,
        selectedCategories: Set<ExpenseCategory>
    ): ImmutableList<ExpenseCategory> {
        if (availableCategories.isEmpty() && selectedCategories.isEmpty()) {
            return persistentListOf()
        }
        val visibleSet = buildSet {
            addAll(availableCategories)
            addAll(selectedCategories)
        }
        return ExpenseCategory.entries
            .filter { it in visibleSet }
            .toImmutableList()
    }

    fun resolveVisibleSubcategories(
        category: ExpenseCategory,
        availableSubcategories: List<ExpenseSubcategory>,
        selectedSubcategories: Set<ExpenseSubcategory>
    ): ImmutableList<ExpenseSubcategory> {
        val visibleSet = buildSet {
            availableSubcategories.forEach { if (it.parentCategory == category) add(it) }
            selectedSubcategories.forEach { if (it.parentCategory == category) add(it) }
        }
        if (visibleSet.isEmpty()) return persistentListOf()
        return ExpenseSubcategory.forCategory(category)
            .filter { it in visibleSet }
            .toImmutableList()
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
