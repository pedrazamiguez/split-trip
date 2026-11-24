package es.pedrazamiguez.splittrip.features.expense.presentation.model

import kotlinx.collections.immutable.ImmutableList

data class EditCategorySelection(
    val showAllCategories: Boolean,
    val availableCategories: ImmutableList<CategoryUiModel>,
    val selectedCategory: CategoryUiModel?,
    val availableSubcategories: ImmutableList<SubcategoryUiModel>,
    val selectedSubcategory: SubcategoryUiModel?
)
