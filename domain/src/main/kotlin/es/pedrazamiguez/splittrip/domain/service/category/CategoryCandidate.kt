package es.pedrazamiguez.splittrip.domain.service.category

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.CategoryClassifierRuleConfig
import es.pedrazamiguez.splittrip.domain.model.CategoryKeywordRule

internal data class CategoryCandidate(
    val category: ExpenseCategory,
    val subcategory: ExpenseSubcategory?
)

internal data class ScoredCategoryCandidate(
    val candidate: CategoryCandidate,
    val baseScore: Int,
    val totalScore: Int,
    val fitsContext: Boolean,
    val matchedPhrases: Set<String> = emptySet(),
    val matchedKeywords: Set<String> = emptySet()
)

internal data class CompiledRuleSnapshot(
    val sourceConfig: CategoryClassifierRuleConfig,
    val commonRules: List<CategoryKeywordRule>,
    val primaryPackByLanguage: Map<String, List<CategoryKeywordRule>>,
    val englishFallbackRules: List<CategoryKeywordRule>
)
