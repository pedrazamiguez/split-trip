package es.pedrazamiguez.splittrip.domain.service.category

import es.pedrazamiguez.splittrip.domain.model.CategoryKeywordRule

internal object BundledCategoryRuleCatalog {
    const val PACK_COMMON = "common"
    const val PACK_EN = "en"
    const val PACK_ES = "es"
    const val WILDCARD_PACK = "*"

    val commonRules: List<CategoryKeywordRule> = CategoryCommonRules.rules

    val languagePacks: Map<String, List<CategoryKeywordRule>> = mapOf(
        PACK_EN to CategoryEnglishRules.rules,
        PACK_ES to CategorySpanishRules.rules
    )
}
