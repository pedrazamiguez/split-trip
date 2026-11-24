package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryAiClassifierService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryClassifierService
import es.pedrazamiguez.splittrip.domain.usecase.expense.SuggestExpenseCategoryUseCase

class SuggestExpenseCategoryUseCaseImpl(
    private val expenseCategoryClassifierService: ExpenseCategoryClassifierService,
    private val expenseCategoryAiClassifierService: ExpenseCategoryAiClassifierService
) : SuggestExpenseCategoryUseCase {

    override suspend operator fun invoke(
        title: String,
        groupContext: GroupContext
    ): CategorySuggestion? {
        val trimmedTitle = title.trim()
        if (trimmedTitle.length < MIN_TITLE_LENGTH) return null

        val localSuggestion = expenseCategoryClassifierService.classify(
            title = trimmedTitle,
            groupContext = groupContext
        )
        if (localSuggestion != null) {
            return localSuggestion
        }

        return expenseCategoryAiClassifierService.classify(
            title = trimmedTitle,
            groupContext = groupContext
        )
    }

    private companion object {
        private const val MIN_TITLE_LENGTH = 2
    }
}
