package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.enums.AppLanguage
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategoryPredictionOutcome
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryAiClassifierService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryClassifierService
import es.pedrazamiguez.splittrip.domain.usecase.expense.SuggestExpenseCategoryUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull

class SuggestExpenseCategoryUseCaseImpl(
    private val expenseCategoryClassifierService: ExpenseCategoryClassifierService,
    private val expenseCategoryAiClassifierService: ExpenseCategoryAiClassifierService,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val tierTwoDebounceMs: Long = DEFAULT_TIER_TWO_DEBOUNCE_MS
) : SuggestExpenseCategoryUseCase {

    override suspend operator fun invoke(
        title: String,
        groupContext: GroupContext
    ): CategorySuggestion? {
        val trimmedTitle = title.trim()
        if (trimmedTitle.length < MIN_TITLE_LENGTH) return null

        classifyTierOne(trimmedTitle, groupContext)?.let { return it }

        awaitTierTwoQuietWindow()
        return expenseCategoryAiClassifierService.classify(
            title = trimmedTitle,
            groupContext = groupContext
        )
    }

    override suspend fun diagnose(
        title: String,
        groupContext: GroupContext
    ): CategoryPredictionOutcome {
        val trimmedTitle = title.trim()
        if (trimmedTitle.length < MIN_TITLE_LENGTH) return CategoryPredictionOutcome.NoMatch

        val localSuggestion = classifyTierOne(trimmedTitle, groupContext)
        if (localSuggestion != null) {
            return CategoryPredictionOutcome.Matched(localSuggestion)
        }

        awaitTierTwoQuietWindow()
        return expenseCategoryAiClassifierService.classifyDetailed(
            title = trimmedTitle,
            groupContext = groupContext
        )
    }

    private suspend fun awaitTierTwoQuietWindow() {
        if (tierTwoDebounceMs > 0L) {
            delay(tierTwoDebounceMs)
        }
    }

    private suspend fun classifyTierOne(
        trimmedTitle: String,
        groupContext: GroupContext
    ): CategorySuggestion? {
        val activeLanguageCode = AppLanguage.resolveBaseLanguageCode(
            userPreferenceRepository.getAppLanguage().firstOrNull()
        )
        return expenseCategoryClassifierService.classify(
            title = trimmedTitle,
            groupContext = groupContext,
            languageCode = activeLanguageCode
        )
    }

    companion object {
        const val DEFAULT_TIER_TWO_DEBOUNCE_MS = 750L
        private const val MIN_TITLE_LENGTH = 2
    }
}
