package es.pedrazamiguez.splittrip.domain.model

data class CategoryClassifierRuleConfig(
    val mergeMode: CategoryRuleMergeMode = CategoryRuleMergeMode.MERGE,
    val commonRules: List<CategoryKeywordRule> = emptyList(),
    val languageRules: Map<String, List<CategoryKeywordRule>> = emptyMap(),
    val disabledTerms: Map<String, Set<String>> = emptyMap()
) {
    val isEmpty: Boolean
        get() = commonRules.isEmpty() &&
            languageRules.values.all { it.isEmpty() } &&
            disabledTerms.values.all { it.isEmpty() }

    companion object {
        val EMPTY = CategoryClassifierRuleConfig()
    }
}
