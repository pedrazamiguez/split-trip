package es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.network.NetworkMonitor
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupSettlementsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetSettlementSuggestionsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.settlement.GetNudgeTimestampsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.MemberSpendingChartUiMapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.SettlementConsensusUiMapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.YourBalanceUiMapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.MemberSpendingChartUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.delegate.LocalUiState
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.delegate.YourBalanceActionDelegate
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.event.YourBalanceUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YourBalanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getGroupByIdUseCase: GetGroupByIdUseCase = mockk()
    private val getGroupContributionsFlowUseCase: GetGroupContributionsFlowUseCase = mockk()
    private val getCashWithdrawalsFlowUseCase: GetCashWithdrawalsFlowUseCase = mockk()
    private val getGroupExpensesFlowUseCase: GetGroupExpensesFlowUseCase = mockk()
    private val getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase = mockk()
    private val getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase = mockk()
    private val getGroupSettlementsFlowUseCase: GetGroupSettlementsFlowUseCase = mockk()
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase = mockk()
    private val getSettlementSuggestionsUseCase: GetSettlementSuggestionsUseCase = mockk()
    private val getNudgeTimestampsFlowUseCase: GetNudgeTimestampsFlowUseCase = mockk()
    private val actionDelegate: YourBalanceActionDelegate = mockk()

    private val authenticationService: AuthenticationService = mockk()
    private val appConfigService: AppConfigService = mockk()
    private val networkMonitor: NetworkMonitor = mockk()
    private val isOnlineFlow = MutableStateFlow(true)
    private val localeProvider: LocaleProvider = mockk()
    private val resourceProvider: ResourceProvider = mockk()
    private val settlementConsensusUiMapper: SettlementConsensusUiMapper = mockk()
    private val memberSpendingChartUiMapper: MemberSpendingChartUiMapper = mockk()

    private lateinit var useCases: YourBalanceUseCases
    private lateinit var mapper: YourBalanceUiMapper
    private lateinit var viewModel: YourBalanceViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        isOnlineFlow.value = true
        every { networkMonitor.isOnline } returns isOnlineFlow
        every { localeProvider.getCurrentLocale() } returns Locale.US
        every { resourceProvider.getString(any()) } returns "Label"
        every { resourceProvider.getString(any(), *anyVararg()) } returns "Formatted Label"
        every { appConfigService.defaultCurrencyCode } returns MutableStateFlow("EUR")
        every { appConfigService.balanceComputationDebounceMs } returns MutableStateFlow(0L)
        every { appConfigService.settlementNudgeRateLimitHours } returns MutableStateFlow(24L)
        every { authenticationService.currentUserId() } returns "user1"
        coEvery { getSettlementSuggestionsUseCase.persistForGroup(any(), any()) } returns emptyList()
        every {
            getSettlementSuggestionsUseCase.getEphemeralSettlements(any(), any(), any(), any())
        } returns emptyList()
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()
        every { getNudgeTimestampsFlowUseCase() } returns flowOf(emptyMap())
        every { settlementConsensusUiMapper.toConsensusItems(any(), any(), any(), any(), any(), any(), any()) } returns
            persistentListOf()
        every { memberSpendingChartUiMapper.toChartUiModel(any(), any(), any(), any(), any()) } returns
            MemberSpendingChartUiModel(
                bars = persistentListOf(),
                formattedGroupTotal = "Total",
                isCashOnly = true,
                hasCashExpenses = true
            )
        every { actionDelegate.localState } returns MutableStateFlow(LocalUiState())

        mapper = YourBalanceUiMapper(localeProvider, resourceProvider)

        useCases = YourBalanceUseCases(
            getGroupByIdUseCase = getGroupByIdUseCase,
            getGroupContributionsFlowUseCase = getGroupContributionsFlowUseCase,
            getCashWithdrawalsFlowUseCase = getCashWithdrawalsFlowUseCase,
            getGroupExpensesFlowUseCase = getGroupExpensesFlowUseCase,
            getGroupSubunitsFlowUseCase = getGroupSubunitsFlowUseCase,
            getMemberBalancesFlowUseCase = getMemberBalancesFlowUseCase,
            getGroupSettlementsFlowUseCase = getGroupSettlementsFlowUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            getSettlementSuggestionsUseCase = getSettlementSuggestionsUseCase,
            getNudgeTimestampsFlowUseCase = getNudgeTimestampsFlowUseCase
        )

        viewModel = YourBalanceViewModel(
            useCases = useCases,
            actionDelegate = actionDelegate,
            authenticationService = authenticationService,
            yourBalanceUiMapper = mapper,
            settlementConsensusUiMapper = settlementConsensusUiMapper,
            memberSpendingChartUiMapper = memberSpendingChartUiMapper,
            appConfigService = appConfigService,
            networkMonitor = networkMonitor,
            computationDispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest(testDispatcher) {
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.personalPosition)
        assertFalse(viewModel.uiState.value.isCashBreakdownVisible)
    }

    @Test
    fun `setSelectedGroup loads position for current user`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val group = Group(id = "group1", name = "Trip", currency = "EUR", members = listOf("user1"))
        coEvery { getGroupByIdUseCase("group1") } returns group
        every { getGroupContributionsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getCashWithdrawalsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupExpensesFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupSubunitsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupSettlementsFlowUseCase("group1") } returns flowOf(emptyList())

        val calculatedBalances = listOf(
            MemberBalance(
                userId = "user1",
                pocketBalance = 30000L,
                cashInHand = 20000L
            )
        )
        every { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns calculatedBalances

        viewModel.setSelectedGroup("group1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isOffline)
        assertNotNull(state.personalPosition)
        assertEquals("€500.00", state.personalPosition?.formattedNetPosition)
        coVerify(exactly = 0) { getSettlementSuggestionsUseCase.persistForGroup(any(), any()) }
        verify(atLeast = 1) { getSettlementSuggestionsUseCase.getEphemeralSettlements("group1", any(), any(), any()) }
    }

    @Test
    fun `isOffline is updated dynamically based on networkMonitor flow`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val group = Group(id = "group1", name = "Trip", currency = "EUR", members = listOf("user1"))
        coEvery { getGroupByIdUseCase("group1") } returns group
        every { getGroupContributionsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getCashWithdrawalsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupExpensesFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupSubunitsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupSettlementsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns emptyList()

        viewModel.setSelectedGroup("group1")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isOffline)

        isOnlineFlow.value = false
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isOffline)

        isOnlineFlow.value = true
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isOffline)
    }

    @Test
    fun `cash breakdown visibility toggles on event`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val group = Group(id = "group1", name = "Trip", currency = "EUR", members = listOf("user1"))
        coEvery { getGroupByIdUseCase("group1") } returns group
        every { getGroupContributionsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getCashWithdrawalsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupExpensesFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupSubunitsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupSettlementsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns emptyList()

        viewModel.setSelectedGroup("group1")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCashBreakdownVisible)

        viewModel.onEvent(YourBalanceUiEvent.ShowCashBreakdown)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isCashBreakdownVisible)

        viewModel.onEvent(YourBalanceUiEvent.DismissCashBreakdown)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isCashBreakdownVisible)
    }

    @Test
    fun `ChartModeToggled event updates isChartCashOnly state`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val group = Group(id = "group1", name = "Trip", currency = "EUR", members = listOf("user1"))
        coEvery { getGroupByIdUseCase("group1") } returns group
        every { getGroupContributionsFlowUseCase("group1") } returns flowOf(emptyList())
        val mockWithdrawal = CashWithdrawal(
            id = "w1",
            groupId = "group1",
            withdrawnBy = "user1",
            amountWithdrawn = 1000L,
            currency = "EUR"
        )
        every { getCashWithdrawalsFlowUseCase("group1") } returns flowOf(listOf(mockWithdrawal))
        every { getGroupExpensesFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupSubunitsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupSettlementsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns emptyList()

        viewModel.setSelectedGroup("group1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isChartCashOnly)

        viewModel.onEvent(YourBalanceUiEvent.ChartModeToggled(false))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isChartCashOnly)
    }

    @Test
    fun `initial state of isChartCashOnly is false when no withdrawals exist`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val group = Group(id = "group1", name = "Trip", currency = "EUR", members = listOf("user1"))
        coEvery { getGroupByIdUseCase("group1") } returns group
        every { getGroupContributionsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getCashWithdrawalsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupExpensesFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupSubunitsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getGroupSettlementsFlowUseCase("group1") } returns flowOf(emptyList())
        every { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns emptyList()

        viewModel.setSelectedGroup("group1")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isChartCashOnly)
    }

    @Test
    fun `ConfirmSettlement delegates to actionDelegate`() = runTest(testDispatcher) {
        coEvery { actionDelegate.handleConfirm(any(), any(), any(), any()) } returns Unit

        viewModel.onEvent(YourBalanceUiEvent.ConfirmSettlement("s1"))
        advanceUntilIdle()

        coVerify(exactly = 1) { actionDelegate.handleConfirm("s1", null, false, any()) }
    }

    @Test
    fun `DisputeSettlement delegates to actionDelegate`() = runTest(testDispatcher) {
        coEvery { actionDelegate.handleOpenDispute(any(), any(), any()) } returns Unit

        viewModel.onEvent(YourBalanceUiEvent.DisputeSettlement("s1"))
        advanceUntilIdle()

        coVerify(exactly = 1) { actionDelegate.handleOpenDispute("s1", false, any()) }
    }

    @Test
    fun `DisputeReasonChanged delegates to actionDelegate`() = runTest(testDispatcher) {
        every { actionDelegate.updateDisputeReason(any()) } returns Unit

        viewModel.onEvent(YourBalanceUiEvent.DisputeReasonChanged("reason"))
        advanceUntilIdle()

        coVerify(exactly = 1) { actionDelegate.updateDisputeReason("reason") }
    }

    @Test
    fun `DisputeSubmitted delegates to actionDelegate`() = runTest(testDispatcher) {
        coEvery { actionDelegate.handleSubmitDispute(any(), any(), any()) } returns Unit

        viewModel.onEvent(YourBalanceUiEvent.DisputeSubmitted)
        advanceUntilIdle()

        coVerify(exactly = 1) { actionDelegate.handleSubmitDispute(null, false, any()) }
    }

    @Test
    fun `DisputeCancelled delegates to actionDelegate`() = runTest(testDispatcher) {
        every { actionDelegate.handleCancelDispute() } returns Unit

        viewModel.onEvent(YourBalanceUiEvent.DisputeCancelled)
        advanceUntilIdle()

        coVerify(exactly = 1) { actionDelegate.handleCancelDispute() }
    }

    @Test
    fun `NudgeDebtor delegates to actionDelegate`() = runTest(testDispatcher) {
        coEvery { actionDelegate.handleNudgeDebtor(any(), any(), any(), any()) } returns Unit

        viewModel.onEvent(YourBalanceUiEvent.NudgeDebtor("s1"))
        advanceUntilIdle()

        coVerify(exactly = 1) { actionDelegate.handleNudgeDebtor("s1", null, false, any()) }
    }
}
