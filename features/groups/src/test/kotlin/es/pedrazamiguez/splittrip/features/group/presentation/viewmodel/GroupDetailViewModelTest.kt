package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.group.ArchiveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.GroupDetailUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.GroupDetailUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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

        // Default stubs
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()
        every { getGroupSubunitsFlowUseCase(any()) } returns flowOf(emptyList())
        every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup))
        every { groupUiMapper.toGroupUiModel(any(), any()) } returns testGroupUiModel
        every { observeGroupUseCase(any()) } returns flowOf(testGroup)
        every { authenticationService.currentUserId() } returns "user-1"

        viewModel = createViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = GroupDetailViewModel(
        getGroupSubunitsFlowUseCase = getGroupSubunitsFlowUseCase,
        getUserGroupsFlowUseCase = getUserGroupsFlowUseCase,
        getMemberProfilesUseCase = getMemberProfilesUseCase,
        groupUiMapper = groupUiMapper,
        observeGroupUseCase = observeGroupUseCase,
        authenticationService = authenticationService,
        archiveGroupUseCase = archiveGroupUseCase
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

            // Flow filtered blank IDs — state stays at initial loading
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

        @Test
        fun `calling setGroupId with same id does not re-trigger load`() = runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            viewModel.setGroupId(testGroupId) // same id — guarded by if check
            advanceUntilIdle()

            // Use case should only be called once (only one unique id was set)
            io.mockk.verify(exactly = 1) { observeGroupUseCase(testGroupId) }

            collectJob.cancel()
        }

        @Test
        fun `calling setGroupId with different id triggers new load`() = runTest(testDispatcher) {
            val secondGroupId = "group-456"
            val secondGroup = testGroup.copy(id = secondGroupId, name = "Winter Trip")
            val secondGroupUiModel = testGroupUiModel.copy(id = secondGroupId, name = "Winter Trip")
            every { observeGroupUseCase(secondGroupId) } returns flowOf(secondGroup)
            every { getGroupSubunitsFlowUseCase(secondGroupId) } returns flowOf(emptyList())
            every { groupUiMapper.toGroupUiModel(secondGroup, any()) } returns secondGroupUiModel

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            viewModel.setGroupId(secondGroupId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(secondGroupUiModel, state.group)

            collectJob.cancel()
        }
    }

    @Nested
    inner class SuccessPath {

        @Test
        fun `loads group with member profiles and emits loaded state`() = runTest(testDispatcher) {
            val memberProfiles = mapOf(
                "user-1" to User(userId = "user-1", email = "a@b.com", displayName = "Alice"),
                "user-2" to User(userId = "user-2", email = "c@d.com", displayName = "Bob")
            )
            coEvery { getMemberProfilesUseCase(testGroup.members) } returns memberProfiles

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            coVerify { getMemberProfilesUseCase(testGroup.members) }
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.group)

            collectJob.cancel()
        }

        @Test
        fun `subunit count is reflected in state`() = runTest(testDispatcher) {
            val subunits = listOf(
                Subunit(id = "sub-1", groupId = testGroupId),
                Subunit(id = "sub-2", groupId = testGroupId),
                Subunit(id = "sub-3", groupId = testGroupId)
            )
            every { getGroupSubunitsFlowUseCase(testGroupId) } returns flowOf(subunits)

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            assertEquals(3, viewModel.uiState.value.subunitsCount)

            collectJob.cancel()
        }

        @Test
        fun `subunit count updates when flow emits new list`() = runTest(testDispatcher) {
            val subunitsFlow = MutableSharedFlow<List<Subunit>>()
            every { getGroupSubunitsFlowUseCase(testGroupId) } returns subunitsFlow

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId(testGroupId)
            // Advance to let the ViewModel subscribe to subunitsFlow before emitting
            advanceUntilIdle()

            subunitsFlow.emit(listOf(Subunit(id = "sub-1", groupId = testGroupId)))
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.subunitsCount)

            subunitsFlow.emit(
                listOf(
                    Subunit(id = "sub-1", groupId = testGroupId),
                    Subunit(id = "sub-2", groupId = testGroupId)
                )
            )
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.subunitsCount)

            collectJob.cancel()
        }

        @Test
        fun `group with no members skips member profile fetch`() = runTest(testDispatcher) {
            val groupWithNoMembers = testGroup.copy(members = emptyList())
            every { observeGroupUseCase(testGroupId) } returns flowOf(groupWithNoMembers)

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            coVerify(exactly = 0) { getMemberProfilesUseCase(any()) }
            assertFalse(viewModel.uiState.value.isLoading)

            collectJob.cancel()
        }

        @Test
        fun `isOnlyGroup is true when user has only one group`() = runTest(testDispatcher) {
            every { getUserGroupsFlowUseCase() } returns flowOf(listOf(testGroup))

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isOnlyGroup)

            collectJob.cancel()
        }

        @Test
        fun `isOnlyGroup is false when user has multiple groups`() = runTest(testDispatcher) {
            val groups = listOf(testGroup, testGroup.copy(id = "other-group"))
            every { getUserGroupsFlowUseCase() } returns flowOf(groups)

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isOnlyGroup)

            collectJob.cancel()
        }
    }

    @Nested
    inner class ErrorPaths {

        @Test
        fun `observeGroupUseCase throwing emits error state`() = runTest(testDispatcher) {
            every { observeGroupUseCase(testGroupId) } returns flow { throw IOException("Network error") }

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.hasError)
            assertNull(state.group)

            collectJob.cancel()
        }

        @Test
        fun `getMemberProfilesUseCase throwing continues with empty profiles`() = runTest(testDispatcher) {
            coEvery { getMemberProfilesUseCase(any()) } throws IOException("Profile fetch failed")
            every { groupUiMapper.toGroupUiModel(testGroup, emptyMap()) } returns testGroupUiModel

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            // ViewModel catches the exception and falls back to emptyMap()
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.group)
            assertFalse(state.hasError)

            collectJob.cancel()
        }

        @Test
        fun `subunit flow error emits fallback state with group but zero subunits`() = runTest(testDispatcher) {
            every { getGroupSubunitsFlowUseCase(testGroupId) } returns flow {
                throw IllegalStateException("Subunit DB error")
            }

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            // Inner .catch emits fallback state with the group still set
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.group)
            assertEquals(0, state.subunitsCount)
            assertFalse(state.hasError)

            collectJob.cancel()
        }
    }

    @Nested
    inner class FatalErrorPath {

        @Test
        fun `mapper throwing triggers outer catch emitting ShowError action and error state`() =
            runTest(testDispatcher) {
                every {
                    groupUiMapper.toGroupUiModel(any(), any())
                } throws IllegalStateException("Mapper failure")

                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

                val actions = mutableListOf<GroupDetailUiAction>()
                val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.actions.collect { actions.add(it) }
                }

                viewModel.setGroupId(testGroupId)
                advanceUntilIdle()

                assertTrue(
                    actions.any { it is GroupDetailUiAction.ShowError },
                    "Expected ShowError action to be emitted"
                )
                val state = viewModel.uiState.value
                assertFalse(state.isLoading)
                assertTrue(state.hasError)

                actionsJob.cancel()
                collectJob.cancel()
            }
    }

    @Nested
    inner class ArchiveFlow {

        @Test
        fun `ArchiveClicked event updates state to show archive confirmation`() = runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(GroupDetailUiEvent.ArchiveClicked)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showArchiveConfirmation)
            collectJob.cancel()
        }

        @Test
        fun `ArchiveCancelled event updates state to hide archive confirmation`() = runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(GroupDetailUiEvent.ArchiveClicked)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showArchiveConfirmation)

            viewModel.onEvent(GroupDetailUiEvent.ArchiveCancelled)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.showArchiveConfirmation)

            collectJob.cancel()
        }

        @Test
        fun `ArchiveConfirmed event success path archives group and updates state`() = runTest(testDispatcher) {
            coEvery { archiveGroupUseCase(testGroupId) } returns Result.success(Unit)

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(GroupDetailUiEvent.ArchiveConfirmed)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.showArchiveConfirmation)
            assertFalse(state.isArchiving)
            coVerify(exactly = 1) { archiveGroupUseCase(testGroupId) }

            collectJob.cancel()
        }

        @Test
        fun `ArchiveConfirmed event failure path emits ShowError action and updates state`() = runTest(testDispatcher) {
            coEvery { archiveGroupUseCase(testGroupId) } returns Result.failure(Exception("Archive failed"))

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setGroupId(testGroupId)
            advanceUntilIdle()

            val actions = mutableListOf<GroupDetailUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.onEvent(GroupDetailUiEvent.ArchiveConfirmed)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.showArchiveConfirmation)
            assertFalse(state.isArchiving)
            assertTrue(actions.any { it is GroupDetailUiAction.ShowError })
            coVerify(exactly = 1) { archiveGroupUseCase(testGroupId) }

            actionsJob.cancel()
            collectJob.cancel()
        }
    }
}
