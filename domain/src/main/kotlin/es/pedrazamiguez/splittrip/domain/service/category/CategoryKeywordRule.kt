package es.pedrazamiguez.splittrip.domain.service.category

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory

internal data class CategoryKeywordRule(
    val category: ExpenseCategory,
    val subcategory: ExpenseSubcategory? = null,
    val phrases: List<String> = emptyList(),
    val keywords: List<String> = emptyList()
)

internal data class CategoryCandidate(
    val category: ExpenseCategory,
    val subcategory: ExpenseSubcategory?
)

internal data class ScoredCategoryCandidate(
    val candidate: CategoryCandidate,
    val baseScore: Int,
    val totalScore: Int,
    val fitsContext: Boolean
)
