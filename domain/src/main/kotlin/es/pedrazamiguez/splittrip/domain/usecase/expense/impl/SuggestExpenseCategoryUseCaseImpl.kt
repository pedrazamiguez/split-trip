package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.enums.AppLanguage
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryAiClassifierService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryClassifierService
import es.pedrazamiguez.splittrip.domain.usecase.expense.SuggestExpenseCategoryUseCase
import kotlinx.coroutines.flow.firstOrNull

class SuggestExpenseCategoryUseCaseImpl(
    private val expenseCategoryClassifierService: ExpenseCategoryClassifierService,
    private val expenseCategoryAiClassifierService: ExpenseCategoryAiClassifierService,
    private val userPreferenceRepository: UserPreferenceRepository
) : SuggestExpenseCategoryUseCase {

    override suspend operator fun invoke(
        title: String,
        groupContext: GroupContext
    ): CategorySuggestion? {
        val trimmedTitle = title.trim()
        if (trimmedTitle.length < MIN_TITLE_LENGTH) return null

        val activeLanguageCode = AppLanguage.resolveBaseLanguageCode(
            userPreferenceRepository.getAppLanguage().firstOrNull()
        )

        val localSuggestion = expenseCategoryClassifierService.classify(
            title = trimmedTitle,
            groupContext = groupContext,
            languageCode = activeLanguageCode
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
