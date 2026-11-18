package es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.network.NetworkMonitor
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.usecase.balance.DisputeSettlementUseCase
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.action.DisputeSettlementUiAction
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.event.DisputeSettlementUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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
class DisputeSettlementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val disputeSettlementUseCase: DisputeSettlementUseCase = mockk()
    private val networkMonitor: NetworkMonitor = mockk()
    private val isOnlineFlow = MutableStateFlow(true)

    private lateinit var viewModel: DisputeSettlementViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        isOnlineFlow.value = true
        every { networkMonitor.isOnline } returns isOnlineFlow

        viewModel = DisputeSettlementViewModel(
            disputeSettlementUseCase = disputeSettlementUseCase,
            networkMonitor = networkMonitor
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty reason, isSubmitting false, isOffline false, and isSubmitEnabled false`() =
        runTest(testDispatcher) {
            val state = viewModel.uiState.value
            assertEquals("", state.reason)
            assertFalse(state.isSubmitting)
            assertFalse(state.isOffline)
            assertFalse(state.isSubmitEnabled)
        }

    @Test
    fun `ReasonChanged updates reason and enables submit when online and non-blank`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(DisputeSettlementUiEvent.ReasonChanged("Wrong amount"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Wrong amount", state.reason)
        assertTrue(state.isSubmitEnabled)
    }

    @Test
    fun `isOffline updates dynamically from networkMonitor and disables submit`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onEvent(DisputeSettlementUiEvent.ReasonChanged("Wrong amount"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSubmitEnabled)

        isOnlineFlow.value = false
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isOffline)
        assertFalse(viewModel.uiState.value.isSubmitEnabled)

        isOnlineFlow.value = true
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isOffline)
        assertTrue(viewModel.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `SubmitClicked invokes DisputeSettlementUseCase and emits ShowSuccess then NavigateBack on success`() =
        runTest(testDispatcher) {
            backgroundScope.launch { viewModel.uiState.collect {} }
            val record: SettlementRecord = mockk()
            coEvery { disputeSettlementUseCase("group1", "s1", "Wrong amount") } returns Result.success(record)

            viewModel.setContext(groupId = "group1", settlementId = "s1")
            viewModel.onEvent(DisputeSettlementUiEvent.ReasonChanged("  Wrong amount  "))
            advanceUntilIdle()

            val emittedActions = mutableListOf<DisputeSettlementUiAction>()
            val collectJob = launch { viewModel.actions.take(2).toList(emittedActions) }

            viewModel.onEvent(DisputeSettlementUiEvent.SubmitClicked)
            advanceUntilIdle()
            collectJob.join()

            coVerify(exactly = 1) { disputeSettlementUseCase("group1", "s1", "Wrong amount") }
            assertFalse(viewModel.uiState.value.isSubmitting)
            assertEquals(
                listOf(
                    DisputeSettlementUiAction.ShowSuccess(
                        UiText.StringResource(R.string.your_balance_dispute_success)
                    ),
                    DisputeSettlementUiAction.NavigateBack
                ),
                emittedActions
            )
        }

    @Test
    fun `SubmitClicked splits comma-separated settlementIds and invokes DisputeSettlementUseCase for each id`() =
        runTest(testDispatcher) {
            backgroundScope.launch { viewModel.uiState.collect {} }
            val record: SettlementRecord = mockk()
            coEvery { disputeSettlementUseCase("group1", any(), "Already settled") } returns Result.success(record)

            viewModel.setContext(groupId = "group1", settlementId = "s1,s2, ,s3")
            viewModel.onEvent(DisputeSettlementUiEvent.ReasonChanged("Already settled"))
            advanceUntilIdle()

            viewModel.onEvent(DisputeSettlementUiEvent.SubmitClicked)
            advanceUntilIdle()

            coVerify(exactly = 1) { disputeSettlementUseCase("group1", "s1", "Already settled") }
            coVerify(exactly = 1) { disputeSettlementUseCase("group1", "s2", "Already settled") }
            coVerify(exactly = 1) { disputeSettlementUseCase("group1", "s3", "Already settled") }
        }

    @Test
    fun `SubmitClicked emits ShowError and resets isSubmitting when DisputeSettlementUseCase fails`() =
        runTest(testDispatcher) {
            backgroundScope.launch { viewModel.uiState.collect {} }
            coEvery {
                disputeSettlementUseCase("group1", "s1", "Wrong amount")
            } returns Result.failure(RuntimeException("Network error"))

            viewModel.setContext(groupId = "group1", settlementId = "s1")
            viewModel.onEvent(DisputeSettlementUiEvent.ReasonChanged("Wrong amount"))
            advanceUntilIdle()

            viewModel.onEvent(DisputeSettlementUiEvent.SubmitClicked)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSubmitting)
            val action = viewModel.actions.first()
            assertEquals(
                DisputeSettlementUiAction.ShowError(
                    UiText.StringResource(R.string.your_balance_dispute_error)
                ),
                action
            )
        }

    @Test
    fun `SubmitClicked emits offline warning ShowError and does not call use case when offline`() =
        runTest(testDispatcher) {
            backgroundScope.launch { viewModel.uiState.collect {} }
            isOnlineFlow.value = false
            viewModel.setContext(groupId = "group1", settlementId = "s1")
            viewModel.onEvent(DisputeSettlementUiEvent.ReasonChanged("Wrong amount"))
            advanceUntilIdle()

            viewModel.onEvent(DisputeSettlementUiEvent.SubmitClicked)
            advanceUntilIdle()

            coVerify(exactly = 0) { disputeSettlementUseCase(any(), any(), any()) }
            val action = viewModel.actions.first()
            assertEquals(
                DisputeSettlementUiAction.ShowError(
                    UiText.StringResource(R.string.your_balance_offline_warning)
                ),
                action
            )
        }

    @Test
    fun `SubmitClicked does nothing when reason is blank`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setContext(groupId = "group1", settlementId = "s1")
        viewModel.onEvent(DisputeSettlementUiEvent.ReasonChanged("   "))
        advanceUntilIdle()

        viewModel.onEvent(DisputeSettlementUiEvent.SubmitClicked)
        advanceUntilIdle()

        coVerify(exactly = 0) { disputeSettlementUseCase(any(), any(), any()) }
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `NavigateBackClicked emits NavigateBack action`() = runTest(testDispatcher) {
        viewModel.onEvent(DisputeSettlementUiEvent.NavigateBackClicked)
        advanceUntilIdle()

        val action = viewModel.actions.first()
        assertEquals(DisputeSettlementUiAction.NavigateBack, action)
    }
}
