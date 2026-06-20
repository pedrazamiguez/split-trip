package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateGroupSubmitEventHandlerImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var createGroupUseCase: CreateGroupUseCase
    private lateinit var telemetryTracker: TelemetryTracker
    private lateinit var handler: CreateGroupSubmitEventHandlerImpl
    private lateinit var stateFlow: MutableStateFlow<CreateGroupUiState>
    private lateinit var actionsFlow: MutableSharedFlow<CreateGroupUiAction>

    @BeforeEach
    fun setUp() {
        createGroupUseCase = mockk(relaxed = true)
        telemetryTracker = mockk(relaxed = true)
        handler = CreateGroupSubmitEventHandlerImpl(createGroupUseCase, telemetryTracker)
        stateFlow = MutableStateFlow(CreateGroupUiState())
        actionsFlow = MutableSharedFlow()
    }

    @Test
    fun `handleSubmit with blank name sets error and does not create group`() = runTest(testDispatcher) {
        // Given
        handler.bind(stateFlow, actionsFlow, this)
        stateFlow.value = stateFlow.value.copy(groupName = "   ")
        var callbackCalled = false

        // When
        handler.handleSubmit { callbackCalled = true }
        advanceUntilIdle()

        // Then
        assertFalse(stateFlow.value.isNameValid)
        assertNotNull(stateFlow.value.error)
        assertFalse(callbackCalled)
        coVerify(exactly = 0) { createGroupUseCase(any(), any()) }
    }

    @Test
    fun `handleSubmit success creates group, tracks telemetry and triggers callback`() = runTest(testDispatcher) {
        // Given
        handler.bind(stateFlow, actionsFlow, this)
        stateFlow.value = stateFlow.value.copy(groupName = "My Group", groupDescription = "Description")
        val groupSlot = slot<Group>()
        coEvery { createGroupUseCase(capture(groupSlot), any()) } returns Result.success("group-123")

        val actions = mutableListOf<CreateGroupUiAction>()
        val collectJob = launch {
            actionsFlow.collect { actions.add(it) }
        }
        var callbackCalled = false

        // When
        handler.handleSubmit { callbackCalled = true }
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { createGroupUseCase(any(), any()) }
        coVerify(exactly = 1) { telemetryTracker.trackEvent("group_created", any()) }
        assertTrue(callbackCalled)
        assertFalse(stateFlow.value.isLoading)
        assertNull(stateFlow.value.error)

        assertEquals("My Group", groupSlot.captured.name)
        assertEquals("Description", groupSlot.captured.description)

        assertEquals(1, actions.size)
        assertTrue(actions[0] is CreateGroupUiAction.ShowSuccess)

        collectJob.cancel()
    }

    @Test
    fun `handleSubmit failure sets error and emits error action`() = runTest(testDispatcher) {
        // Given
        handler.bind(stateFlow, actionsFlow, this)
        stateFlow.value = stateFlow.value.copy(groupName = "My Group")
        coEvery { createGroupUseCase(any(), any()) } returns Result.failure(RuntimeException("Create failed"))

        val actions = mutableListOf<CreateGroupUiAction>()
        val collectJob = launch {
            actionsFlow.collect { actions.add(it) }
        }
        var callbackCalled = false

        // When
        handler.handleSubmit { callbackCalled = true }
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { createGroupUseCase(any(), any()) }
        assertFalse(callbackCalled)
        assertFalse(stateFlow.value.isLoading)
        assertNotNull(stateFlow.value.error)

        assertEquals(1, actions.size)
        assertTrue(actions[0] is CreateGroupUiAction.ShowError)

        collectJob.cancel()
    }
}
