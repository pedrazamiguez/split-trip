package es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.delegate

import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.usecase.balance.ConfirmSettlementUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.DisputeSettlementUseCase
import es.pedrazamiguez.splittrip.domain.usecase.settlement.NudgeDebtorUseCase
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.action.YourBalanceUiAction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YourBalanceActionDelegateTest {

    private val confirmSettlementUseCase: ConfirmSettlementUseCase = mockk()
    private val disputeSettlementUseCase: DisputeSettlementUseCase = mockk()
    private val nudgeDebtorUseCase: NudgeDebtorUseCase = mockk()
    private val telemetryTracker: TelemetryTracker = mockk(relaxed = true)

    private lateinit var delegate: YourBalanceActionDelegate

    @BeforeEach
    fun setUp() {
        delegate = YourBalanceActionDelegate(
            confirmSettlementUseCase,
            disputeSettlementUseCase,
            nudgeDebtorUseCase,
            telemetryTracker
        )
    }

    @Test
    fun `handleConfirm calls use case and emits success`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)
        coEvery { confirmSettlementUseCase("group1", "s1") } returns Result.success(mockk())

        delegate.handleConfirm("s1", "group1", isOffline = false, actions)

        coVerify(exactly = 1) { confirmSettlementUseCase("group1", "s1") }
        coVerify(exactly = 1) {
            telemetryTracker.trackEvent(
                "settlement_confirmed",
                mapOf("group_id" to "group1", "settlement_id" to "s1")
            )
        }
        val action = actions.receive()
        assertTrue(action is YourBalanceUiAction.ShowSuccess)
    }

    @Test
    fun `handleConfirm emits error when offline`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)
        delegate.handleConfirm("s1", "group1", isOffline = true, actions)

        val action = actions.receive()
        assertTrue(action is YourBalanceUiAction.ShowError)
        coVerify(exactly = 0) { confirmSettlementUseCase(any(), any()) }
    }

    @Test
    fun `handleConfirm emits error on failure`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)
        coEvery { confirmSettlementUseCase("group1", "s1") } returns Result.failure(RuntimeException("Error"))

        delegate.handleConfirm("s1", "group1", isOffline = false, actions)

        coVerify(exactly = 1) { confirmSettlementUseCase("group1", "s1") }
        val action = actions.receive()
        assertTrue(action is YourBalanceUiAction.ShowError)
    }

    @Test
    fun `handleOpenDispute sets active dispute settlement id`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)
        delegate.handleOpenDispute("s1", isOffline = false, actions)

        assertEquals("s1", delegate.localState.value.activeDisputeSettlementId)
    }

    @Test
    fun `updateDisputeReason updates state`() = runTest {
        delegate.updateDisputeReason("Wrong amount")
        assertEquals("Wrong amount", delegate.localState.value.disputeReasonInput)
    }

    @Test
    fun `handleCancelDispute clears state`() = runTest {
        delegate.updateDisputeReason("Wrong amount")
        delegate.handleCancelDispute()
        assertNull(delegate.localState.value.activeDisputeSettlementId)
        assertEquals("", delegate.localState.value.disputeReasonInput)
    }

    @Test
    fun `handleSubmitDispute calls use case and clears state on success`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)
        coEvery { disputeSettlementUseCase("group1", "s1", "Wrong amount") } returns Result.success(mockk())

        delegate.handleOpenDispute("s1", isOffline = false, actions)
        delegate.updateDisputeReason("Wrong amount")
        delegate.handleSubmitDispute("group1", isOffline = false, actions)

        coVerify(exactly = 1) { disputeSettlementUseCase("group1", "s1", "Wrong amount") }
        val action = actions.receive()
        assertTrue(action is YourBalanceUiAction.ShowSuccess)
        assertNull(delegate.localState.value.activeDisputeSettlementId)
        assertEquals("", delegate.localState.value.disputeReasonInput)
    }

    @Test
    fun `handleSubmitDispute emits error on failure`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)
        coEvery { disputeSettlementUseCase("group1", "s1", "Wrong amount") } returns Result.failure(RuntimeException())

        delegate.handleOpenDispute("s1", isOffline = false, actions)
        delegate.updateDisputeReason("Wrong amount")
        delegate.handleSubmitDispute("group1", isOffline = false, actions)

        coVerify(exactly = 1) { disputeSettlementUseCase("group1", "s1", "Wrong amount") }
        val action = actions.receive()
        assertTrue(action is YourBalanceUiAction.ShowError)
    }

    @Test
    fun `handleSubmitDispute does nothing if reason is blank`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)
        delegate.handleOpenDispute("s1", isOffline = false, actions)
        delegate.updateDisputeReason("  ")
        delegate.handleSubmitDispute("group1", isOffline = false, actions)

        coVerify(exactly = 0) { disputeSettlementUseCase(any(), any(), any()) }
    }

    @Test
    fun `handleNudgeDebtor calls use case and emits success`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)
        coEvery { nudgeDebtorUseCase("group1", "s1") } returns Result.success(Unit)

        delegate.handleNudgeDebtor("s1", "group1", isOffline = false, actions)

        coVerify(exactly = 1) { nudgeDebtorUseCase("group1", "s1") }
        val action = actions.receive()
        assertTrue(action is YourBalanceUiAction.ShowSuccess)
    }

    @Test
    fun `handleNudgeDebtor emits error on failure`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)
        coEvery { nudgeDebtorUseCase("group1", "s1") } returns Result.failure(RuntimeException())

        delegate.handleNudgeDebtor("s1", "group1", isOffline = false, actions)

        coVerify(exactly = 1) { nudgeDebtorUseCase("group1", "s1") }
        val action = actions.receive()
        assertTrue(action is YourBalanceUiAction.ShowError)
    }

    @Test
    fun `consensus actions are blocked when offline`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)

        delegate.handleConfirm("s1", "group1", isOffline = true, actions)
        assertTrue(actions.receive() is YourBalanceUiAction.ShowError)

        delegate.handleNudgeDebtor("s1", "group1", isOffline = true, actions)
        assertTrue(actions.receive() is YourBalanceUiAction.ShowError)

        delegate.handleOpenDispute("s1", isOffline = true, actions)
        assertTrue(actions.receive() is YourBalanceUiAction.ShowError)

        delegate.handleSubmitDispute("group1", isOffline = true, actions)
        assertTrue(actions.receive() is YourBalanceUiAction.ShowError)

        coVerify(exactly = 0) { confirmSettlementUseCase(any(), any()) }
        coVerify(exactly = 0) { nudgeDebtorUseCase(any(), any()) }
        coVerify(exactly = 0) { disputeSettlementUseCase(any(), any(), any()) }
    }
}
