package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.usecase.expense.SuggestExpenseCategoryUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CategorySuggestionEventHandler(
    private val suggestExpenseCategoryUseCase: SuggestExpenseCategoryUseCase,
    private val addExpenseOptionsUiMapper: AddExpenseOptionsUiMapper,
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS
) : AddExpenseEventHandler {

    private lateinit var _uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var _actionsFlow: MutableSharedFlow<AddExpenseUiAction>
    private lateinit var scope: CoroutineScope

    private var suggestionJob: Job? = null
    private var isCategoryLocked: Boolean = false
    private var hasReachedCategoryStep: Boolean = false

    override fun bind(
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        actionsFlow: MutableSharedFlow<AddExpenseUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actionsFlow = actionsFlow
        this.scope = scope
    }

    fun handleTitleChanged(title: String) {
        suggestionJob?.cancel()
        val state = _uiState.value
        val requestedTitle = title.trim()
        if (!canTriggerSuggestion(state, requestedTitle)) return

        val groupContext = state.groupContext
        suggestionJob = scope.launch {
            delay(debounceMs)
            val suggestion = try {
                suggestExpenseCategoryUseCase(
                    title = requestedTitle,
                    groupContext = groupContext
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "CategorySuggestionEventHandler: background category prediction failed")
                null
            } ?: return@launch

            applySuggestion(suggestion = suggestion, requestedTitle = requestedTitle)
        }
    }

    /**
     * Locks background suggestions after an explicit user category/subcategory selection
     * or receipt AI auto-fill category assignment.
     */
    fun handleManualCategoryOverride() {
        isCategoryLocked = true
        suggestionJob?.cancel()
    }

    /**
     * Prevents late-arriving background predictions from mutating the category grid
     * once the user has reached [AddExpenseStep.CATEGORY].
     */
    fun handleCategoryStepReached() {
        hasReachedCategoryStep = true
        suggestionJob?.cancel()
    }

    /**
     * Resets in-flight work and override flags when loading a different group's config.
     */
    fun reset() {
        suggestionJob?.cancel()
        suggestionJob = null
        isCategoryLocked = false
        hasReachedCategoryStep = false
    }

    private fun canTriggerSuggestion(
        state: AddExpenseUiState,
        requestedTitle: String
    ): Boolean {
        val isEligibleMode = !state.isEditMode && state.isConfigLoaded
        val isUnlocked = !isCategoryLocked && !hasReachedCategoryStep
        return isEligibleMode && isUnlocked && requestedTitle.isNotBlank()
    }

    private fun canApplySuggestion(
        state: AddExpenseUiState,
        requestedTitle: String
    ): Boolean {
        val isEligibleMode = !state.isEditMode && state.currentStep != AddExpenseStep.CATEGORY
        val isUnlocked = !isCategoryLocked && !hasReachedCategoryStep
        return isEligibleMode && isUnlocked && state.expenseTitle.trim() == requestedTitle
    }

    private fun applySuggestion(
        suggestion: CategorySuggestion,
        requestedTitle: String
    ) {
        _uiState.update { state ->
            buildSuggestedState(state, suggestion, requestedTitle) ?: state
        }
    }

    private fun buildSuggestedState(
        state: AddExpenseUiState,
        suggestion: CategorySuggestion,
        requestedTitle: String
    ): AddExpenseUiState? {
        if (!canApplySuggestion(state, requestedTitle)) return null

        val predictedSubcategory = suggestion.subcategory
        val requiresShowAll = !suggestion.category.appliesTo(state.groupContext) ||
            (predictedSubcategory != null && !predictedSubcategory.appliesTo(state.groupContext))
        val nextShowAll = state.showAllCategories || requiresShowAll

        val updatedCategories = addExpenseOptionsUiMapper.mapCategoriesForContext(
            context = state.groupContext,
            showAll = nextShowAll,
            alwaysInclude = suggestion.category
        )
        val selectedCategoryUi = updatedCategories.find { it.id == suggestion.category.name }
            ?: return null

        val updatedSubcategories = addExpenseOptionsUiMapper.mapSubcategories(
            category = suggestion.category,
            context = state.groupContext,
            showAll = nextShowAll,
            alwaysInclude = predictedSubcategory
        )
        val selectedSubcategoryUi = predictedSubcategory?.let { predictedSub ->
            updatedSubcategories.find { it.subcategory == predictedSub }
        }

        return state.copy(
            showAllCategories = nextShowAll,
            availableCategories = updatedCategories,
            selectedCategory = selectedCategoryUi,
            availableSubcategories = updatedSubcategories,
            selectedSubcategory = selectedSubcategoryUi
        )
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MS = 250L
    }
}
