package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory

data class CategoryKeywordRule(
    val category: ExpenseCategory,
    val subcategory: ExpenseSubcategory? = null,
    val phrases: List<String> = emptyList(),
    val keywords: List<String> = emptyList()
) {
    init {
        require(
            subcategory == null ||
                (subcategory != ExpenseSubcategory.UNSPECIFIED && subcategory.parentCategory == category)
        ) {
            "Subcategory $subcategory does not belong to category $category"
        }
    }
}
