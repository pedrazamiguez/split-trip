package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryAiClassifierService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCategoryClassifierService
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.SuggestExpenseCategoryUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Locale
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SuggestExpenseCategoryUseCaseTest {

    private lateinit var localClassifierService: ExpenseCategoryClassifierService
    private lateinit var aiClassifierService: ExpenseCategoryAiClassifierService
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private lateinit var useCase: SuggestExpenseCategoryUseCaseImpl

    @BeforeEach
    fun setUp() {
        localClassifierService = mockk()
        aiClassifierService = mockk()
        userPreferenceRepository = mockk {
            every { getAppLanguage() } returns flowOf("es")
        }
        useCase = SuggestExpenseCategoryUseCaseImpl(
            expenseCategoryClassifierService = localClassifierService,
            expenseCategoryAiClassifierService = aiClassifierService,
            userPreferenceRepository = userPreferenceRepository
        )
    }

    @Test
    fun `returns null immediately without invoking classifiers for blank or 1-char title`() = runTest {
        assertNull(useCase("   ", GroupContext.TRAVEL))
        assertNull(useCase("a", GroupContext.EVERYDAY))

        verify(exactly = 0) { localClassifierService.classify(any(), any(), any()) }
        coVerify(exactly = 0) { aiClassifierService.classify(any(), any()) }
    }

    @Test
    fun `returns Tier 1 local suggestion and never invokes Tier 2 AI classifier when local matches`() = runTest {
        val localResult = CategorySuggestion(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.RESTAURANT,
            source = CategorySuggestionSource.LOCAL_RULES
        )
        every { localClassifierService.classify("Dinner", GroupContext.TRAVEL, "es") } returns localResult

        val actual = useCase("Dinner", GroupContext.TRAVEL)

        assertEquals(localResult, actual)
        verify(exactly = 1) { localClassifierService.classify("Dinner", GroupContext.TRAVEL, "es") }
        coVerify(exactly = 0) { aiClassifierService.classify(any(), any()) }
    }

    @Test
    fun `falls back to Tier 2 AI classifier when Tier 1 returns null`() = runTest {
        val aiResult = CategorySuggestion(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.TOUR_EXCURSION,
            source = CategorySuggestionSource.ON_DEVICE_AI
        )
        every { localClassifierService.classify("Shaxi outing", GroupContext.TRAVEL, "es") } returns null
        coEvery { aiClassifierService.classify("Shaxi outing", GroupContext.TRAVEL) } returns aiResult

        val actual = useCase("Shaxi outing", GroupContext.TRAVEL)

        assertEquals(aiResult, actual)
        verify(exactly = 1) { localClassifierService.classify("Shaxi outing", GroupContext.TRAVEL, "es") }
        coVerify(exactly = 1) { aiClassifierService.classify("Shaxi outing", GroupContext.TRAVEL) }
    }

    @Test
    fun `returns null when both Tier 1 and Tier 2 return null`() = runTest {
        every { localClassifierService.classify("Unknown item", GroupContext.EVERYDAY, "es") } returns null
        coEvery { aiClassifierService.classify("Unknown item", GroupContext.EVERYDAY) } returns null

        val actual = useCase("Unknown item", GroupContext.EVERYDAY)

        assertNull(actual)
    }

    @Test
    fun `trims input title before passing to classifiers`() = runTest {
        every { localClassifierService.classify("hotel", GroupContext.TRAVEL, "es") } returns null
        coEvery { aiClassifierService.classify("hotel", GroupContext.TRAVEL) } returns null

        useCase("  hotel  ", GroupContext.TRAVEL)

        verify(exactly = 1) { localClassifierService.classify("hotel", GroupContext.TRAVEL, "es") }
        coVerify(exactly = 1) { aiClassifierService.classify("hotel", GroupContext.TRAVEL) }
    }

    @Test
    fun `resolves Andaluz preference es-AN to es base language code before invoking Tier 1 classifier`() = runTest {
        every { userPreferenceRepository.getAppLanguage() } returns flowOf("es-AN")
        val localResult = CategorySuggestion(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.UTILITIES,
            source = CategorySuggestionSource.LOCAL_RULES
        )
        every { localClassifierService.classify("factura del agua", GroupContext.EVERYDAY, "es") } returns localResult

        val actual = useCase("factura del agua", GroupContext.EVERYDAY)

        assertEquals(localResult, actual)
        verify(exactly = 1) { localClassifierService.classify("factura del agua", GroupContext.EVERYDAY, "es") }
    }

    @Test
    fun `falls back to system default locale when stored app language preference is null`() = runTest {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("fr"))
            every { userPreferenceRepository.getAppLanguage() } returns flowOf(null)
            every { localClassifierService.classify("boulangerie", GroupContext.TRAVEL, "fr") } returns null
            coEvery { aiClassifierService.classify("boulangerie", GroupContext.TRAVEL) } returns null

            useCase("boulangerie", GroupContext.TRAVEL)

            verify(exactly = 1) { localClassifierService.classify("boulangerie", GroupContext.TRAVEL, "fr") }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
