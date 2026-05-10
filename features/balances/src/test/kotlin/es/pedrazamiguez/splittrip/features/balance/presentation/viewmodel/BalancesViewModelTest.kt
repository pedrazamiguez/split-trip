package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteCashWithdrawalUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupPocketBalanceFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.BalancesUiMapper
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private lateinit var getGroupPocketBalanceFlowUseCase: GetGroupPocketBalanceFlowUseCase
    private lateinit var getGroupContributionsFlowUseCase: GetGroupContributionsFlowUseCase
    private lateinit var getCashWithdrawalsFlowUseCase: GetCashWithdrawalsFlowUseCase
    private lateinit var getGroupExpensesFlowUseCase: GetGroupExpensesFlowUseCase
    private lateinit var getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase
    private lateinit var getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase
    private lateinit var getGroupByIdUseCase: GetGroupByIdUseCase
    private lateinit var authenticationService: AuthenticationService
    private lateinit var balancesUiMapper: BalancesUiMapper
    private lateinit var getLastSeenBalanceUseCase: GetLastSeenBalanceUseCase
    private lateinit var setLastSeenBalanceUseCase: SetLastSeenBalanceUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var deleteContributionUseCase: DeleteContributionUseCase
    private lateinit var deleteCashWithdrawalUseCase: DeleteCashWithdrawalUseCase
    private lateinit var viewModel: BalancesViewModel

    private val testGroupId = "group-123"
    private val testGroup = Group(
        id = testGroupId,
        name = "Trip to Paris",
        currency = "EUR"
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
        getGroupPocketBalanceFlowUseCase = mockk()
        getGroupContributionsFlowUseCase = mockk()
        getCashWithdrawalsFlowUseCase = mockk()
        getGroupExpensesFlowUseCase = mockk()
        getMemberBalancesFlowUseCase = GetMemberBalancesFlowUseCase()
        getGroupSubunitsFlowUseCase = mockk()
        getGroupByIdUseCase = mockk()
        authenticationService = mockk()
        balancesUiMapper = mockk()
        getLastSeenBalanceUseCase = mockk()
        setLastSeenBalanceUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        deleteContributionUseCase = mockk(relaxed = true)
        deleteCashWithdrawalUseCase = mockk(relaxed = true)

        // Default mock for getGroupByIdUseCase
        coEvery { getGroupByIdUseCase(testGroupId) } returns testGroup

        // Default mock for authenticationService
        every { authenticationService.currentUserId() } returns "test-user-id"

        // Default mock for member display names (returns empty map)
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()

        // Default mock for last-seen balance (no previous balance stored)
        every { getLastSeenBalanceUseCase(any()) } returns flowOf(null)
        coEvery { setLastSeenBalanceUseCase(any(), any()) } just Runs

        // Default mock for cash withdrawals flow
        every { getCashWithdrawalsFlowUseCase(any()) } returns flowOf(emptyList())
        every { balancesUiMapper.mapCashWithdrawals(any(), any(), any(), any(), any()) } returns persistentListOf()

        // Default mock for subunits flow (no subunits by default)
        every { getGroupSubunitsFlowUseCase(any()) } returns flowOf(emptyList())

        // Default mock for expenses flow (empty by default)
        every { getGroupExpensesFlowUseCase(any()) } returns flowOf(emptyList())

        // Default mock for mapper
        every { balancesUiMapper.mapBalance(any(), any()) } returns testBalanceUiModel
        every { balancesUiMapper.mapMemberBalances(any(), any(), any(), any(), any(), any()) } returns
            persistentListOf()
        every { balancesUiMapper.mapContributions(any(), any(), any(), any()) } answers {
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
                            ?.atZone(java.time.ZoneOffset.UTC)
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
                            ?.atZone(java.time.ZoneOffset.UTC)
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
            every { getGroupPocketBalanceFlowUseCase(any(), any()) } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(any()) } returns flowOf(emptyList())

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
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(
                testBalance
            )
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(
                listOf(testContribution1, testContribution2)
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
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(
                testBalance
            )
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(
                listOf(testContribution1)
            )
            every { getGroupPocketBalanceFlowUseCase(group2Id, "USD") } returns flowOf(
                testBalance.copy(currency = "USD")
            )
            every { getGroupContributionsFlowUseCase(group2Id) } returns flowOf(
                listOf(testContribution2)
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
            var callCount = 0
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } answers {
                callCount++
                flowOf(testBalance)
            }
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
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
    }

    @Nested
    inner class ActivityItems {

        @Test
        fun `activityItems is populated from mapActivity`() = runTest(testDispatcher) {
            // Given
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(
                testBalance
            )
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(
                listOf(testContribution1, testContribution2)
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
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(
                testBalance
            )
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
            every { getCashWithdrawalsFlowUseCase(testGroupId) } returns flowOf(emptyList())
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
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(
                testBalance
            )
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(
                listOf(testContribution1, testContribution2)
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
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flow {
                throw IOException("Network error")
            }
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
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
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(
                testBalance
            )
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
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
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
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
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
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
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
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
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
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
            every { getGroupPocketBalanceFlowUseCase(any(), any()) } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(any()) } returns flowOf(emptyList())
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
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
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
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
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
        fun `DeleteContributionRequested sets contributionToDelete in state`() = runTest(testDispatcher) {
            // Given
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // When
            viewModel.onEvent(BalancesUiEvent.DeleteContributionRequested(testContributionUiModel))
            advanceUntilIdle()

            // Then
            assertEquals(testContributionUiModel, viewModel.uiState.value.contributionToDelete)

            collectJob.cancel()
        }

        @Test
        fun `DeleteContributionDismissed clears contributionToDelete`() = runTest(testDispatcher) {
            // Given
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()
            viewModel.onEvent(BalancesUiEvent.DeleteContributionRequested(testContributionUiModel))

            // When
            viewModel.onEvent(BalancesUiEvent.DeleteContributionDismissed)

            // Then
            assertNull(viewModel.uiState.value.contributionToDelete)

            collectJob.cancel()
        }

        @Test
        fun `DeleteWithdrawalRequested sets withdrawalToDelete in state`() = runTest(testDispatcher) {
            // Given
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
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
            every { getGroupPocketBalanceFlowUseCase(testGroupId, "EUR") } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(testGroupId) } returns flowOf(emptyList())
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
            every { getGroupPocketBalanceFlowUseCase(any(), any()) } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(any()) } returns flowOf(emptyList())
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
            every { getGroupPocketBalanceFlowUseCase(any(), any()) } returns flowOf(testBalance)
            every { getGroupContributionsFlowUseCase(any()) } returns flowOf(emptyList())
            viewModel = createViewModel()

            // When — fire confirmed before selecting a group
            viewModel.onEvent(BalancesUiEvent.DeleteWithdrawalConfirmed("withdrawal-1"))
            advanceUntilIdle()

            // Then — use case is never called
            coVerify(exactly = 0) { deleteCashWithdrawalUseCase(any(), any()) }
        }
    }

    private fun createViewModel() = BalancesViewModel(
        useCases = BalancesUseCases(
            getGroupPocketBalanceFlowUseCase = getGroupPocketBalanceFlowUseCase,
            getGroupContributionsFlowUseCase = getGroupContributionsFlowUseCase,
            getCashWithdrawalsFlowUseCase = getCashWithdrawalsFlowUseCase,
            getGroupExpensesFlowUseCase = getGroupExpensesFlowUseCase,
            getMemberBalancesFlowUseCase = getMemberBalancesFlowUseCase,
            getGroupSubunitsFlowUseCase = getGroupSubunitsFlowUseCase,
            getGroupByIdUseCase = getGroupByIdUseCase,
            getLastSeenBalanceUseCase = getLastSeenBalanceUseCase,
            setLastSeenBalanceUseCase = setLastSeenBalanceUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            deleteContributionUseCase = deleteContributionUseCase,
            deleteCashWithdrawalUseCase = deleteCashWithdrawalUseCase
        ),
        authenticationService = authenticationService,
        balancesUiMapper = balancesUiMapper,
        activityEventHandler = BalancesActivityEventHandlerImpl(
            deleteContributionUseCase = deleteContributionUseCase,
            deleteCashWithdrawalUseCase = deleteCashWithdrawalUseCase
        ),
        computationDispatcher = testDispatcher
    )
}
