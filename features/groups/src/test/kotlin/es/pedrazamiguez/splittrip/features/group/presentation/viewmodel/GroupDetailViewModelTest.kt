package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.exception.CannotLeaveGroupException
import es.pedrazamiguez.splittrip.domain.exception.UnresolvedSettlementsException
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateWithStaleness
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.AreMemberSettlementsResolvedUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupSettlementsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetSettlementSuggestionsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ArchiveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.DeleteGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.LeaveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.LeaveWizardUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupCurrencyRateUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.model.leave.LeaveBalanceSummaryUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.model.leave.LeaveSubunitImpactUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.model.leave.LeaveWizardStep
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.GroupDetailUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.GroupDetailUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.GroupLeaveWizardEventHandlerImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase
    private lateinit var getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var groupUiMapper: GroupUiMapper
    private lateinit var observeGroupUseCase: ObserveGroupUseCase
    private lateinit var authenticationService: AuthenticationService
    private lateinit var archiveGroupUseCase: ArchiveGroupUseCase
    private lateinit var deleteGroupUseCase: DeleteGroupUseCase
    private lateinit var leaveGroupUseCase: LeaveGroupUseCase
    private lateinit var getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase
    private lateinit var areMemberSettlementsResolvedUseCase: AreMemberSettlementsResolvedUseCase
    private lateinit var getSettlementSuggestionsUseCase: GetSettlementSuggestionsUseCase

    private lateinit var getGroupExpensesFlowUseCase: GetGroupExpensesFlowUseCase
    private lateinit var getGroupContributionsFlowUseCase: GetGroupContributionsFlowUseCase
    private lateinit var getCashWithdrawalsFlowUseCase: GetCashWithdrawalsFlowUseCase
    private lateinit var getGroupSettlementsFlowUseCase: GetGroupSettlementsFlowUseCase
    private lateinit var getExchangeRateUseCase: GetExchangeRateUseCase
    private lateinit var leaveWizardUiMapper: LeaveWizardUiMapper
    private lateinit var leaveWizardEventHandler: GroupLeaveWizardEventHandlerImpl
    private lateinit var viewModel: GroupDetailViewModel

    private val testGroupId = "group-123"
    private val testGroup = Group(
        id = testGroupId,
        name = "Summer Trip",
        description = "A fun trip",
        currency = "EUR",
        members = listOf("user-1", "user-2"),
        createdBy = "user-1"
    )
    private val testGroupUiModel = GroupUiModel(
        id = testGroupId,
        name = "Summer Trip",
        currency = "EUR",
        membersCountText = "2 travelers"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getGroupSubunitsFlowUseCase = mockk()
        getUserGroupsFlowUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        groupUiMapper = mockk()
        observeGroupUseCase = mockk()
        authenticationService = mockk(relaxed = true)
        archiveGroupUseCase = mockk(relaxed = true)
        deleteGroupUseCase = mockk(relaxed = true)
        leaveGroupUseCase = mockk(relaxed = true)
        getMemberBalancesFlowUseCase = mockk(relaxed = true)
        areMemberSettlementsResolvedUseCase = mockk(relaxed = true)
        getSettlementSuggestionsUseCase = mockk(relaxed = true)

        getGroupExpensesFlowUseCase = mockk(relaxed = true)
        getGroupContributionsFlowUseCase = mockk(relaxed = true)
        getCashWithdrawalsFlowUseCase = mockk(relaxed = true)
        getGroupSettlementsFlowUseCase = mockk(relaxed = true)
        getExchangeRateUseCase = mockk(relaxed = true)
        leaveWizardUiMapper = mockk(relaxed = true)

        leaveWizardEventHandler = GroupLeaveWizardEventHandlerImpl(
            authenticationService = authenticationService,
            observeGroupUseCase = observeGroupUseCase,
            getGroupExpensesFlowUseCase = getGroupExpensesFlowUseCase,
            getGroupContributionsFlowUseCase = getGroupContributionsFlowUseCase,
            getCashWithdrawalsFlowUseCase = getCashWithdrawalsFlowUseCase,
            getGroupSubunitsFlowUseCase = getGroupSubunitsFlowUseCase,
            getMemberBalancesFlowUseCase = getMemberBalancesFlowUseCase,
            getSettlementSuggestionsUseCase = getSettlementSuggestionsUseCase,
            areMemberSettlementsResolvedUseCase = areMemberSettlementsResolvedUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,

            leaveGroupUseCase = leaveGroupUseCase,
            getGroupSettlementsFlowUseCase = getGroupSettlementsFlowUseCase,
            leaveWizardUiMapper = leaveWizardUiMapper
        )

        // Default stubs
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()
        every { getGroupSubunitsFlowUseCase(any()) } returns flowOf(emptyList())
        every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup))
        every { groupUiMapper.toGroupUiModel(any(), any()) } returns testGroupUiModel
        every { observeGroupUseCase(any()) } returns flowOf(testGroup)
        every { authenticationService.requireUserId() } returns "user-1"
        every { getGroupExpensesFlowUseCase(any()) } returns flowOf(emptyList())
        every { getGroupContributionsFlowUseCase(any()) } returns flowOf(emptyList())
        every { getCashWithdrawalsFlowUseCase(any()) } returns flowOf(emptyList())
        every { getGroupSettlementsFlowUseCase(any()) } returns flowOf(emptyList())
        coEvery { areMemberSettlementsResolvedUseCase(any(), any()) } returns emptyList()
        coEvery { getExchangeRateUseCase(any(), any()) } returns null
        every { groupUiMapper.mapCurrencyExchangeRates(any(), any()) } returns persistentListOf()

        viewModel = createViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = GroupDetailViewModel(
        observeGroupUseCase = observeGroupUseCase,
        getGroupSubunitsFlowUseCase = getGroupSubunitsFlowUseCase,
        getUserGroupsFlowUseCase = getUserGroupsFlowUseCase,
        getMemberProfilesUseCase = getMemberProfilesUseCase,
        groupUiMapper = groupUiMapper,
        authenticationService = authenticationService,
        deleteGroupUseCase = deleteGroupUseCase,
        getGroupSettlementsFlowUseCase = getGroupSettlementsFlowUseCase,
        leaveWizardEventHandler = leaveWizardEventHandler,
        getExchangeRateUseCase = getExchangeRateUseCase
    )

    @Nested
    inner class InitialState {

        @Test
        fun `initial state is loading with no group`() = runTest(testDispatcher) {
            val state = viewModel.uiState.value

            assertTrue(state.isLoading)
            assertNull(state.group)
            assertFalse(state.hasError)
            assertEquals(0, state.subunitsCount)
        }
    }

    @Nested
    inner class SetGroupId {

        @Test
        fun `blank group id does not trigger load`() = runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId("")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.group)

            collectJob.cancel()
        }

        @Test
        fun `valid group id triggers group loading`() = runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.group)
            assertEquals(testGroupUiModel, state.group)

            collectJob.cancel()
        }
    }

    @Nested
    inner class LeaveWizardFlow {

        @Test
        fun `on LeaveClicked populates leaveWizardState and opens wizard sheet`() = runTest(
            testDispatcher
        ) {
            val memberBalance = MemberBalance(userId = "user-1", pocketBalance = 2500L, cashInHand = 1000L)
            val subunit = Subunit(
                id = "sub-1",
                groupId = testGroupId,
                name = "Test",
                memberShares = mapOf(
                    "user-1" to BigDecimal.ONE
                )
            )
            every { getGroupSubunitsFlowUseCase(testGroupId) } returns flowOf(listOf(subunit))
            every {
                getMemberBalancesFlowUseCase.computeMemberBalances(any())
            } returns
                listOf(memberBalance)
            every {
                leaveWizardUiMapper.toBalanceSummaryUiModel(
                    memberBalance = any(),
                    memberBalances = any(),
                    currentUserId = any(),
                    memberProfiles = any(),
                    currency = any()
                )
            } returns LeaveBalanceSummaryUiModel("€25.00", "€10.00", "€35.00")

            every { leaveWizardUiMapper.toSubunitImpactUiModel(any()) } returns
                LeaveSubunitImpactUiModel(hasSubunitImpact = true)

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(GroupDetailUiEvent.LeaveClicked)
            advanceUntilIdle()

            val wizardState = viewModel.uiState.value.leaveWizardState
            assertTrue(wizardState.showSheet)
            assertEquals(LeaveWizardStep.BALANCE_SUMMARY, wizardState.currentStep)
            assertTrue(wizardState.activeSteps.contains(LeaveWizardStep.BALANCE_SUMMARY))

            assertTrue(wizardState.activeSteps.contains(LeaveWizardStep.CONFIRMATION))

            collectJob.cancel()
        }

        @Test
        fun `on LeaveClicked with zero balance opens wizard at CONFIRMATION step`() = runTest(
            testDispatcher
        ) {
            val memberBalance = MemberBalance(userId = "user-1", pocketBalance = 0L, cashInHand = 0L)
            every {
                getMemberBalancesFlowUseCase.computeMemberBalances(any())
            } returns
                listOf(memberBalance)
            coEvery { areMemberSettlementsResolvedUseCase(any(), any()) } returns emptyList()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(GroupDetailUiEvent.LeaveClicked)
            advanceUntilIdle()

            val wizardState = viewModel.uiState.value.leaveWizardState
            assertTrue(wizardState.showSheet)
            assertEquals(LeaveWizardStep.CONFIRMATION, wizardState.currentStep)
            assertEquals(listOf(LeaveWizardStep.CONFIRMATION), wizardState.activeSteps)

            collectJob.cancel()
        }

        @Test
        fun `on LeaveClicked with unresolved settlements includes CONFIRMATION step`() = runTest(
            testDispatcher
        ) {
            val memberBalance = MemberBalance(userId = "user-1", pocketBalance = 0L, cashInHand = 0L)
            every {
                getMemberBalancesFlowUseCase.computeMemberBalances(any())
            } returns listOf(memberBalance)
            coEvery { areMemberSettlementsResolvedUseCase(any(), any()) } returns listOf(mockk())

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(GroupDetailUiEvent.LeaveClicked)
            advanceUntilIdle()

            val wizardState = viewModel.uiState.value.leaveWizardState
            assertTrue(wizardState.activeSteps.contains(LeaveWizardStep.CONFIRMATION))

            collectJob.cancel()
        }

        @Test
        fun `on LeaveClicked with blank groupId does nothing`() = runTest(testDispatcher) {
            viewModel.onEvent(GroupDetailUiEvent.LeaveClicked)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.leaveWizardState.showSheet)
        }

        @Test
        fun `on LeaveClicked exception emits ShowError action`() = runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            every {
                getMemberBalancesFlowUseCase.computeMemberBalances(any())
            } throws
                RuntimeException("Calculation failed")

            val actions = mutableListOf<GroupDetailUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.onEvent(GroupDetailUiEvent.LeaveClicked)
            advanceUntilIdle()

            assertTrue(actions.any { it is GroupDetailUiAction.ShowError })

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `on WizardNextClicked advances currentStep through activeSteps sequence`() = runTest(testDispatcher) {
            val memberBalance = MemberBalance(userId = "user-1", pocketBalance = 2500L, cashInHand = 0L)
            every {
                getMemberBalancesFlowUseCase.computeMemberBalances(any())
            } returns
                listOf(memberBalance)

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(GroupDetailUiEvent.LeaveClicked)
            advanceUntilIdle()

            assertEquals(LeaveWizardStep.BALANCE_SUMMARY, viewModel.uiState.value.leaveWizardState.currentStep)

            viewModel.onEvent(GroupDetailUiEvent.WizardNextClicked)
            advanceUntilIdle()

            assertEquals(LeaveWizardStep.CONFIRMATION, viewModel.uiState.value.leaveWizardState.currentStep)

            coEvery { leaveGroupUseCase(testGroupId) } returns Result.success(Unit)
            viewModel.onEvent(GroupDetailUiEvent.WizardNextClicked)
            advanceUntilIdle()

            coVerify(exactly = 1) { leaveGroupUseCase(testGroupId) }

            collectJob.cancel()
        }

        @Test
        fun `on WizardBackClicked moves currentStep backward or closes sheet`() = runTest(testDispatcher) {
            val memberBalance = MemberBalance(userId = "user-1", pocketBalance = 2500L, cashInHand = 0L)
            every {
                getMemberBalancesFlowUseCase.computeMemberBalances(any())
            } returns
                listOf(memberBalance)

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(GroupDetailUiEvent.LeaveClicked)
            advanceUntilIdle()

            viewModel.onEvent(GroupDetailUiEvent.WizardNextClicked)
            advanceUntilIdle()
            assertEquals(LeaveWizardStep.CONFIRMATION, viewModel.uiState.value.leaveWizardState.currentStep)

            viewModel.onEvent(GroupDetailUiEvent.WizardBackClicked)
            advanceUntilIdle()
            assertEquals(LeaveWizardStep.BALANCE_SUMMARY, viewModel.uiState.value.leaveWizardState.currentStep)

            viewModel.onEvent(GroupDetailUiEvent.WizardBackClicked)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.leaveWizardState.showSheet)

            collectJob.cancel()
        }

        @Test
        fun `on WizardCancelled or LeaveCancelled closes leave wizard sheet`() = runTest(testDispatcher) {
            val memberBalance = MemberBalance(userId = "user-1", pocketBalance = 2500L, cashInHand = 0L)
            every {
                getMemberBalancesFlowUseCase.computeMemberBalances(any())
            } returns listOf(memberBalance)

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(GroupDetailUiEvent.LeaveClicked)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.leaveWizardState.showSheet)

            viewModel.onEvent(GroupDetailUiEvent.WizardCancelled)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.leaveWizardState.showSheet)

            viewModel.onEvent(GroupDetailUiEvent.LeaveClicked)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.leaveWizardState.showSheet)

            viewModel.onEvent(GroupDetailUiEvent.LeaveCancelled)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.leaveWizardState.showSheet)

            collectJob.cancel()
        }

        @Test
        fun `on LeaveConfirmed failure with NON_ZERO_POCKET_BALANCE emits balance error`() = runTest(testDispatcher) {
            val ex = CannotLeaveGroupException(CannotLeaveGroupException.Reason.NON_ZERO_POCKET_BALANCE)
            coEvery { leaveGroupUseCase(testGroupId) } returns Result.failure(ex)

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            val actions = mutableListOf<GroupDetailUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.onEvent(GroupDetailUiEvent.LeaveConfirmed)
            advanceUntilIdle()

            assertTrue(actions.any { it is GroupDetailUiAction.ShowError })

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `on LeaveConfirmed failure with IS_CREATOR emits admin error`() = runTest(testDispatcher) {
            val ex = CannotLeaveGroupException(CannotLeaveGroupException.Reason.IS_CREATOR)
            coEvery { leaveGroupUseCase(testGroupId) } returns Result.failure(ex)

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            val actions = mutableListOf<GroupDetailUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.onEvent(GroupDetailUiEvent.LeaveConfirmed)
            advanceUntilIdle()

            assertTrue(actions.any { it is GroupDetailUiAction.ShowError })
            assertFalse(viewModel.uiState.value.leaveWizardState.showSheet)

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `on LeaveConfirmed failure with UnresolvedSettlementsException navigates to CONFIRMATION step`() = runTest(
            testDispatcher
        ) {
            val ex = UnresolvedSettlementsException(testGroupId, emptyList())
            coEvery { leaveGroupUseCase(testGroupId) } returns Result.failure(ex)

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(GroupDetailUiEvent.LeaveConfirmed)
            advanceUntilIdle()

            val state = viewModel.uiState.value.leaveWizardState
            assertEquals(LeaveWizardStep.CONFIRMATION, state.currentStep)

            collectJob.cancel()
        }

        @Test
        fun `GroupDetailUiAction models coverage test`() {
            val dummyText = UiText.DynamicString("test")
            val showError = GroupDetailUiAction.ShowError(dummyText)
            val archiveSuccess = GroupDetailUiAction.ArchiveSuccess(dummyText)
            val deleteSuccess = GroupDetailUiAction.DeleteSuccess(dummyText)
            val leaveSuccess = GroupDetailUiAction.LeaveSuccess(dummyText)
            val navigate = GroupDetailUiAction.NavigateToSettlementOverview("group-1")

            assertEquals(dummyText, showError.message)
            assertEquals(dummyText, archiveSuccess.message)
            assertEquals(dummyText, deleteSuccess.message)
            assertEquals(dummyText, leaveSuccess.message)
            assertNull(navigate.message)
            assertEquals("group-1", navigate.groupId)
        }
    }

    @Nested
    inner class ArchiveFlow {

        @Test
        fun `ArchiveClicked emits NavigateToSettlementOverview action`() = runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            val actions = mutableListOf<GroupDetailUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.onEvent(GroupDetailUiEvent.ArchiveClicked)
            advanceUntilIdle()

            assertTrue(
                actions.any {
                    it is GroupDetailUiAction.NavigateToSettlementOverview &&
                        (it as GroupDetailUiAction.NavigateToSettlementOverview).groupId == testGroupId
                }
            )

            actionsJob.cancel()
            collectJob.cancel()
        }
    }

    @Nested
    inner class DeleteFlow {

        @Test
        fun `DeleteClicked and DeleteCancelled update showDeleteConfirmation state`() = runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(GroupDetailUiEvent.DeleteClicked)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showDeleteConfirmation)

            viewModel.onEvent(GroupDetailUiEvent.DeleteCancelled)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.showDeleteConfirmation)

            collectJob.cancel()
        }

        @Test
        fun `DeleteConfirmed success emits DeleteSuccess action`() = runTest(testDispatcher) {
            coEvery { deleteGroupUseCase(testGroupId) } returns Unit
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            val actions = mutableListOf<GroupDetailUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.onEvent(GroupDetailUiEvent.DeleteConfirmed)
            advanceUntilIdle()

            assertTrue(actions.any { it is GroupDetailUiAction.DeleteSuccess })

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `DeleteConfirmed failure emits ShowError action`() = runTest(testDispatcher) {
            coEvery { deleteGroupUseCase(testGroupId) } throws RuntimeException("Delete failed")
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            val actions = mutableListOf<GroupDetailUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.onEvent(GroupDetailUiEvent.DeleteConfirmed)
            advanceUntilIdle()

            assertTrue(actions.any { it is GroupDetailUiAction.ShowError })

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `DeleteConfirmed failure with UnresolvedSettlementsException emits specific ShowError action`() = runTest(
            testDispatcher
        ) {
            coEvery { deleteGroupUseCase(testGroupId) } throws UnresolvedSettlementsException(testGroupId, emptyList())
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            val actions = mutableListOf<GroupDetailUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.onEvent(GroupDetailUiEvent.DeleteConfirmed)
            advanceUntilIdle()

            val showErrorAction = actions.find { it is GroupDetailUiAction.ShowError } as? GroupDetailUiAction.ShowError
            assertNotNull(showErrorAction)
            assertEquals(
                R.string.error_group_delete_unresolved_settlements,
                (showErrorAction?.message as? UiText.StringResource)?.resId
            )

            actionsJob.cancel()
            collectJob.cancel()
        }
    }

    @Nested
    inner class ErrorCases {

        @Test
        fun `observeGroupUseCase null emission sets hasError true`() = runTest(testDispatcher) {
            every { observeGroupUseCase(testGroupId) } returns flowOf(null)

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.hasError)

            collectJob.cancel()
        }

        @Test
        fun `getMemberProfilesUseCase exception uses empty member profiles map`() = runTest(testDispatcher) {
            coEvery { getMemberProfilesUseCase(any()) } throws RuntimeException("Network error")

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.group)

            collectJob.cancel()
        }
    }

    @Nested
    inner class ExchangeRateResolution {

        @Test
        fun `resolves and maps secondary currency exchange rates when extraCurrencies are present`() =
            runTest(testDispatcher) {
                val groupWithExtra = testGroup.copy(
                    currency = "EUR",
                    extraCurrencies = listOf("USD", "THB")
                )
                every { observeGroupUseCase(testGroupId) } returns flowOf(groupWithExtra)
                coEvery { getExchangeRateUseCase("EUR", "USD") } returns ExchangeRateWithStaleness(
                    rate = BigDecimal("1.08"),
                    isStale = false
                )
                coEvery { getExchangeRateUseCase("EUR", "THB") } returns ExchangeRateWithStaleness(
                    rate = BigDecimal("38.25"),
                    isStale = false
                )
                val expectedRates = persistentListOf(
                    GroupCurrencyRateUiModel(currency = "USD", formattedRate = "1 EUR ≈ 1.08 USD"),
                    GroupCurrencyRateUiModel(currency = "THB", formattedRate = "1 EUR ≈ 38.25 THB")
                )
                every {
                    groupUiMapper.mapCurrencyExchangeRates(
                        "EUR",
                        mapOf("USD" to BigDecimal("1.08"), "THB" to BigDecimal("38.25"))
                    )
                } returns expectedRates

                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
                viewModel.setGroupId(testGroupId)
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(expectedRates, state.currencyRates)

                collectJob.cancel()
            }

        @Test
        fun `handles exchange rate fetch failure gracefully without breaking state`() =
            runTest(testDispatcher) {
                val groupWithExtra = testGroup.copy(
                    currency = "EUR",
                    extraCurrencies = listOf("USD")
                )
                every { observeGroupUseCase(testGroupId) } returns flowOf(groupWithExtra)
                coEvery { getExchangeRateUseCase("EUR", "USD") } throws RuntimeException("Offline")
                val expectedRates = persistentListOf(
                    GroupCurrencyRateUiModel(currency = "USD", formattedRate = null)
                )
                every {
                    groupUiMapper.mapCurrencyExchangeRates(
                        "EUR",
                        mapOf("USD" to null)
                    )
                } returns expectedRates

                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
                viewModel.setGroupId(testGroupId)
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertFalse(state.hasError)
                assertEquals(expectedRates, state.currencyRates)

                collectJob.cancel()
            }

        @Test
        fun `does not invoke getExchangeRateUseCase when extraCurrencies is empty`() =
            runTest(testDispatcher) {
                val groupWithoutExtra = testGroup.copy(
                    currency = "EUR",
                    extraCurrencies = emptyList()
                )
                every { observeGroupUseCase(testGroupId) } returns flowOf(groupWithoutExtra)

                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
                viewModel.setGroupId(testGroupId)
                advanceUntilIdle()

                coVerify(exactly = 0) { getExchangeRateUseCase(any(), any()) }
                assertTrue(viewModel.uiState.value.currencyRates.isEmpty())

                collectJob.cancel()
            }
    }
}
