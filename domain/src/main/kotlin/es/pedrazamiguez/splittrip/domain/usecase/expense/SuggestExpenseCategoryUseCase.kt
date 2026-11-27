package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategoryPredictionOutcome
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion

interface SuggestExpenseCategoryUseCase {
    suspend operator fun invoke(
        title: String,
        groupContext: GroupContext
    ): CategorySuggestion?

    suspend fun diagnose(
        title: String,
        groupContext: GroupContext
    ): CategoryPredictionOutcome
}
