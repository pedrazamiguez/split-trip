package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.IsUserAnonymousUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ArchiveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.DeleteGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.LeaveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.GroupsUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.GroupsUiEvent
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.io.IOException
import java.time.LocalDateTime
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase
    private lateinit var deleteGroupUseCase: DeleteGroupUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var groupUiMapper: GroupUiMapper
    private lateinit var isUserAnonymousUseCase: IsUserAnonymousUseCase
    private lateinit var authenticationService: AuthenticationService
    private lateinit var archiveGroupUseCase: ArchiveGroupUseCase
    private lateinit var leaveGroupUseCase: LeaveGroupUseCase
    private lateinit var viewModel: GroupsViewModel

    private val testGroup1 = Group(
        id = "group-1",
        name = "Trip to Paris",
        description = "Summer vacation",
        currency = "EUR",
        extraCurrencies = emptyList(),
        members = listOf("user-1", "user-2", "user-3"),
        createdAt = LocalDateTime.of(2024, 1, 15, 12, 0),
        lastUpdatedAt = LocalDateTime.of(2024, 1, 15, 12, 0)
    )

    private val testGroup2 = Group(
        id = "group-2",
        name = "Office Lunch",
        description = "Daily lunches",
        currency = "USD",
        extraCurrencies = emptyList(),
        members = listOf("user-1", "user-2", "user-3", "user-4", "user-5"),
        createdAt = LocalDateTime.of(2024, 2, 1, 10, 0),
        lastUpdatedAt = LocalDateTime.of(2024, 2, 1, 10, 0)
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getUserGroupsFlowUseCase = mockk()
        deleteGroupUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        groupUiMapper = mockk()

        // By default, GetUserGroupsFlowUseCase returns an empty list (prevents MockKException on init)
        every { getUserGroupsFlowUseCase() } returns flowOf(emptyList())

        // By default, GetMemberProfilesUseCase returns an empty map
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()

        // Mock the mapper to return predictable UI models (2-arg version used by ViewModel)
        every { groupUiMapper.toGroupUiModelList(any(), any()) } answers {
            val groups = firstArg<List<Group>>()
            groups.map { group ->
                GroupUiModel(
                    id = group.id,
                    name = group.name,
                    description = group.description,
                    currency = group.currency,
                    membersCountText = "${group.members.size} travelers",
                    dateText = group.createdAt?.toString() ?: ""
                )
            }.toImmutableList()
        }

        isUserAnonymousUseCase = mockk()
        every { isUserAnonymousUseCase() } returns flowOf(false)

        authenticationService = mockk()
        every { authenticationService.currentUserId() } returns "user-1"

        archiveGroupUseCase = mockk()
        coEvery { archiveGroupUseCase(any()) } returns Result.success(Unit)

        leaveGroupUseCase = mockk()
        coEvery { leaveGroupUseCase(any()) } returns Result.success(Unit)

        viewModel = createViewModel()
    }

    private fun createViewModel(
        getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase = this.getUserGroupsFlowUseCase,
        deleteGroupUseCase: DeleteGroupUseCase = this.deleteGroupUseCase,
        getMemberProfilesUseCase: GetMemberProfilesUseCase = this.getMemberProfilesUseCase,
        groupUiMapper: GroupUiMapper = this.groupUiMapper,
        isUserAnonymousUseCase: IsUserAnonymousUseCase = this.isUserAnonymousUseCase,
        authenticationService: AuthenticationService = this.authenticationService,
        archiveGroupUseCase: ArchiveGroupUseCase = this.archiveGroupUseCase,
        leaveGroupUseCase: LeaveGroupUseCase = this.leaveGroupUseCase
    ): GroupsViewModel {
        return GroupsViewModel(
            getUserGroupsFlowUseCase = getUserGroupsFlowUseCase,
            deleteGroupUseCase = deleteGroupUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            groupUiMapper = groupUiMapper,
            isUserAnonymousUseCase = isUserAnonymousUseCase,
            authenticationService = authenticationService,
            archiveGroupUseCase = archiveGroupUseCase,
            leaveGroupUseCase = leaveGroupUseCase
        )
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
            every { getUserGroupsFlowUseCase() } returns flowOf(emptyList())

            // When
            viewModel = createViewModel()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isLoading)
            assertTrue(state.groups.isEmpty())
        }

        @Test
        fun `emits groups from use case`() = runTest(testDispatcher) {
            // Given
            every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup1, testGroup2))
            viewModel = createViewModel()

            // Start collecting to activate the WhileSubscribed flow
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(2, state.groups.size)
            assertEquals("Trip to Paris", state.groups[0].name)
            assertEquals("Office Lunch", state.groups[1].name)

            collectJob.cancel()
        }

        @Test
        fun `handles empty groups list`() = runTest(testDispatcher) {
            // Given - flowOf completes immediately, so advanceUntilIdle processes
            // the full flow chain including the 400ms grace period delay
            every { getUserGroupsFlowUseCase() } returns flowOf(emptyList())
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            advanceUntilIdle()

            // Then - After grace period, empty state is shown
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.groups.isEmpty())

            collectJob.cancel()
        }

        @Test
        fun `grace period is cancelled when data arrives during wait`() = runTest(testDispatcher) {
            // Given - Simulates cold start: Room emits empty first, then data after sync
            val groupsFlow = MutableSharedFlow<List<Group>>()
            every { getUserGroupsFlowUseCase() } returns groupsFlow
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When - Room emits empty list (cold start, cloud sync not done yet)
            groupsFlow.emit(emptyList())
            runCurrent()

            // Then - Should stay in loading (grace period active)
            assertTrue(viewModel.uiState.value.isLoading)
            assertTrue(viewModel.uiState.value.groups.isEmpty())

            // When - Cloud sync completes, Room re-emits with data BEFORE grace period ends
            advanceTimeBy(200) // Only 200ms of 400ms grace period elapsed
            groupsFlow.emit(listOf(testGroup1))
            advanceUntilIdle()

            // Then - transformLatest cancelled the delay, data shows immediately
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(1, state.groups.size)
            assertEquals("Trip to Paris", state.groups[0].name)

            collectJob.cancel()
        }

        @Test
        fun `stays loading during grace period for empty list`() = runTest(testDispatcher) {
            // Given - Use MutableSharedFlow to keep the flow alive
            val groupsFlow = MutableSharedFlow<List<Group>>()
            every { getUserGroupsFlowUseCase() } returns groupsFlow
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When - Room emits empty list, within grace period
            groupsFlow.emit(emptyList())
            runCurrent()

            // Then - Should still be loading (within 400ms grace period)
            assertTrue(viewModel.uiState.value.isLoading)
            assertTrue(viewModel.uiState.value.groups.isEmpty())

            collectJob.cancel()
        }

        @Test
        fun `handles error from use case`() = runTest(testDispatcher) {
            // Given
            every { getUserGroupsFlowUseCase() } returns flow {
                throw IOException("Network error")
            }
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // Collect actions in background
            val actions = mutableListOf<GroupsUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.groups.isEmpty())
            assertTrue(
                actions.any { it is GroupsUiAction.ShowLoadError },
                "Expected ShowLoadError action"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `emits isAnonymous state from use case`() = runTest(testDispatcher) {
            // Given
            every { isUserAnonymousUseCase() } returns flowOf(true)
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.isAnonymous)

            collectJob.cancel()
        }
    }

    @Nested
    inner class DeleteGroupEvent {

        @Test
        fun `DeleteGroup event calls use case with correct groupId`() = runTest(testDispatcher) {
            // Given
            every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup1, testGroup2))
            coEvery { deleteGroupUseCase(any()) } just Runs
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // When
            viewModel.onEvent(GroupsUiEvent.DeleteGroup("group-1"))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { deleteGroupUseCase("group-1") }

            collectJob.cancel()
        }

        @Test
        fun `DeleteGroup event handles deletion gracefully`() = runTest(testDispatcher) {
            // Given
            every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup1))
            coEvery { deleteGroupUseCase(any()) } just Runs
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // When - Delete event should not throw
            viewModel.onEvent(GroupsUiEvent.DeleteGroup("group-1"))
            advanceUntilIdle()

            // Then - State should still be valid
            assertFalse(viewModel.uiState.value.isLoading)

            collectJob.cancel()
        }

        @Test
        fun `multiple delete events are handled independently`() = runTest(testDispatcher) {
            // Given
            every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup1, testGroup2))
            coEvery { deleteGroupUseCase(any()) } just Runs
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // When
            viewModel.onEvent(GroupsUiEvent.DeleteGroup("group-1"))
            viewModel.onEvent(GroupsUiEvent.DeleteGroup("group-2"))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { deleteGroupUseCase("group-1") }
            coVerify(exactly = 1) { deleteGroupUseCase("group-2") }

            collectJob.cancel()
        }

        @Test
        fun `DeleteGroup event emits error action when deletion fails`() = runTest(testDispatcher) {
            // Given
            every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup1))
            val exception = RuntimeException("Database error")
            coEvery { deleteGroupUseCase(any()) } throws exception
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // Collect actions in background - Use UnconfinedTestDispatcher here
            val actions = mutableListOf<GroupsUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }
            // No advanceUntilIdle() needed here for the collector start, as Unconfined starts immediately

            // When
            viewModel.onEvent(GroupsUiEvent.DeleteGroup("group-1"))
            advanceUntilIdle() // Still needed to execute the ViewModel's launch block

            // Then
            coVerify(exactly = 1) { deleteGroupUseCase("group-1") }
            assertTrue(
                actions.any { it is GroupsUiAction.ShowDeleteError },
                "Expected ShowDeleteError action"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `DeleteGroup event emits success action when deletion succeeds`() = runTest(testDispatcher) {
            // Given
            every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup1))
            coEvery { deleteGroupUseCase(any()) } just Runs
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // Collect actions in background - Use UnconfinedTestDispatcher here
            val actions = mutableListOf<GroupsUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When
            viewModel.onEvent(GroupsUiEvent.DeleteGroup("group-1"))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { deleteGroupUseCase("group-1") }
            assertTrue(
                actions.any { it is GroupsUiAction.ShowDeleteSuccess },
                "Expected ShowDeleteSuccess action"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }
    }

    @Nested
    inner class ScrollPositionEvent {

        @Test
        fun `ScrollPositionChanged updates scroll state`() = runTest(testDispatcher) {
            // Given
            every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup1))
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // When
            viewModel.onEvent(GroupsUiEvent.ScrollPositionChanged(5, 100))
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertEquals(5, state.scrollPosition)
            assertEquals(100, state.scrollOffset)

            collectJob.cancel()
        }

        @Test
        fun `scroll position is preserved across state updates`() = runTest(testDispatcher) {
            // Given
            every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup1))
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // When - Set scroll position
            viewModel.onEvent(GroupsUiEvent.ScrollPositionChanged(3, 50))
            advanceUntilIdle()

            // Then - Scroll position should be preserved
            val state = viewModel.uiState.value
            assertEquals(3, state.scrollPosition)
            assertEquals(50, state.scrollOffset)

            collectJob.cancel()
        }
    }

    @Nested
    inner class ArchiveGroupEvent {

        @Test
        fun `ArchiveGroup event calls use case with correct groupId`() = runTest(testDispatcher) {
            // Given
            every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup1, testGroup2))
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            // When
            viewModel.onEvent(GroupsUiEvent.ArchiveGroup("group-1"))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { archiveGroupUseCase("group-1") }

            collectJob.cancel()
        }

        @Test
        fun `ArchiveGroup event emits success action`() = runTest(testDispatcher) {
            // Given
            coEvery { archiveGroupUseCase(any()) } returns Result.success(Unit)
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val actions = mutableListOf<GroupsUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When
            viewModel.onEvent(GroupsUiEvent.ArchiveGroup("group-1"))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { archiveGroupUseCase("group-1") }
            assertTrue(
                actions.any { it is GroupsUiAction.ShowArchiveSuccess },
                "Expected ShowArchiveSuccess action"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `ArchiveGroup event emits error action when archiving fails`() = runTest(testDispatcher) {
            // Given
            coEvery { archiveGroupUseCase(any()) } returns Result.failure(Exception("Archive failed"))
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val actions = mutableListOf<GroupsUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When
            viewModel.onEvent(GroupsUiEvent.ArchiveGroup("group-1"))
            advanceUntilIdle()

            // Then
            assertTrue(
                actions.any { it is GroupsUiAction.ShowArchiveError },
                "Expected ShowArchiveError action"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `ArchiveGroup event handles non-existent group gracefully`() = runTest(testDispatcher) {
            // Given
            coEvery { archiveGroupUseCase(any()) } returns Result.failure(IllegalArgumentException("Group not found"))
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val actions = mutableListOf<GroupsUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When - Should not throw
            viewModel.onEvent(GroupsUiEvent.ArchiveGroup("non-existent"))
            advanceUntilIdle()

            // Then - Error is propagated as ShowArchiveError, not thrown
            assertTrue(
                actions.any { it is GroupsUiAction.ShowArchiveError },
                "Expected ShowArchiveError action instead of crash"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }
    }

    @Nested
    inner class LeaveGroupEvent {

        @Test
        fun `LeaveGroup event calls use case with correct groupId`() = runTest(testDispatcher) {
            every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup1, testGroup2))
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.onEvent(GroupsUiEvent.LeaveGroup("group-1"))
            advanceUntilIdle()

            coVerify(exactly = 1) { leaveGroupUseCase("group-1") }

            collectJob.cancel()
        }

        @Test
        fun `LeaveGroup event emits success action`() = runTest(testDispatcher) {
            coEvery { leaveGroupUseCase(any()) } returns Result.success(Unit)
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val actions = mutableListOf<GroupsUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.onEvent(GroupsUiEvent.LeaveGroup("group-1"))
            advanceUntilIdle()

            coVerify(exactly = 1) { leaveGroupUseCase("group-1") }
            assertTrue(
                actions.any { it is GroupsUiAction.ShowLeaveSuccess },
                "Expected ShowLeaveSuccess action"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `LeaveGroup event emits error action when leaving fails`() = runTest(testDispatcher) {
            coEvery { leaveGroupUseCase(any()) } returns Result.failure(Exception("non_zero_balance"))
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val actions = mutableListOf<GroupsUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.onEvent(GroupsUiEvent.LeaveGroup("group-1"))
            advanceUntilIdle()

            assertTrue(
                actions.any { it is GroupsUiAction.ShowLeaveError },
                "Expected ShowLeaveError action"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }
    }

    @Nested
    inner class LoadGroupsEvent {

        @Test
        fun `LoadGroups event is a no-op with stateIn`() = runTest(testDispatcher) {
            // Given - Data loads automatically via stateIn
            every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup1))
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val initialState = viewModel.uiState.value

            // When - LoadGroups should have no effect
            viewModel.onEvent(GroupsUiEvent.LoadGroups)
            advanceUntilIdle()

            // Then - State should remain the same
            assertEquals(initialState.groups.size, viewModel.uiState.value.groups.size)

            collectJob.cancel()
        }
    }
}
