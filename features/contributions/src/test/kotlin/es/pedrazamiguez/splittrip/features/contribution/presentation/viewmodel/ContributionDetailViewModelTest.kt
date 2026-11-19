package es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.contribution.presentation.mapper.ContributionDetailUiMapper
import es.pedrazamiguez.splittrip.features.contribution.presentation.model.ContributionDetailUiModel
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.action.ContributionDetailUiAction
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.event.ContributionDetailUiEvent
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("ContributionDetailViewModel")
class ContributionDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getGroupContributionsFlowUseCase: GetGroupContributionsFlowUseCase
    private lateinit var observeGroupUseCase: ObserveGroupUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var getGroupSubunitsUseCase: GetGroupSubunitsUseCase
    private lateinit var deleteContributionUseCase: DeleteContributionUseCase
    private lateinit var authenticationService: AuthenticationService
    private lateinit var contributionDetailUiMapper: ContributionDetailUiMapper
    private lateinit var viewModel: ContributionDetailViewModel

    private val testGroup = Group(
        id = "group-1",
        name = "Trip",
        currency = "EUR",
        members = listOf("user-1", "user-2"),
        status = GroupStatus.ACTIVE
    )

    private val testSubunit = Subunit(
        id = "subunit-1",
        groupId = "group-1",
        name = "Couple A",
        memberIds = listOf("user-1", "user-2")
    )

    private val testContribution = Contribution(
        id = "contrib-1",
        groupId = "group-1",
        userId = "user-1",
        createdBy = "user-1",
        contributionScope = PayerType.GROUP,
        amount = 10000L,
        currency = "EUR"
    )

    private val testUiModel = ContributionDetailUiModel(
        id = "contrib-1",
        groupId = "group-1",
        formattedAmount = "100,00 €",
        contributorName = "Andrés"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getGroupContributionsFlowUseCase = mockk()
        observeGroupUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        getGroupSubunitsUseCase = mockk()
        deleteContributionUseCase = mockk()
        authenticationService = mockk()
        contributionDetailUiMapper = mockk()

        every { authenticationService.currentUserId() } returns "user-1"
        coEvery { getGroupSubunitsUseCase("group-1") } returns listOf(testSubunit)
        coEvery { getMemberProfilesUseCase(any()) } returns mapOf(
            "user-1" to User(userId = "user-1", email = "user1@test.com", displayName = "Andrés")
        )
        every { observeGroupUseCase("group-1") } returns flowOf(testGroup)
        every { getGroupContributionsFlowUseCase("group-1") } returns flowOf(listOf(testContribution))
        every {
            contributionDetailUiMapper.map(
                contribution = any(),
                groupCurrency = any(),
                memberProfiles = any(),
                subunitsMap = any(),
                groupMemberIds = any(),
                currentUserId = any()
            )
        } returns testUiModel

        viewModel = ContributionDetailViewModel(
            getGroupContributionsFlowUseCase = getGroupContributionsFlowUseCase,
            observeGroupUseCase = observeGroupUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            getGroupSubunitsUseCase = getGroupSubunitsUseCase,
            deleteContributionUseCase = deleteContributionUseCase,
            authenticationService = authenticationService,
            contributionDetailUiMapper = contributionDetailUiMapper
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() {
        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertFalse(state.hasError)
        assertEquals(null, state.contribution)
    }

    @Test
    fun `setContext loads contribution, observes group, and updates state to loaded`() =
        runTest(testDispatcher) {
            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.setContext("group-1", "contrib-1")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.hasError)
            assertEquals("contrib-1", state.contribution?.id)
            assertEquals("100,00 €", state.contribution?.formattedAmount)
            assertFalse(state.isGroupArchived)

            collectJob.cancel()
        }

    @Test
    fun `setContext sets hasError to true when contribution is not found`() =
        runTest(testDispatcher) {
            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.setContext("group-1", "non-existent")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.hasError)
            assertEquals(null, state.contribution)

            collectJob.cancel()
        }

    @Test
    fun `observing archived group updates isGroupArchived flag in state`() =
        runTest(testDispatcher) {
            val archivedGroup = testGroup.copy(status = GroupStatus.ARCHIVED)
            every { observeGroupUseCase("group-1") } returns flowOf(archivedGroup)

            val collectJob = launch { viewModel.uiState.collect {} }

            viewModel.setContext("group-1", "contrib-1")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isGroupArchived)

            collectJob.cancel()
        }

    @Test
    fun `onEvent DeleteConfirmed invokes DeleteContributionUseCase and emits DeleteSuccess`() =
        runTest(testDispatcher) {
            coEvery { deleteContributionUseCase("group-1", "contrib-1") } just Runs

            val emitted = mutableListOf<ContributionDetailUiAction>()
            val collectActionsJob = launch {
                viewModel.actions.collect { emitted.add(it) }
            }
            val collectStateJob = launch { viewModel.uiState.collect {} }

            viewModel.setContext("group-1", "contrib-1")
            advanceUntilIdle()

            viewModel.onEvent(ContributionDetailUiEvent.DeleteConfirmed)
            advanceUntilIdle()

            coVerify { deleteContributionUseCase("group-1", "contrib-1") }
            assertTrue(emitted.any { it is ContributionDetailUiAction.DeleteSuccess })

            collectActionsJob.cancel()
            collectStateJob.cancel()
        }

    @Test
    fun `onEvent DeleteConfirmed emits ShowError when use case throws exception`() =
        runTest(testDispatcher) {
            coEvery { deleteContributionUseCase("group-1", "contrib-1") } throws RuntimeException("Delete failed")

            val emitted = mutableListOf<ContributionDetailUiAction>()
            val collectActionsJob = launch {
                viewModel.actions.collect { emitted.add(it) }
            }
            val collectStateJob = launch { viewModel.uiState.collect {} }

            viewModel.setContext("group-1", "contrib-1")
            advanceUntilIdle()

            viewModel.onEvent(ContributionDetailUiEvent.DeleteConfirmed)
            advanceUntilIdle()

            coVerify { deleteContributionUseCase("group-1", "contrib-1") }
            assertTrue(emitted.any { it is ContributionDetailUiAction.ShowError })

            collectActionsJob.cancel()
            collectStateJob.cancel()
        }

    @Test
    fun `onEvent LinkedExpenseClicked emits NavigateToExpenseDetail with expenseId`() =
        runTest(testDispatcher) {
            val emitted = mutableListOf<ContributionDetailUiAction>()
            val collectActionsJob = launch {
                viewModel.actions.collect { emitted.add(it) }
            }

            viewModel.onEvent(ContributionDetailUiEvent.LinkedExpenseClicked("expense-123"))
            advanceUntilIdle()

            assertEquals(
                listOf(ContributionDetailUiAction.NavigateToExpenseDetail("expense-123")),
                emitted
            )

            collectActionsJob.cancel()
        }

    @Test
    fun `onEvent LinkedExpenseClicked with blank expenseId does not emit NavigateToExpenseDetail`() =
        runTest(testDispatcher) {
            val emitted = mutableListOf<ContributionDetailUiAction>()
            val collectActionsJob = launch {
                viewModel.actions.collect { emitted.add(it) }
            }

            viewModel.onEvent(ContributionDetailUiEvent.LinkedExpenseClicked("   "))
            advanceUntilIdle()

            assertTrue(emitted.isEmpty())

            collectActionsJob.cancel()
        }
}
