package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory

data class CategorySuggestion(
    val category: ExpenseCategory,
    val subcategory: ExpenseSubcategory? = null,
    val source: CategorySuggestionSource = CategorySuggestionSource.LOCAL_RULES
) {
    init {
        require(
            subcategory == null ||
                (subcategory != ExpenseSubcategory.UNSPECIFIED && subcategory.parentCategory == category)
        ) {
            "Subcategory $subcategory does not belong to category $category"
        }
    }

    companion object {
        /**
         * Creates a taxonomy-safe [CategorySuggestion]. If [subcategory] is [ExpenseSubcategory.UNSPECIFIED]
         * or its [ExpenseSubcategory.parentCategory] does not equal [category], [subcategory] degrades to `null`.
         */
        fun sanitized(
            category: ExpenseCategory,
            subcategory: ExpenseSubcategory?,
            source: CategorySuggestionSource = CategorySuggestionSource.LOCAL_RULES
        ): CategorySuggestion {
            val validSubcategory = subcategory?.takeIf {
                it != ExpenseSubcategory.UNSPECIFIED && it.parentCategory == category
            }
            return CategorySuggestion(
                category = category,
                subcategory = validSubcategory,
                source = source
            )
        }
    }
}

enum class CategorySuggestionSource {
    LOCAL_RULES,
    ON_DEVICE_AI
}
