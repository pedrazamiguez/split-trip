package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateGroupNavigationHandlerImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var handler: CreateGroupNavigationHandlerImpl
    private lateinit var stateFlow: MutableStateFlow<CreateGroupUiState>
    private lateinit var actionsFlow: MutableSharedFlow<CreateGroupUiAction>

    @BeforeEach
    fun setUp() {
        handler = CreateGroupNavigationHandlerImpl()
        stateFlow = MutableStateFlow(CreateGroupUiState())
        actionsFlow = MutableSharedFlow()
    }

    @Test
    fun `NextStep advances current step`() = runTest(testDispatcher) {
        // Given
        handler.bind(stateFlow, actionsFlow, this)
        val initialStep = stateFlow.value.currentStep

        // When
        handler.handleNavigation(CreateGroupUiEvent.NextStep)

        // Then
        val newStep = stateFlow.value.currentStep
        assertTrue(newStep != initialStep)
        assertNull(stateFlow.value.error)
    }

    @Test
    fun `PreviousStep on first step emits NavigateBack action`() = runTest(testDispatcher) {
        // Given
        handler.bind(stateFlow, actionsFlow, this)
        val actions = mutableListOf<CreateGroupUiAction>()
        val collectJob = launch {
            actionsFlow.collect { actions.add(it) }
        }

        // When
        handler.handleNavigation(CreateGroupUiEvent.PreviousStep)
        advanceUntilIdle()

        // Then
        assertTrue(actions.any { it is CreateGroupUiAction.NavigateBack })
        collectJob.cancel()
    }

    @Test
    fun `JumpToStep navigates to step by index`() = runTest(testDispatcher) {
        // Given
        handler.bind(stateFlow, actionsFlow, this)
        // Move to step index 1
        handler.handleNavigation(CreateGroupUiEvent.NextStep)
        val targetStep = stateFlow.value.currentStep

        // Move to step index 2
        handler.handleNavigation(CreateGroupUiEvent.NextStep)

        // When
        handler.handleNavigation(CreateGroupUiEvent.JumpToStep(1))

        // Then
        assertEquals(targetStep, stateFlow.value.currentStep)
    }
}
