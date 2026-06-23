package es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.action.ReconciliationUiAction
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.event.ReconciliationUiEvent
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.state.ReconciliationUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReconciliationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var reconcileUnregisteredUserUseCase: ReconcileUnregisteredUserUseCase
    private lateinit var authenticationService: AuthenticationService
    private lateinit var viewModel: ReconciliationViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        reconcileUnregisteredUserUseCase = mockk()
        authenticationService = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = ReconciliationViewModel(
            reconcileUnregisteredUserUseCase = reconcileUnregisteredUserUseCase,
            authenticationService = authenticationService
        )
    }

    @Nested
    inner class Initialization {

        @Test
        fun `starts in WaitingForYou state`() = runTest(testDispatcher) {
            // When
            createViewModel()

            // Then
            assertEquals(ReconciliationUiState.WaitingForYou, viewModel.uiState.value)
        }
    }

    @Nested
    inner class MigrateData {

        @Test
        fun `transitions to Migrating and success when migrate event is triggered`() = runTest(testDispatcher) {
            // Given
            val email = "pending@example.com"
            val currentUserId = "active-user-123"
            every { authenticationService.currentUserEmail() } returns email
            every { authenticationService.currentUserId() } returns currentUserId
            coEvery { reconcileUnregisteredUserUseCase(email, currentUserId) } returns Result.success(Unit)

            createViewModel()

            // Start collecting actions
            val emittedActions = mutableListOf<ReconciliationUiAction>()
            val collectJob = launch {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When
            viewModel.onEvent(ReconciliationUiEvent.MigrateData)

            // Then - immediately should show migrating
            assertEquals(ReconciliationUiState.Migrating, viewModel.uiState.value)

            // Advance coroutines
            advanceUntilIdle()

            // Then - should end in Success
            assertEquals(ReconciliationUiState.Success, viewModel.uiState.value)
            assertEquals(1, emittedActions.size)
            assertTrue(emittedActions.first() is ReconciliationUiAction.NavigationToNext)

            collectJob.cancel()
        }

        @Test
        fun `returns to WaitingForYou and emits ShowError on migration failure`() = runTest(testDispatcher) {
            // Given
            val email = "pending@example.com"
            val currentUserId = "active-user-123"
            every { authenticationService.currentUserEmail() } returns email
            every { authenticationService.currentUserId() } returns currentUserId
            coEvery { reconcileUnregisteredUserUseCase(email, currentUserId) } returns
                Result.failure(RuntimeException("Migration error"))

            createViewModel()

            // Start collecting actions
            val emittedActions = mutableListOf<ReconciliationUiAction>()
            val collectJob = launch {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When
            viewModel.onEvent(ReconciliationUiEvent.MigrateData)
            advanceUntilIdle()

            // Then
            assertEquals(ReconciliationUiState.WaitingForYou, viewModel.uiState.value)
            assertEquals(1, emittedActions.size)
            assertTrue(emittedActions.first() is ReconciliationUiAction.ShowError)

            collectJob.cancel()
        }

        @Test
        fun `emits ShowError when user session is invalid`() = runTest(testDispatcher) {
            // Given
            every { authenticationService.currentUserEmail() } returns null
            every { authenticationService.currentUserId() } returns null

            createViewModel()

            // Start collecting actions
            val emittedActions = mutableListOf<ReconciliationUiAction>()
            val collectJob = launch {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When
            viewModel.onEvent(ReconciliationUiEvent.MigrateData)
            advanceUntilIdle()

            // Then
            assertEquals(ReconciliationUiState.WaitingForYou, viewModel.uiState.value)
            assertEquals(1, emittedActions.size)
            assertTrue(emittedActions.first() is ReconciliationUiAction.ShowError)

            collectJob.cancel()
        }
    }
}
