package es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.DeleteSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.subunit.presentation.mapper.SubunitUiMapper
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.MemberShareUiModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.SubunitUiModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.action.SubunitManagementUiAction
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.SubunitManagementUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
class SubunitManagementViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase
    private lateinit var deleteSubunitUseCase: DeleteSubunitUseCase
    private lateinit var getGroupByIdUseCase: GetGroupByIdUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var subunitUiMapper: SubunitUiMapper
    private lateinit var observeGroupUseCase: ObserveGroupUseCase
    private lateinit var viewModel: SubunitManagementViewModel

    private val testGroup = Group(
        id = "group-1",
        name = "Group One",
        members = listOf("user-1", "user-2"),
        createdBy = "user-1"
    )

    private val testMemberProfiles = mapOf(
        "user-1" to User("user-1", "alice@test.com", "Alice"),
        "user-2" to User("user-2", "bob@test.com", "Bob")
    )

    private val testSubunit = Subunit(
        id = "sub-1",
        groupId = "group-1",
        name = "Subunit One",
        memberIds = listOf("user-1", "user-2"),
        memberShares = mapOf("user-1" to BigDecimal("0.6"), "user-2" to BigDecimal("0.4"))
    )

    private val testSubunitUiModel = SubunitUiModel(
        id = "sub-1",
        name = "Subunit One",
        memberShares = persistentListOf(
            MemberShareUiModel("Alice", "60%"),
            MemberShareUiModel("Bob", "40%")
        ),
        memberCount = "2 members"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getGroupSubunitsFlowUseCase = mockk()
        deleteSubunitUseCase = mockk()
        getGroupByIdUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        subunitUiMapper = mockk()
        observeGroupUseCase = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = SubunitManagementViewModel(
            getGroupSubunitsFlowUseCase = getGroupSubunitsFlowUseCase,
            deleteSubunitUseCase = deleteSubunitUseCase,
            getGroupByIdUseCase = getGroupByIdUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            subunitUiMapper = subunitUiMapper,
            observeGroupUseCase = observeGroupUseCase
        )
    }

    private fun setupDefaultMocks(subunits: List<Subunit> = listOf(testSubunit)) {
        coEvery { getGroupByIdUseCase("group-1") } returns testGroup
        coEvery { getMemberProfilesUseCase(testGroup.members) } returns testMemberProfiles
        every { getGroupSubunitsFlowUseCase("group-1") } returns flowOf(subunits)
        every { observeGroupUseCase("group-1") } returns flowOf(testGroup)
        every {
            subunitUiMapper.toSubunitUiModelList(any(), any())
        } returns listOf(testSubunitUiModel).toImmutableList()
    }

    @Nested
    inner class StateManagement {

        @Test
        fun `initial state is loading`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val state = viewModel.uiState.value
            assertTrue(state.isLoading)
            assertTrue(state.subunits.isEmpty())
        }

        @Test
        fun `emits subunits after setting groupId`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId("group-1")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals("group-1", state.groupId)
            assertEquals("Group One", state.groupName)
            assertEquals(1, state.subunits.size)
            assertEquals("Subunit One", state.subunits[0].name)

            collectJob.cancel()
        }

        @Test
        fun `does not re-emit when same groupId is set`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setGroupId("group-1")
            viewModel.setGroupId("group-1")
            advanceUntilIdle()

            coVerify(exactly = 1) { getGroupByIdUseCase("group-1") }

            collectJob.cancel()
        }
    }

    @Nested
    inner class NavigationEvents {

        @Test
        fun `CreateSubunit emits NavigateToCreateSubunit action`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val actions = mutableListOf<SubunitManagementUiAction>()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.setGroupId("group-1")
            advanceUntilIdle()

            viewModel.onEvent(SubunitManagementUiEvent.CreateSubunit)
            advanceUntilIdle()

            assertTrue(
                actions.any {
                    it is SubunitManagementUiAction.NavigateToCreateSubunit && it.groupId == "group-1"
                }
            )

            collectJob.cancel()
            actionsJob.cancel()
        }

        @Test
        fun `EditSubunit emits NavigateToEditSubunit action`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val actions = mutableListOf<SubunitManagementUiAction>()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.setGroupId("group-1")
            advanceUntilIdle()

            viewModel.onEvent(SubunitManagementUiEvent.EditSubunit("sub-1"))
            advanceUntilIdle()

            assertTrue(
                actions.any {
                    it is SubunitManagementUiAction.NavigateToEditSubunit &&
                        it.groupId == "group-1" &&
                        it.subunitId == "sub-1"
                }
            )

            collectJob.cancel()
            actionsJob.cancel()
        }
    }

    @Nested
    inner class DeleteSubunit {

        @Test
        fun `ConfirmDeleteSubunit calls use case and emits success`() = runTest(testDispatcher) {
            setupDefaultMocks()
            coEvery { deleteSubunitUseCase(any(), any()) } returns Unit
            createViewModel()

            val actions = mutableListOf<SubunitManagementUiAction>()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.setGroupId("group-1")
            advanceUntilIdle()

            viewModel.onEvent(SubunitManagementUiEvent.ConfirmDeleteSubunit("sub-1"))
            advanceUntilIdle()

            coVerify { deleteSubunitUseCase("group-1", "sub-1") }
            assertTrue(actions.any { it is SubunitManagementUiAction.ShowSuccess })

            collectJob.cancel()
            actionsJob.cancel()
        }

        @Test
        fun `ConfirmDeleteSubunit emits error action when deletion fails`() = runTest(testDispatcher) {
            setupDefaultMocks()
            coEvery { deleteSubunitUseCase(any(), any()) } throws RuntimeException("Delete failed")
            createViewModel()

            val actions = mutableListOf<SubunitManagementUiAction>()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.setGroupId("group-1")
            advanceUntilIdle()

            viewModel.onEvent(SubunitManagementUiEvent.ConfirmDeleteSubunit("sub-1"))
            advanceUntilIdle()

            assertTrue(actions.any { it is SubunitManagementUiAction.ShowError })

            collectJob.cancel()
            actionsJob.cancel()
        }
    }
}
