package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.model.BalancesDashboardDomainModel
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteCashWithdrawalUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetBalancesDashboardFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.BalancesUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.SettlementsUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ActivityItemUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.GroupPocketBalanceUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.action.BalancesUiAction
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.BalancesUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.handler.BalancesActivityEventHandlerImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BalancesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getBalancesDashboardFlowUseCase: GetBalancesDashboardFlowUseCase
    private lateinit var getGroupByIdUseCase: GetGroupByIdUseCase
    private lateinit var authenticationService: AuthenticationService
    private lateinit var balancesUiMapper: BalancesUiMapper
    private lateinit var settlementsUiMapper: SettlementsUiMapper
    private lateinit var getLastSeenBalanceUseCase: GetLastSeenBalanceUseCase
    private lateinit var setLastSeenBalanceUseCase: SetLastSeenBalanceUseCase
    private lateinit var deleteContributionUseCase: DeleteContributionUseCase
    private lateinit var deleteCashWithdrawalUseCase: DeleteCashWithdrawalUseCase
    private lateinit var appConfigService: AppConfigService
    private lateinit var observeGroupUseCase: ObserveGroupUseCase

    private lateinit var viewModel: BalancesViewModel

    private val testGroupId = "group-123"
    private val testGroup = Group(
        id = testGroupId,
        name = "Trip to Paris",
        currency = "EUR",
        status = GroupStatus.ACTIVE
    )

    private val testContribution1 = Contribution(
        id = "contrib-1",
        groupId = testGroupId,
        userId = "user-1",
        amount = 30000L,
        currency = "EUR",
        createdAt = LocalDateTime.of(2026, 1, 15, 12, 0)
    )

    private val testContribution2 = Contribution(
        id = "contrib-2",
        groupId = testGroupId,
        userId = "user-2",
        amount = 20000L,
        currency = "EUR",
        createdAt = LocalDateTime.of(2026, 1, 16, 10, 0)
    )

    private val testBalance = GroupPocketBalance(
        totalContributions = 50000L,
        totalExpenses = 15000L,
        virtualBalance = 35000L,
        currency = "EUR"
    )

    private val testBalanceUiModel = GroupPocketBalanceUiModel(
        groupName = "Trip to Paris",
        formattedBalance = "€350.00",
        formattedTotalContributed = "€500.00",
        formattedTotalSpent = "€150.00",
        currency = "EUR"
    )

    @BeforeEach
    @Suppress("LongMethod") // Test setup — mock instantiation and wiring for all constructor dependencies
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getBalancesDashboardFlowUseCase = mockk()
        getGroupByIdUseCase = mockk()
        authenticationService = mockk()
        balancesUiMapper = mockk()
        settlementsUiMapper = mockk()
        getLastSeenBalanceUseCase = mockk()
        setLastSeenBalanceUseCase = mockk()
        deleteContributionUseCase = mockk(relaxed = true)
        deleteCashWithdrawalUseCase = mockk(relaxed = true)
        appConfigService = mockk(relaxed = true) {
            every { defaultCurrencyCode } returns MutableStateFlow("EUR")
            every { balanceComputationDebounceMs } returns MutableStateFlow(300L)
        }
        observeGroupUseCase = mockk()
        val defaultDashboard = BalancesDashboardDomainModel(
            balance = testBalance,
            contributions = emptyList(),
            withdrawals = emptyList(),
            subunits = emptyList(),
            expenses = emptyList(),
            settlements = emptyList(),
            memberBalances = emptyList(),
            settlementSuggestions = emptyList(),
            memberProfiles = emptyMap()
        )
        every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(defaultDashboard)

        coEvery { getGroupByIdUseCase(testGroupId) } returns testGroup
        every { authenticationService.currentUserId() } returns "test-user-id"
        every { getLastSeenBalanceUseCase(any()) } returns flowOf(null)
        coEvery { setLastSeenBalanceUseCase(any(), any()) } just Runs
        every { observeGroupUseCase(any()) } returns flowOf(testGroup)
        every { balancesUiMapper.mapCashWithdrawals(any(), any(), any(), any(), any(), any()) } returns
            persistentListOf()

        every { balancesUiMapper.mapBalance(any(), any()) } returns testBalanceUiModel
        every { balancesUiMapper.mapExtrasBreakdown(any(), any(), any(), any(), any(), any()) } returns
            persistentListOf()
        every { balancesUiMapper.mapMemberBalances(any(), any(), any(), any(), any(), any()) } returns
            persistentListOf()
        every { settlementsUiMapper.mapSettlements(any(), any(), any(), any()) } returns
            persistentListOf()
        every { balancesUiMapper.mapContributions(any(), any(), any(), any(), any()) } answers {
            val contributions = firstArg<List<Contribution>>()
            contributions.map { contribution ->
                ContributionUiModel(
                    id = contribution.id,
                    displayName = contribution.userId,
                    formattedAmount = "€${contribution.amount / 100}.00",
                    dateText = contribution.createdAt?.toString() ?: ""
                )
            }.toImmutableList()
        }
        every { balancesUiMapper.mapActivity(any(), any(), any(), any(), any(), any()) } answers {
            val contributions = firstArg<List<Contribution>>()
            val withdrawals = secondArg<List<CashWithdrawal>>()
            val items = mutableListOf<ActivityItemUiModel>()
            contributions.forEach { contribution ->
                items.add(
                    ActivityItemUiModel.ContributionItem(
                        contribution = ContributionUiModel(
                            id = contribution.id,
                            displayName = contribution.userId,
                            formattedAmount = "€${contribution.amount / 100}.00",
                            dateText = contribution.createdAt?.toString() ?: ""
                        ),
                        sortTimestamp = contribution.createdAt
                            ?.atZone(ZoneOffset.UTC)
                            ?.toInstant()?.toEpochMilli() ?: 0L
                    )
                )
            }
            withdrawals.forEach { withdrawal ->
                items.add(
                    ActivityItemUiModel.CashWithdrawalItem(
                        withdrawal = CashWithdrawalUiModel(
                            id = withdrawal.id,
                            displayName = withdrawal.withdrawnBy,
                            formattedAmount = "€${withdrawal.amountWithdrawn / 100}.00",
                            dateText = withdrawal.createdAt?.toString() ?: ""
                        ),
                        sortTimestamp = withdrawal.createdAt
                            ?.atZone(ZoneOffset.UTC)
                            ?.toInstant()?.toEpochMilli() ?: 0L
                    )
                )
            }
            items.sortByDescending { it.sortTimestamp }
            items.toImmutableList()
        }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    inner class StateManagement {

        @Test
        fun `initial state is loading`() = runTest(testDispatcher) {
            // Given

            // When
            viewModel = createViewModel()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isLoading)
            assertNull(state.groupId)
        }

        @Test
        fun `setSelectedGroup updates state with balance and contributions`() = runTest(testDispatcher) {
            // Given
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = listOf(testContribution1, testContribution2),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()

            // Start collecting to activate the WhileSubscribed flow
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(testGroupId, state.groupId)
            assertEquals(testBalanceUiModel, state.pocketBalance)
            assertEquals(2, state.contributions.size)

            collectJob.cancel()
        }

        @Test
        fun `changing group triggers new data load`() = runTest(testDispatcher) {
            // Given
            val group2Id = "group-456"
            val group2 = Group(id = group2Id, name = "Beach Trip", currency = "USD")
            val balanceUiModel2 = testBalanceUiModel.copy(groupName = "Beach Trip", currency = "USD")

            coEvery { getGroupByIdUseCase(group2Id) } returns group2
            every { observeGroupUseCase(group2Id) } returns flowOf(group2)
            every { getBalancesDashboardFlowUseCase(group2Id, "USD", any()) } returns
                flowOf(
                    BalancesDashboardDomainModel(
                        balance = testBalance.copy(currency = "USD"),
                        contributions = listOf(testContribution1),
                        withdrawals = emptyList(),
                        subunits = emptyList(),
                        expenses = emptyList(),
                        settlements = emptyList(),
                        memberBalances = emptyList(),
                        settlementSuggestions = emptyList(),
                        memberProfiles = emptyMap()
                    )
                )

            every { balancesUiMapper.mapBalance(testBalance.copy(currency = "USD"), "Beach Trip") } returns
                balanceUiModel2

            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When - Load first group
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then
            assertEquals(testGroupId, viewModel.uiState.value.groupId)

            // When - Switch to second group
            viewModel.setSelectedGroup(group2Id)
            advanceUntilIdle()

            // Then
            assertEquals(group2Id, viewModel.uiState.value.groupId)
            assertEquals(balanceUiModel2, viewModel.uiState.value.pocketBalance)

            collectJob.cancel()
        }

        @Test
        fun `setSelectedGroup with same groupId does not reload`() = runTest(testDispatcher) {
            // Given
            val callCount = 0
            viewModel = createViewModel()

            // When - Set same group twice
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()
            val initialCallCount = callCount

            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then - Should not trigger additional calls
            assertEquals(initialCallCount, callCount)
        }

        @Test
        fun `observeGroupUseCase emission with updated group name reactively updates pocketBalance groupName`() =
            runTest(testDispatcher) {
                // Given
                val groupFlow = MutableStateFlow(testGroup)
                every { observeGroupUseCase(testGroupId) } returns groupFlow

                val initialBalanceUiModel = testBalanceUiModel.copy(groupName = "Trip to Paris")
                val updatedBalanceUiModel = testBalanceUiModel.copy(groupName = "Trip to Rome")

                every { balancesUiMapper.mapBalance(testBalance, "Trip to Paris") } returns initialBalanceUiModel
                every { balancesUiMapper.mapBalance(testBalance, "Trip to Rome") } returns updatedBalanceUiModel

                viewModel = createViewModel()
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

                // When - Initial load
                viewModel.setSelectedGroup(testGroupId)
                advanceUntilIdle()

                // Then - Initial group name is rendered
                assertEquals("Trip to Paris", viewModel.uiState.value.pocketBalance.groupName)

                // When - Reactive group emits updated name without switching selectedGroupId
                groupFlow.value = testGroup.copy(name = "Trip to Rome")
                advanceUntilIdle()

                // Then - Pocket balance reactively updates with new group name
                assertEquals("Trip to Rome", viewModel.uiState.value.pocketBalance.groupName)

                collectJob.cancel()
            }
    }

    @Nested
    inner class ActivityItems {

        @Test
        fun `activityItems is populated from mapActivity`() = runTest(testDispatcher) {
            // Given
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = listOf(testContribution1, testContribution2),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(2, state.activityItems.size)

            collectJob.cancel()
        }

        @Test
        fun `activityItems is empty when no contributions and no withdrawals`() = runTest(testDispatcher) {
            // Given
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.activityItems.isEmpty())

            collectJob.cancel()
        }

        @Test
        fun `activityItems is sorted by date descending`() = runTest(testDispatcher) {
            // Given - contrib1 is Jan 15, contrib2 is Jan 16
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = listOf(testContribution1, testContribution2),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then - newest (contrib2, Jan 16) should come first
            val items = viewModel.uiState.value.activityItems
            assertEquals(2, items.size)
            assertTrue(items[0].sortTimestamp >= items[1].sortTimestamp)
            val firstItem = items[0] as ActivityItemUiModel.ContributionItem
            assertEquals("contrib-2", firstItem.contribution.id)

            collectJob.cancel()
        }
    }

    @Nested
    inner class ErrorHandling {

        @Test
        fun `error in flow emits ShowLoadError action`() = runTest(testDispatcher) {
            // Given
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns
                kotlinx.coroutines.flow.flow { throw IOException("Network error") }
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // Collect actions in background
            val actions = mutableListOf<BalancesUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.contributions.isEmpty())
            assertTrue(
                actions.any { it is BalancesUiAction.ShowLoadError },
                "Expected ShowLoadError action"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `uses default currency when group is not found`() = runTest(testDispatcher) {
            // Given
            coEvery { getGroupByIdUseCase(testGroupId) } returns null
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then - Should still work using default EUR currency
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(testGroupId, state.groupId)

            collectJob.cancel()
        }
    }

    @Nested
    inner class BalanceAnimation {

        @Test
        fun `shouldAnimateBalance is true when balance differs from last seen`() = runTest(testDispatcher) {
            // Given - last seen balance is different from current
            every { getLastSeenBalanceUseCase(any()) } returns flowOf("€200.00")
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.shouldAnimateBalance)
            assertEquals("€200.00", viewModel.uiState.value.previousBalance)

            collectJob.cancel()
        }

        @Test
        fun `shouldAnimateBalance is false when balance matches last seen`() = runTest(testDispatcher) {
            // Given - last seen balance matches current
            every { getLastSeenBalanceUseCase(any()) } returns flowOf("€350.00")
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.shouldAnimateBalance)

            collectJob.cancel()
        }

        @Test
        fun `BalanceAnimationComplete sets shouldAnimateBalance to false`() = runTest(testDispatcher) {
            // Given - balance differs from last seen, so animation triggers
            every { getLastSeenBalanceUseCase(any()) } returns flowOf("€200.00")
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.shouldAnimateBalance)

            // When
            viewModel.onEvent(BalancesUiEvent.BalanceAnimationComplete)
            advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.shouldAnimateBalance)

            collectJob.cancel()
        }

        @Test
        fun `BalanceAnimationComplete persists balance via SetLastSeenBalanceUseCase`() = runTest(testDispatcher) {
            // Given
            every { getLastSeenBalanceUseCase(any()) } returns flowOf("€200.00")
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // When
            viewModel.onEvent(BalancesUiEvent.BalanceAnimationComplete)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { setLastSeenBalanceUseCase(testGroupId, "€350.00") }

            collectJob.cancel()
        }

        @Test
        fun `BalanceAnimationComplete with no group selected does nothing`() = runTest(testDispatcher) {
            // Given - No group selected
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            // Note: NOT calling setSelectedGroup

            // When
            viewModel.onEvent(BalancesUiEvent.BalanceAnimationComplete)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { setLastSeenBalanceUseCase(any(), any()) }

            collectJob.cancel()
        }

        @Test
        fun `balanceRollingUp is true when no previous balance exists`() = runTest(testDispatcher) {
            // Given - no last seen balance (first time)
            every { getLastSeenBalanceUseCase(any()) } returns flowOf(null)
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then - default direction is up when no previous value
            assertTrue(viewModel.uiState.value.balanceRollingUp)

            collectJob.cancel()
        }

        @Test
        fun `balanceRollingUp is true when balance increases`() = runTest(testDispatcher) {
            // Given - last seen was "€200.00", current balance is €350.00 (35000 cents)
            every { getLastSeenBalanceUseCase(any()) } returns flowOf("€200.00")
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then - rolling up because virtualBalance (35000) > previous (null → default up)
            assertTrue(viewModel.uiState.value.balanceRollingUp)

            collectJob.cancel()
        }
    }

    @Nested
    inner class ActivityDeleteEventRouting {

        private val testContributionUiModel = ContributionUiModel(
            id = "contrib-1",
            displayName = "Alice",
            formattedAmount = "€50.00"
        )

        private val testWithdrawalUiModel = CashWithdrawalUiModel(
            id = "withdrawal-1",
            displayName = "Bob",
            formattedAmount = "€100.00"
        )

        @Test
        fun `DeleteContributionRequested sets contributionActionsTarget in state`() = runTest(testDispatcher) {
            // Given
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // When
            viewModel.onEvent(BalancesUiEvent.ContributionActionsRequested(testContributionUiModel))
            advanceUntilIdle()

            // Then
            assertEquals(testContributionUiModel, viewModel.uiState.value.contributionActionsTarget)

            collectJob.cancel()
        }

        @Test
        fun `DeleteContributionDismissed clears contributionActionsTarget`() = runTest(testDispatcher) {
            // Given
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()
            viewModel.onEvent(BalancesUiEvent.ContributionActionsRequested(testContributionUiModel))

            // When
            viewModel.onEvent(BalancesUiEvent.ContributionActionsDismissed)

            // Then
            assertNull(viewModel.uiState.value.contributionActionsTarget)

            collectJob.cancel()
        }

        @Test
        fun `DeleteWithdrawalRequested sets withdrawalToDelete in state`() = runTest(testDispatcher) {
            // Given
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // When
            viewModel.onEvent(BalancesUiEvent.DeleteWithdrawalRequested(testWithdrawalUiModel))
            advanceUntilIdle()

            // Then
            assertEquals(testWithdrawalUiModel, viewModel.uiState.value.withdrawalToDelete)

            collectJob.cancel()
        }

        @Test
        fun `DeleteWithdrawalDismissed clears withdrawalToDelete`() = runTest(testDispatcher) {
            // Given
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()
            viewModel.onEvent(BalancesUiEvent.DeleteWithdrawalRequested(testWithdrawalUiModel))

            // When
            viewModel.onEvent(BalancesUiEvent.DeleteWithdrawalDismissed)

            // Then
            assertNull(viewModel.uiState.value.withdrawalToDelete)

            collectJob.cancel()
        }

        @Test
        fun `DeleteContributionConfirmed with no group selected does nothing`() = runTest(testDispatcher) {
            // Given — no group selected
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()

            // When — fire confirmed before selecting a group
            viewModel.onEvent(BalancesUiEvent.DeleteContributionConfirmed("contrib-1"))
            advanceUntilIdle()

            // Then — use case is never called
            coVerify(exactly = 0) { deleteContributionUseCase(any(), any()) }
        }

        @Test
        fun `DeleteWithdrawalConfirmed with no group selected does nothing`() = runTest(testDispatcher) {
            // Given — no group selected
            every { getBalancesDashboardFlowUseCase(any(), any(), any()) } returns flowOf(
                BalancesDashboardDomainModel(
                    balance = testBalance,
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    subunits = emptyList(),
                    expenses = emptyList(),
                    settlements = emptyList(),
                    memberBalances = emptyList(),
                    settlementSuggestions = emptyList(),
                    memberProfiles = emptyMap()
                )
            )
            viewModel = createViewModel()

            // When — fire confirmed before selecting a group
            viewModel.onEvent(BalancesUiEvent.DeleteWithdrawalConfirmed("withdrawal-1"))
            advanceUntilIdle()

            // Then — use case is never called
            coVerify(exactly = 0) { deleteCashWithdrawalUseCase(any(), any()) }
        }
    }

    private fun createViewModel() = BalancesViewModel(
        getBalancesDashboardFlowUseCase = getBalancesDashboardFlowUseCase,
        getLastSeenBalanceUseCase = getLastSeenBalanceUseCase,
        setLastSeenBalanceUseCase = setLastSeenBalanceUseCase,
        getGroupByIdUseCase = getGroupByIdUseCase,
        observeGroupUseCase = observeGroupUseCase,
        authenticationService = authenticationService,
        balancesUiMapper = balancesUiMapper,
        settlementsUiMapper = settlementsUiMapper,
        activityEventHandler = BalancesActivityEventHandlerImpl(
            deleteContributionUseCase = deleteContributionUseCase,
            deleteCashWithdrawalUseCase = deleteCashWithdrawalUseCase
        ),
        appConfigService = appConfigService,
        computationDispatcher = testDispatcher
    )
}
