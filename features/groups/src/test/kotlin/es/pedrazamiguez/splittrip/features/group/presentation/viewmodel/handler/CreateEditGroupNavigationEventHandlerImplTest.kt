package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupStep
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateEditGroupNavigationEventHandlerImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var stateFlow: MutableStateFlow<CreateEditGroupUiState>
    private lateinit var actionsFlow: MutableSharedFlow<CreateEditGroupUiAction>
    private lateinit var handler: CreateEditGroupNavigationEventHandlerImpl

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        stateFlow = MutableStateFlow(CreateEditGroupUiState())
        actionsFlow = MutableSharedFlow(replay = 1)
        handler = CreateEditGroupNavigationEventHandlerImpl()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    inner class NextStep {

        @Test
        fun `NextStep advances to next step from INFO`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = CreateEditGroupUiState(currentStep = CreateEditGroupStep.INFO)

            handler.handleNavigation(CreateEditGroupUiEvent.NextStep)
            advanceUntilIdle()

            assertEquals(CreateEditGroupStep.CURRENCY, stateFlow.value.currentStep)
        }

        @Test
        fun `NextStep clears error when navigating`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = CreateEditGroupUiState(
                currentStep = CreateEditGroupStep.INFO,
                error = es.pedrazamiguez.splittrip.core.common.presentation.UiText.DynamicString("Some error")
            )

            handler.handleNavigation(CreateEditGroupUiEvent.NextStep)

            assertEquals(null, stateFlow.value.error)
        }

        @Test
        fun `NextStep is a no-op on last step`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = CreateEditGroupUiState(currentStep = CreateEditGroupStep.REVIEW)

            handler.handleNavigation(CreateEditGroupUiEvent.NextStep)

            assertEquals(CreateEditGroupStep.REVIEW, stateFlow.value.currentStep)
        }
    }

    @Nested
    inner class PreviousStep {

        @Test
        fun `PreviousStep from CURRENCY goes back to INFO`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = CreateEditGroupUiState(currentStep = CreateEditGroupStep.CURRENCY)

            handler.handleNavigation(CreateEditGroupUiEvent.PreviousStep)
            advanceUntilIdle()

            assertEquals(CreateEditGroupStep.INFO, stateFlow.value.currentStep)
        }

        @Test
        fun `PreviousStep from INFO emits NavigateBack action`() = runTest(testDispatcher) {
            val actions = mutableListOf<CreateEditGroupUiAction>()
            val collectJob = launch { actionsFlow.collect { actions.add(it) } }

            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = CreateEditGroupUiState(currentStep = CreateEditGroupStep.INFO)

            handler.handleNavigation(CreateEditGroupUiEvent.PreviousStep)
            advanceUntilIdle()

            assertTrue(actions.any { it is CreateEditGroupUiAction.NavigateBack })
            collectJob.cancel()
        }
    }

    @Nested
    inner class JumpToStep {

        @Test
        fun `JumpToStep navigates to the specified index`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = CreateEditGroupUiState(currentStep = CreateEditGroupStep.REVIEW)

            // Jump to index 0 (INFO)
            handler.handleNavigation(CreateEditGroupUiEvent.JumpToStep(0))

            assertEquals(CreateEditGroupStep.INFO, stateFlow.value.currentStep)
        }

        @Test
        fun `JumpToStep is a no-op for out-of-bounds index`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = CreateEditGroupUiState(currentStep = CreateEditGroupStep.INFO)

            handler.handleNavigation(CreateEditGroupUiEvent.JumpToStep(999))

            assertEquals(CreateEditGroupStep.INFO, stateFlow.value.currentStep)
        }
    }
}
