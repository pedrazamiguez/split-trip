package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategoryPredictionOutcome
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion

/**
 * Optional on-device AI fallback classifier for ambiguous expense titles
 * not confidently matched by local rules.
 */
interface ExpenseCategoryAiClassifierService {
    suspend fun classify(
        title: String,
        groupContext: GroupContext
    ): CategorySuggestion?

    suspend fun classifyDetailed(
        title: String,
        groupContext: GroupContext
    ): CategoryPredictionOutcome
}
