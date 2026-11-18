package es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.delegate

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.usecase.balance.ConfirmSettlementUseCase
import es.pedrazamiguez.splittrip.domain.usecase.settlement.NudgeDebtorUseCase
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.action.YourBalanceUiAction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YourBalanceActionDelegateTest {

    private val confirmSettlementUseCase: ConfirmSettlementUseCase = mockk()
    private val nudgeDebtorUseCase: NudgeDebtorUseCase = mockk()
    private val telemetryTracker: TelemetryTracker = mockk(relaxed = true)

    private lateinit var delegate: YourBalanceActionDelegate

    @BeforeEach
    fun setUp() {
        delegate = YourBalanceActionDelegate(
            confirmSettlementUseCase,
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
    fun `handleOpenDispute emits NavigateToDisputeSettlement when online and groupId is present`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)

        delegate.handleOpenDispute("s1", "group1", isOffline = false, actions)

        val action = actions.receive()
        assertEquals(
            YourBalanceUiAction.NavigateToDisputeSettlement(groupId = "group1", settlementId = "s1"),
            action
        )
    }

    @Test
    fun `handleOpenDispute does nothing when groupId is null`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)

        delegate.handleOpenDispute("s1", null, isOffline = false, actions)

        assertTrue(actions.isEmpty)
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
    fun `handleNudgeDebtor emits error when nudgeDebtorUseCase fails for ephemeral settlement`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)
        val ephemeralId = "ephemeral|debtor|creditor|POCKET|EUR|1000"
        coEvery {
            nudgeDebtorUseCase("group1", ephemeralId)
        } returns Result.failure(IllegalArgumentException("Ephemeral settlements cannot be nudged"))

        delegate.handleNudgeDebtor(ephemeralId, "group1", isOffline = false, actions)

        coVerify(exactly = 1) { nudgeDebtorUseCase("group1", ephemeralId) }
        val action = actions.receive()
        assertTrue(action is YourBalanceUiAction.ShowError)
        assertEquals(
            YourBalanceUiAction.ShowError(UiText.StringResource(R.string.your_balance_nudge_error)),
            action
        )
    }

    @Test
    fun `consensus actions are blocked when offline`() = runTest {
        val actions = Channel<YourBalanceUiAction>(Channel.BUFFERED)

        delegate.handleConfirm("s1", "group1", isOffline = true, actions)
        assertTrue(actions.receive() is YourBalanceUiAction.ShowError)

        delegate.handleNudgeDebtor("s1", "group1", isOffline = true, actions)
        assertTrue(actions.receive() is YourBalanceUiAction.ShowError)

        delegate.handleOpenDispute("s1", "group1", isOffline = true, actions)
        assertTrue(actions.receive() is YourBalanceUiAction.ShowError)

        coVerify(exactly = 0) { confirmSettlementUseCase(any(), any()) }
        coVerify(exactly = 0) { nudgeDebtorUseCase(any(), any()) }
    }
}
