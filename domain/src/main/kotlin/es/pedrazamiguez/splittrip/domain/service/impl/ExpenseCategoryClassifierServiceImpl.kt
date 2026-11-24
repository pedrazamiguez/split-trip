package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.enums.AppLanguage
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategoryClassifierRuleConfig
import es.pedrazamiguez.splittrip.domain.model.CategoryKeywordRule
import es.pedrazamiguez.splittrip.domain.model.CategoryRuleMergeMode
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryClassifierService
import es.pedrazamiguez.splittrip.domain.service.category.BundledCategoryRuleCatalog
import es.pedrazamiguez.splittrip.domain.service.category.CategoryCandidate
import es.pedrazamiguez.splittrip.domain.service.category.CategoryTextNormalizer
import es.pedrazamiguez.splittrip.domain.service.category.CompiledRuleSnapshot
import es.pedrazamiguez.splittrip.domain.service.category.ScoredCategoryCandidate

class ExpenseCategoryClassifierServiceImpl(
    private val appConfigService: AppConfigService
) : ExpenseCategoryClassifierService {

    @Volatile
    private var cachedSnapshot: CompiledRuleSnapshot? = null

    override fun classify(
        title: String,
        groupContext: GroupContext,
        languageCode: String?
    ): CategorySuggestion? {
        val normalized = CategoryTextNormalizer.normalize(title) ?: return null
        val activeLanguage = AppLanguage.resolveBaseLanguageCode(languageCode)
        val snapshot = resolveCompiledSnapshot()

        val primaryRules = snapshot.primaryPackByLanguage[activeLanguage] ?: snapshot.commonRules
        val primaryCandidates = scoreRules(
            rules = primaryRules,
            normalized = normalized,
            groupContext = groupContext
        )
        if (primaryCandidates.isNotEmpty()) {
            return selectWinner(primaryCandidates)
        }

        if (activeLanguage == BundledCategoryRuleCatalog.PACK_EN) {
            return null
        }

        val fallbackCandidates = scoreRules(
            rules = snapshot.englishFallbackRules,
            normalized = normalized,
            groupContext = groupContext
        )
        return selectWinner(fallbackCandidates)
    }

    private fun resolveCompiledSnapshot(): CompiledRuleSnapshot {
        val config = appConfigService.categoryClassifierRules.value
        val existing = cachedSnapshot
        if (existing != null && existing.sourceConfig === config) {
            return existing
        }
        val compiled = compileSnapshot(config)
        cachedSnapshot = compiled
        return compiled
    }

    private fun compileSnapshot(config: CategoryClassifierRuleConfig): CompiledRuleSnapshot {
        val normalizedDisabled = normalizeDisabledTerms(config.disabledTerms)
        val wildcardDisabled = normalizedDisabled[BundledCategoryRuleCatalog.WILDCARD_PACK].orEmpty()

        val filteredBundledCommon = applyDisabledTerms(
            rules = BundledCategoryRuleCatalog.commonRules,
            disabled = wildcardDisabled + normalizedDisabled[BundledCategoryRuleCatalog.PACK_COMMON].orEmpty()
        )
        val remoteCommon = config.commonRules.mapNotNull(::normalizeRule)
        val effectiveCommon = resolveEffectiveRules(
            mergeMode = config.mergeMode,
            bundledRules = filteredBundledCommon,
            remoteRules = remoteCommon
        )

        val remoteLanguages = normalizeLanguageRules(config.languageRules)
        val allLanguageCodes = BundledCategoryRuleCatalog.languagePacks.keys + remoteLanguages.keys
        val effectiveLanguages = LinkedHashMap<String, List<CategoryKeywordRule>>(allLanguageCodes.size)

        for (lang in allLanguageCodes) {
            val bundledLang = BundledCategoryRuleCatalog.languagePacks[lang].orEmpty()
            val filteredBundledLang = applyDisabledTerms(
                rules = bundledLang,
                disabled = wildcardDisabled + normalizedDisabled[lang].orEmpty()
            )
            val remoteLang = remoteLanguages[lang].orEmpty()
            effectiveLanguages[lang] = resolveEffectiveRules(
                mergeMode = config.mergeMode,
                bundledRules = filteredBundledLang,
                remoteRules = remoteLang
            )
        }

        val consolidatedCommon = consolidateByCandidate(effectiveCommon)
        val primaryPackByLanguage = LinkedHashMap<String, List<CategoryKeywordRule>>(effectiveLanguages.size)
        for ((lang, langRules) in effectiveLanguages) {
            primaryPackByLanguage[lang] = consolidateByCandidate(effectiveCommon + langRules)
        }

        return CompiledRuleSnapshot(
            sourceConfig = config,
            commonRules = consolidatedCommon,
            primaryPackByLanguage = primaryPackByLanguage,
            englishFallbackRules = consolidateByCandidate(
                effectiveLanguages[BundledCategoryRuleCatalog.PACK_EN].orEmpty()
            )
        )
    }

    private fun normalizeDisabledTerms(raw: Map<String, Set<String>>): Map<String, Set<String>> {
        if (raw.isEmpty()) return emptyMap()
        val result = LinkedHashMap<String, MutableSet<String>>(raw.size)
        for ((rawKey, terms) in raw) {
            val trimmedKey = rawKey.trim()
            if (trimmedKey.isEmpty()) continue
            val normalizedKey = when {
                trimmedKey == BundledCategoryRuleCatalog.WILDCARD_PACK -> BundledCategoryRuleCatalog.WILDCARD_PACK
                trimmedKey.equals(BundledCategoryRuleCatalog.PACK_COMMON, ignoreCase = true) ->
                    BundledCategoryRuleCatalog.PACK_COMMON
                else -> AppLanguage.resolveBaseLanguageCode(trimmedKey)
            }
            val normalizedTerms = terms.mapNotNull { CategoryTextNormalizer.normalize(it) }
            if (normalizedTerms.isNotEmpty()) {
                result.getOrPut(normalizedKey) { LinkedHashSet() }.addAll(normalizedTerms)
            }
        }
        return result
    }

    private fun normalizeLanguageRules(
        raw: Map<String, List<CategoryKeywordRule>>
    ): Map<String, List<CategoryKeywordRule>> {
        if (raw.isEmpty()) return emptyMap()
        val result = LinkedHashMap<String, MutableList<CategoryKeywordRule>>(raw.size)
        for ((rawKey, rules) in raw) {
            if (rawKey.isBlank()) continue
            val langCode = AppLanguage.resolveBaseLanguageCode(rawKey)
            val normalized = rules.mapNotNull(::normalizeRule)
            if (normalized.isNotEmpty()) {
                result.getOrPut(langCode) { mutableListOf() }.addAll(normalized)
            }
        }
        return result
    }

    private fun resolveEffectiveRules(
        mergeMode: CategoryRuleMergeMode,
        bundledRules: List<CategoryKeywordRule>,
        remoteRules: List<CategoryKeywordRule>
    ): List<CategoryKeywordRule> = when (mergeMode) {
        CategoryRuleMergeMode.MERGE -> bundledRules + remoteRules
        CategoryRuleMergeMode.REPLACE -> remoteRules.ifEmpty { bundledRules }
    }

    private fun applyDisabledTerms(
        rules: List<CategoryKeywordRule>,
        disabled: Set<String>
    ): List<CategoryKeywordRule> {
        if (disabled.isEmpty()) return rules
        return rules.mapNotNull { rule ->
            val filteredPhrases = rule.phrases.filterNot { it in disabled }
            val filteredKeywords = rule.keywords.filterNot { it in disabled }
            if (filteredPhrases.isEmpty() && filteredKeywords.isEmpty()) {
                null
            } else {
                rule.copy(phrases = filteredPhrases, keywords = filteredKeywords)
            }
        }
    }

    private fun normalizeRule(rule: CategoryKeywordRule): CategoryKeywordRule? {
        val phrases = LinkedHashSet<String>()
        val keywords = LinkedHashSet<String>()

        for (rawPhrase in rule.phrases) {
            val normalized = CategoryTextNormalizer.normalize(rawPhrase) ?: continue
            phrases.add(normalized)
        }
        for (rawKeyword in rule.keywords) {
            val normalized = CategoryTextNormalizer.normalize(rawKeyword) ?: continue
            if (' ' in normalized) {
                phrases.add(normalized)
            } else {
                keywords.add(normalized)
            }
        }

        if (phrases.isEmpty() && keywords.isEmpty()) return null
        return CategoryKeywordRule(
            category = rule.category,
            subcategory = rule.subcategory,
            phrases = phrases.toList(),
            keywords = keywords.toList()
        )
    }

    private fun consolidateByCandidate(rules: List<CategoryKeywordRule>): List<CategoryKeywordRule> {
        if (rules.isEmpty()) return emptyList()
        val phrasesByCandidate = LinkedHashMap<CategoryCandidate, LinkedHashSet<String>>()
        val keywordsByCandidate = LinkedHashMap<CategoryCandidate, LinkedHashSet<String>>()

        for (rule in rules) {
            val candidate = CategoryCandidate(category = rule.category, subcategory = rule.subcategory)
            phrasesByCandidate.getOrPut(candidate) { LinkedHashSet() }.addAll(rule.phrases)
            keywordsByCandidate.getOrPut(candidate) { LinkedHashSet() }.addAll(rule.keywords)
        }

        return phrasesByCandidate.keys.mapNotNull { candidate ->
            val phrases = phrasesByCandidate[candidate].orEmpty().toList()
            val keywords = keywordsByCandidate[candidate].orEmpty().toList()
            if (phrases.isEmpty() && keywords.isEmpty()) {
                null
            } else {
                CategoryKeywordRule(
                    category = candidate.category,
                    subcategory = candidate.subcategory,
                    phrases = phrases,
                    keywords = keywords
                )
            }
        }
    }

    private fun scoreRules(
        rules: List<CategoryKeywordRule>,
        normalized: String,
        groupContext: GroupContext
    ): Collection<ScoredCategoryCandidate> {
        val tokenSet = normalized.split(' ').filter { it.isNotEmpty() }.toSet()
        val paddedText = " $normalized "
        val candidates = LinkedHashMap<CategoryCandidate, ScoredCategoryCandidate>()

        for (rule in rules) {
            val scored = scoreRule(
                rule = rule,
                tokenSet = tokenSet,
                paddedText = paddedText,
                groupContext = groupContext
            ) ?: continue
            mergeCandidate(candidates, scored)
        }
        return candidates.values
    }

    private fun scoreRule(
        rule: CategoryKeywordRule,
        tokenSet: Set<String>,
        paddedText: String,
        groupContext: GroupContext
    ): ScoredCategoryCandidate? {
        val phraseScore = rule.phrases.count { phrase ->
            paddedText.contains(" $phrase ")
        } * PHRASE_MATCH_WEIGHT
        val keywordScore = rule.keywords.count { keyword ->
            keyword in tokenSet
        } * KEYWORD_MATCH_WEIGHT
        val baseScore = phraseScore + keywordScore
        if (baseScore < MIN_MATCH_BASE_SCORE) return null

        val specificityBonus = if (rule.subcategory != null) SUBCATEGORY_SPECIFICITY_BONUS else 0
        val fitsContext = rule.category.appliesTo(groupContext) &&
            (rule.subcategory == null || rule.subcategory.appliesTo(groupContext))
        val contextBonus = if (fitsContext) CONTEXT_MATCH_BONUS else 0

        return ScoredCategoryCandidate(
            candidate = CategoryCandidate(
                category = rule.category,
                subcategory = rule.subcategory
            ),
            baseScore = baseScore,
            totalScore = baseScore + specificityBonus + contextBonus,
            fitsContext = fitsContext
        )
    }

    private fun mergeCandidate(
        candidates: MutableMap<CategoryCandidate, ScoredCategoryCandidate>,
        scored: ScoredCategoryCandidate
    ) {
        val existing = candidates[scored.candidate]
        if (existing == null) {
            candidates[scored.candidate] = scored
            return
        }
        val combinedBaseScore = existing.baseScore + scored.baseScore
        val combinedFitsContext = existing.fitsContext || scored.fitsContext
        val specificityBonus = if (scored.candidate.subcategory != null) SUBCATEGORY_SPECIFICITY_BONUS else 0
        val contextBonus = if (combinedFitsContext) CONTEXT_MATCH_BONUS else 0
        candidates[scored.candidate] = existing.copy(
            baseScore = combinedBaseScore,
            totalScore = combinedBaseScore + specificityBonus + contextBonus,
            fitsContext = combinedFitsContext
        )
    }

    private fun selectWinner(scoredCandidates: Collection<ScoredCategoryCandidate>): CategorySuggestion? {
        val maxScore = scoredCandidates.maxOfOrNull { it.totalScore } ?: return null
        val topCandidates = scoredCandidates.filter { it.totalScore == maxScore }
        if (topCandidates.size == 1) {
            val winner = topCandidates.first().candidate
            return CategorySuggestion.sanitized(
                category = winner.category,
                subcategory = winner.subcategory,
                source = CategorySuggestionSource.LOCAL_RULES
            )
        }

        val contextNarrowed = topCandidates.filter { it.fitsContext }.ifEmpty { topCandidates }
        val specificNarrowed = contextNarrowed.filter { it.candidate.subcategory != null }.ifEmpty { contextNarrowed }
        if (specificNarrowed.size == 1) {
            val winner = specificNarrowed.first().candidate
            return CategorySuggestion.sanitized(
                category = winner.category,
                subcategory = winner.subcategory,
                source = CategorySuggestionSource.LOCAL_RULES
            )
        }

        val distinctCategories = specificNarrowed.map { it.candidate.category }.distinct()
        return if (distinctCategories.size == 1) {
            CategorySuggestion.sanitized(
                category = distinctCategories.first(),
                subcategory = null,
                source = CategorySuggestionSource.LOCAL_RULES
            )
        } else {
            null
        }
    }

    private companion object {
        private const val PHRASE_MATCH_WEIGHT = 6
        private const val KEYWORD_MATCH_WEIGHT = 3
        private const val MIN_MATCH_BASE_SCORE = 3
        private const val SUBCATEGORY_SPECIFICITY_BONUS = 1
        private const val CONTEXT_MATCH_BONUS = 2
    }
}
