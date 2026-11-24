package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryClassifierService
import es.pedrazamiguez.splittrip.domain.service.category.CategoryCandidate
import es.pedrazamiguez.splittrip.domain.service.category.CategoryEverydayAndLifestyleRules
import es.pedrazamiguez.splittrip.domain.service.category.CategoryKeywordRule
import es.pedrazamiguez.splittrip.domain.service.category.CategoryTravelAndCoreRules
import es.pedrazamiguez.splittrip.domain.service.category.ScoredCategoryCandidate
import java.text.Normalizer
import java.util.Locale

class ExpenseCategoryClassifierServiceImpl : ExpenseCategoryClassifierService {

    private val allRules: List<CategoryKeywordRule> =
        CategoryTravelAndCoreRules.rules + CategoryEverydayAndLifestyleRules.rules

    override fun classify(
        title: String,
        groupContext: GroupContext
    ): CategorySuggestion? {
        val normalized = normalizeTitle(title) ?: return null
        val scoredCandidates = scoreRules(normalized = normalized, groupContext = groupContext)
        return selectWinner(scoredCandidates)
    }

    private fun normalizeTitle(title: String): String? {
        val decomposed = Normalizer.normalize(title, Normalizer.Form.NFD)
        val withoutDiacritics = COMBINING_MARKS_REGEX.replace(decomposed, "")
        val alphanumericOnly = NON_ALPHANUMERIC_REGEX.replace(withoutDiacritics, " ")
        val collapsed = WHITESPACE_REGEX.replace(alphanumericOnly, " ").trim().lowercase(Locale.ROOT)
        return collapsed.ifBlank { null }
    }

    private fun scoreRules(
        normalized: String,
        groupContext: GroupContext
    ): Collection<ScoredCategoryCandidate> {
        val tokenSet = normalized.split(' ').filter { it.isNotEmpty() }.toSet()
        val paddedText = " $normalized "
        val candidates = LinkedHashMap<CategoryCandidate, ScoredCategoryCandidate>()

        for (rule in allRules) {
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
        candidates[scored.candidate] = if (existing == null) {
            scored
        } else {
            existing.copy(
                baseScore = existing.baseScore + scored.baseScore,
                totalScore = maxOf(existing.totalScore, scored.totalScore),
                fitsContext = existing.fitsContext || scored.fitsContext
            )
        }
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
        private val COMBINING_MARKS_REGEX = Regex("\\p{M}+")
        private val NON_ALPHANUMERIC_REGEX = Regex("[^\\p{L}\\p{N}\\s]+")
        private val WHITESPACE_REGEX = Regex("\\s+")

        private const val PHRASE_MATCH_WEIGHT = 6
        private const val KEYWORD_MATCH_WEIGHT = 3
        private const val MIN_MATCH_BASE_SCORE = 3
        private const val SUBCATEGORY_SPECIFICITY_BONUS = 1
        private const val CONTEXT_MATCH_BONUS = 2
    }
}
