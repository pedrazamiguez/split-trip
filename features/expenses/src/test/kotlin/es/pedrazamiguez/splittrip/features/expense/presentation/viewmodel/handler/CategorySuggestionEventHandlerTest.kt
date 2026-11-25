package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import es.pedrazamiguez.splittrip.domain.usecase.expense.SuggestExpenseCategoryUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategorySuggestionEventHandlerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var suggestExpenseCategoryUseCase: SuggestExpenseCategoryUseCase
    private lateinit var addExpenseOptionsUiMapper: AddExpenseOptionsUiMapper
    private lateinit var uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var actions: MutableSharedFlow<AddExpenseUiAction>
    private lateinit var handlerScope: CoroutineScope
    private lateinit var handler: CategorySuggestionEventHandler

    @BeforeEach
    fun setUp() {
        suggestExpenseCategoryUseCase = mockk()
        val resourceProvider = mockk<ResourceProvider>()
        every { resourceProvider.getString(any()) } answers { "res_${firstArg<Int>()}" }
        val formattingHelper = mockk<FormattingHelper>(relaxed = true)
        addExpenseOptionsUiMapper = AddExpenseOptionsUiMapper(
            resourceProvider = resourceProvider,
            formattingHelper = formattingHelper
        )

        val initialCategories = addExpenseOptionsUiMapper.mapCategoriesForContext(
            context = GroupContext.TRAVEL,
            showAll = false
        )
        val initialCategory = initialCategories.find { it.id == ExpenseCategory.OTHER.name }
        uiState = MutableStateFlow(
            AddExpenseUiState(
                isConfigLoaded = true,
                isEditMode = false,
                groupContext = GroupContext.TRAVEL,
                availableCategories = initialCategories,
                selectedCategory = initialCategory
            )
        )
        actions = MutableSharedFlow()
        handlerScope = CoroutineScope(SupervisorJob() + testDispatcher)
        handler = CategorySuggestionEventHandler(
            suggestExpenseCategoryUseCase = suggestExpenseCategoryUseCase,
            addExpenseOptionsUiMapper = addExpenseOptionsUiMapper,
            debounceMs = 250L
        )
        handler.bind(uiState, actions, handlerScope)
    }

    @Test
    fun `does not trigger prediction in edit mode`() = runTest(testDispatcher) {
        uiState.value = uiState.value.copy(isEditMode = true, expenseTitle = "Dinner")

        handler.handleTitleChanged("Dinner")
        advanceUntilIdle()

        coVerify(exactly = 0) { suggestExpenseCategoryUseCase(any(), any()) }
        handlerScope.cancel()
    }

    @Test
    fun `does not trigger prediction before config is loaded`() = runTest(testDispatcher) {
        uiState.value = uiState.value.copy(isConfigLoaded = false, expenseTitle = "Dinner")

        handler.handleTitleChanged("Dinner")
        advanceUntilIdle()

        coVerify(exactly = 0) { suggestExpenseCategoryUseCase(any(), any()) }
        handlerScope.cancel()
    }

    @Test
    fun `does not trigger prediction when title is blank`() = runTest(testDispatcher) {
        uiState.value = uiState.value.copy(expenseTitle = "   ")

        handler.handleTitleChanged("   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { suggestExpenseCategoryUseCase(any(), any()) }
        handlerScope.cancel()
    }

    @Test
    fun `debounces rapid TitleChanged events and cancels prior in-flight jobs so only latest title applies`() =
        runTest(testDispatcher) {
            coEvery { suggestExpenseCategoryUseCase("din", GroupContext.TRAVEL) } coAnswers {
                delay(500L)
                CategorySuggestion(ExpenseCategory.OTHER, ExpenseSubcategory.TIPS_GRATUITIES)
            }
            coEvery { suggestExpenseCategoryUseCase("hotel", GroupContext.TRAVEL) } returns CategorySuggestion(
                category = ExpenseCategory.LODGING,
                subcategory = ExpenseSubcategory.HOTEL
            )

            uiState.value = uiState.value.copy(expenseTitle = "din")
            handler.handleTitleChanged("din")
            advanceTimeBy(100L)

            uiState.value = uiState.value.copy(expenseTitle = "hotel")
            handler.handleTitleChanged("hotel")
            advanceUntilIdle()

            coVerify(exactly = 0) { suggestExpenseCategoryUseCase("din", any()) }
            coVerify(exactly = 1) { suggestExpenseCategoryUseCase("hotel", GroupContext.TRAVEL) }
            assertEquals(ExpenseCategory.LODGING.name, uiState.value.selectedCategory?.id)
            assertEquals(ExpenseSubcategory.HOTEL, uiState.value.selectedSubcategory?.subcategory)
            handlerScope.cancel()
        }

    @Test
    fun `applies predicted category and subcategory within group context without expanding showAllCategories`() =
        runTest(testDispatcher) {
            coEvery { suggestExpenseCategoryUseCase("dinner at Bahn Mi", GroupContext.TRAVEL) } returns
                CategorySuggestion(
                    category = ExpenseCategory.FOOD,
                    subcategory = ExpenseSubcategory.RESTAURANT
                )

            uiState.value = uiState.value.copy(expenseTitle = "dinner at Bahn Mi")
            handler.handleTitleChanged("dinner at Bahn Mi")
            advanceUntilIdle()

            assertFalse(uiState.value.showAllCategories)
            assertEquals(ExpenseCategory.FOOD.name, uiState.value.selectedCategory?.id)
            assertEquals(ExpenseSubcategory.RESTAURANT, uiState.value.selectedSubcategory?.subcategory)
            handlerScope.cancel()
        }

    @Test
    fun `expands showAllCategories when predicted category or subcategory falls outside groupContext`() =
        runTest(testDispatcher) {
            coEvery { suggestExpenseCategoryUseCase("cuota gimnasio", GroupContext.TRAVEL) } returns
                CategorySuggestion(
                    category = ExpenseCategory.HEALTH_PERSONAL_CARE,
                    subcategory = ExpenseSubcategory.GYM_FITNESS
                )

            uiState.value = uiState.value.copy(expenseTitle = "cuota gimnasio")
            handler.handleTitleChanged("cuota gimnasio")
            advanceUntilIdle()

            assertTrue(uiState.value.showAllCategories)
            assertEquals(ExpenseCategory.HEALTH_PERSONAL_CARE.name, uiState.value.selectedCategory?.id)
            assertEquals(ExpenseSubcategory.GYM_FITNESS, uiState.value.selectedSubcategory?.subcategory)
            assertTrue(uiState.value.availableCategories.any { it.id == ExpenseCategory.HEALTH_PERSONAL_CARE.name })
            assertTrue(
                uiState.value.availableSubcategories.any { it.subcategory == ExpenseSubcategory.GYM_FITNESS }
            )
            handlerScope.cancel()
        }

    @Test
    fun `does not overwrite category after handleManualCategoryOverride is called`() = runTest(testDispatcher) {
        handler.handleManualCategoryOverride()
        uiState.value = uiState.value.copy(expenseTitle = "hotel")

        handler.handleTitleChanged("hotel")
        advanceUntilIdle()

        coVerify(exactly = 0) { suggestExpenseCategoryUseCase(any(), any()) }
        assertEquals(ExpenseCategory.OTHER.name, uiState.value.selectedCategory?.id)
        handlerScope.cancel()
    }

    @Test
    fun `cancels in-flight prediction when handleManualCategoryOverride is called before prediction resolves`() =
        runTest(testDispatcher) {
            val deferred = CompletableDeferred<CategorySuggestion?>()
            coEvery { suggestExpenseCategoryUseCase("hotel", GroupContext.TRAVEL) } coAnswers {
                deferred.await()
            }

            uiState.value = uiState.value.copy(expenseTitle = "hotel")
            handler.handleTitleChanged("hotel")
            advanceTimeBy(250L)
            runCurrent()

            handler.handleManualCategoryOverride()
            deferred.complete(CategorySuggestion(ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL))
            advanceUntilIdle()

            assertEquals(ExpenseCategory.OTHER.name, uiState.value.selectedCategory?.id)
            assertNull(uiState.value.selectedSubcategory)
            handlerScope.cancel()
        }

    @Test
    fun `ignores late-arriving prediction if user cleared title while inference was in flight`() =
        runTest(testDispatcher) {
            val deferred = CompletableDeferred<CategorySuggestion?>()
            coEvery { suggestExpenseCategoryUseCase("hotel", GroupContext.TRAVEL) } coAnswers {
                deferred.await()
            }

            uiState.value = uiState.value.copy(expenseTitle = "hotel")
            handler.handleTitleChanged("hotel")
            advanceTimeBy(250L)
            runCurrent()

            uiState.value = uiState.value.copy(expenseTitle = "")
            deferred.complete(CategorySuggestion(ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL))
            advanceUntilIdle()

            assertEquals(ExpenseCategory.OTHER.name, uiState.value.selectedCategory?.id)
            assertNull(uiState.value.selectedSubcategory)
            handlerScope.cancel()
        }

    @Test
    fun `ignores late-arriving prediction if title changed while inference was in flight`() =
        runTest(testDispatcher) {
            val deferred = CompletableDeferred<CategorySuggestion?>()
            coEvery { suggestExpenseCategoryUseCase("hotel", GroupContext.TRAVEL) } coAnswers {
                deferred.await()
            }

            uiState.value = uiState.value.copy(expenseTitle = "hotel")
            handler.handleTitleChanged("hotel")
            advanceTimeBy(250L)
            runCurrent()

            uiState.value = uiState.value.copy(expenseTitle = "taxi")
            deferred.complete(CategorySuggestion(ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL))
            advanceUntilIdle()

            assertEquals(ExpenseCategory.OTHER.name, uiState.value.selectedCategory?.id)
            assertNull(uiState.value.selectedSubcategory)
            handlerScope.cancel()
        }

    @Test
    fun `ignores late-arriving prediction and cancels job once handleCategoryStepReached is called`() =
        runTest(testDispatcher) {
            val deferred = CompletableDeferred<CategorySuggestion?>()
            coEvery { suggestExpenseCategoryUseCase("hotel", GroupContext.TRAVEL) } coAnswers {
                deferred.await()
            }

            uiState.value = uiState.value.copy(expenseTitle = "hotel")
            handler.handleTitleChanged("hotel")
            advanceTimeBy(250L)
            runCurrent()

            uiState.value = uiState.value.copy(currentStep = AddExpenseStep.CATEGORY)
            handler.handleCategoryStepReached()
            deferred.complete(CategorySuggestion(ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL))
            advanceUntilIdle()

            assertEquals(ExpenseCategory.OTHER.name, uiState.value.selectedCategory?.id)
            assertNull(uiState.value.selectedSubcategory)
            handlerScope.cancel()
        }

    @Test
    fun `reset clears lock flags and allows suggestions again for a newly loaded group`() =
        runTest(testDispatcher) {
            handler.handleManualCategoryOverride()
            handler.handleCategoryStepReached()
            handler.reset()

            val everydayCategories = addExpenseOptionsUiMapper.mapCategoriesForContext(
                context = GroupContext.EVERYDAY,
                showAll = false
            )
            uiState.value = AddExpenseUiState(
                isConfigLoaded = true,
                groupContext = GroupContext.EVERYDAY,
                availableCategories = everydayCategories,
                selectedCategory = everydayCategories.find { it.id == ExpenseCategory.OTHER.name },
                expenseTitle = "factura del agua"
            )
            coEvery { suggestExpenseCategoryUseCase("factura del agua", GroupContext.EVERYDAY) } returns
                CategorySuggestion(
                    category = ExpenseCategory.HOME,
                    subcategory = ExpenseSubcategory.UTILITIES,
                    source = CategorySuggestionSource.LOCAL_RULES
                )

            handler.handleTitleChanged("factura del agua")
            advanceUntilIdle()

            assertEquals(ExpenseCategory.HOME.name, uiState.value.selectedCategory?.id)
            assertEquals(ExpenseSubcategory.UTILITIES, uiState.value.selectedSubcategory?.subcategory)
            handlerScope.cancel()
        }

    @Test
    fun `handles use case exception gracefully without crashing or mutating category`() =
        runTest(testDispatcher) {
            coEvery { suggestExpenseCategoryUseCase("hotel", GroupContext.TRAVEL) } throws
                IllegalStateException("Unexpected error")

            uiState.value = uiState.value.copy(expenseTitle = "hotel")
            handler.handleTitleChanged("hotel")
            advanceUntilIdle()

            assertEquals(ExpenseCategory.OTHER.name, uiState.value.selectedCategory?.id)
            assertNull(uiState.value.selectedSubcategory)
            handlerScope.cancel()
        }

    @Test
    fun `keeps showAllCategories false and preserves default category when use case returns null for ambiguous title`():
        Unit = runTest(testDispatcher) {
        coEvery { suggestExpenseCategoryUseCase("agua", GroupContext.TRAVEL) } returns null

        uiState.value = uiState.value.copy(expenseTitle = "agua")
        handler.handleTitleChanged("agua")
        advanceUntilIdle()

        assertFalse(uiState.value.showAllCategories)
        assertEquals(ExpenseCategory.OTHER.name, uiState.value.selectedCategory?.id)
        assertNull(uiState.value.selectedSubcategory)
        handlerScope.cancel()
    }
}
