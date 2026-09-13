package es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel

import es.pedrazamiguez.splittrip.features.onboarding.presentation.model.OnboardingStep
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.action.OnboardingUiAction
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.event.OnboardingUiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: OnboardingViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = OnboardingViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialState_startsAtTripsAndGroupsStep`() = runTest(testDispatcher) {
        val state = viewModel.uiState.value

        assertEquals(OnboardingStep.TRIPS_AND_GROUPS, state.currentStep)
        assertEquals(0, state.currentStepIndex)
        assertEquals(4, state.totalSteps)
        assertTrue(state.isFirstStep)
        assertFalse(state.isLastStep)
    }

    @Test
    fun `nextStepEvent_advancesToNextStepSequentially`() = runTest(testDispatcher) {
        // Step 0 -> Step 1
        viewModel.onEvent(OnboardingUiEvent.NextStep)
        var state = viewModel.uiState.value
        assertEquals(OnboardingStep.SMART_SPLITTING, state.currentStep)
        assertEquals(1, state.currentStepIndex)
        assertFalse(state.isFirstStep)
        assertFalse(state.isLastStep)

        // Step 1 -> Step 2
        viewModel.onEvent(OnboardingUiEvent.NextStep)
        state = viewModel.uiState.value
        assertEquals(OnboardingStep.CASH_AND_POCKET, state.currentStep)
        assertEquals(2, state.currentStepIndex)
        assertFalse(state.isFirstStep)
        assertFalse(state.isLastStep)

        // Step 2 -> Step 3
        viewModel.onEvent(OnboardingUiEvent.NextStep)
        state = viewModel.uiState.value
        assertEquals(OnboardingStep.CONSENSUS_AND_SETTLEMENT, state.currentStep)
        assertEquals(3, state.currentStepIndex)
        assertFalse(state.isFirstStep)
        assertTrue(state.isLastStep)
    }

    @Test
    fun `nextStepEvent_onLastStep_doesNotExceedBounds`() = runTest(testDispatcher) {
        // Advance to last step
        viewModel.onEvent(OnboardingUiEvent.NextStep)
        viewModel.onEvent(OnboardingUiEvent.NextStep)
        viewModel.onEvent(OnboardingUiEvent.NextStep)

        val lastState = viewModel.uiState.value
        assertEquals(OnboardingStep.CONSENSUS_AND_SETTLEMENT, lastState.currentStep)
        assertTrue(lastState.isLastStep)

        // Attempt to advance past the last step
        viewModel.onEvent(OnboardingUiEvent.NextStep)

        val stateAfterExtraNext = viewModel.uiState.value
        assertEquals(OnboardingStep.CONSENSUS_AND_SETTLEMENT, stateAfterExtraNext.currentStep)
        assertEquals(3, stateAfterExtraNext.currentStepIndex)
        assertTrue(stateAfterExtraNext.isLastStep)
    }

    @Test
    fun `previousStepEvent_navigatesBackwardSequentially`() = runTest(testDispatcher) {
        // Advance to step 2 (CASH_AND_POCKET)
        viewModel.onEvent(OnboardingUiEvent.NextStep)
        viewModel.onEvent(OnboardingUiEvent.NextStep)
        assertEquals(OnboardingStep.CASH_AND_POCKET, viewModel.uiState.value.currentStep)

        // Move back to step 1 (SMART_SPLITTING)
        viewModel.onEvent(OnboardingUiEvent.PreviousStep)
        var state = viewModel.uiState.value
        assertEquals(OnboardingStep.SMART_SPLITTING, state.currentStep)
        assertEquals(1, state.currentStepIndex)

        // Move back to step 0 (TRIPS_AND_GROUPS)
        viewModel.onEvent(OnboardingUiEvent.PreviousStep)
        state = viewModel.uiState.value
        assertEquals(OnboardingStep.TRIPS_AND_GROUPS, state.currentStep)
        assertEquals(0, state.currentStepIndex)
        assertTrue(state.isFirstStep)
    }

    @Test
    fun `previousStepEvent_onFirstStep_doesNotGoBelowZero`() = runTest(testDispatcher) {
        // Initially on first step
        assertTrue(viewModel.uiState.value.isFirstStep)

        // Attempt to move back from first step
        viewModel.onEvent(OnboardingUiEvent.PreviousStep)

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.TRIPS_AND_GROUPS, state.currentStep)
        assertEquals(0, state.currentStepIndex)
        assertTrue(state.isFirstStep)
    }

    @Test
    fun `skipEvent_emitsCompleteOnboardingAction`() = runTest(testDispatcher) {
        val emittedActions = mutableListOf<OnboardingUiAction>()
        val collectJob = launch {
            viewModel.actions.collect { emittedActions.add(it) }
        }

        viewModel.onEvent(OnboardingUiEvent.Skip)
        advanceUntilIdle()

        assertEquals(1, emittedActions.size)
        assertTrue(emittedActions.first() is OnboardingUiAction.CompleteOnboarding)

        collectJob.cancel()
    }

    @Test
    fun `completeEvent_emitsCompleteOnboardingAction`() = runTest(testDispatcher) {
        val emittedActions = mutableListOf<OnboardingUiAction>()
        val collectJob = launch {
            viewModel.actions.collect { emittedActions.add(it) }
        }

        viewModel.onEvent(OnboardingUiEvent.Complete)
        advanceUntilIdle()

        assertEquals(1, emittedActions.size)
        assertTrue(emittedActions.first() is OnboardingUiAction.CompleteOnboarding)

        collectJob.cancel()
    }
}
