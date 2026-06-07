package es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.SubunitShareDistributionService
import es.pedrazamiguez.splittrip.domain.service.impl.SubunitShareDistributionServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.CreateSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.UpdateSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.mapper.SubunitUiMapper
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.MemberUiModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.action.CreateEditSubunitUiAction
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.CreateEditSubunitUiEvent
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitStep
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.collections.immutable.toImmutableList
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("CreateEditSubunitViewModel")
class CreateEditSubunitViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var createSubunitUseCase: CreateSubunitUseCase
    private lateinit var updateSubunitUseCase: UpdateSubunitUseCase
    private lateinit var getGroupByIdUseCase: GetGroupByIdUseCase
    private lateinit var getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var subunitUiMapper: SubunitUiMapper
    private lateinit var shareDistributionService: SubunitShareDistributionService
    private lateinit var viewModel: CreateEditSubunitViewModel

    private val testGroup = Group(
        id = "group-1",
        name = "Trip to Paris",
        members = listOf("user-1", "user-2", "user-3")
    )

    private val testSubunit = Subunit(
        id = "sub-1",
        groupId = "group-1",
        name = "Couple",
        memberIds = listOf("user-1", "user-2"),
        memberShares = mapOf("user-1" to BigDecimal("0.5"), "user-2" to BigDecimal("0.5"))
    )

    private val testMemberProfiles = mapOf(
        "user-1" to User(userId = "user-1", email = "alice@test.com", displayName = "Alice"),
        "user-2" to User(userId = "user-2", email = "bob@test.com", displayName = "Bob"),
        "user-3" to User(userId = "user-3", email = "charlie@test.com", displayName = "Charlie")
    )

    private val testMemberUiModels = listOf(
        MemberUiModel(userId = "user-1", displayName = "Alice"),
        MemberUiModel(userId = "user-2", displayName = "Bob"),
        MemberUiModel(userId = "user-3", displayName = "Charlie")
    ).toImmutableList()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        createSubunitUseCase = mockk()
        updateSubunitUseCase = mockk()
        getGroupByIdUseCase = mockk()
        getGroupSubunitsFlowUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        subunitUiMapper = mockk()
        shareDistributionService = SubunitShareDistributionServiceImpl()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = CreateEditSubunitViewModel(
            createSubunitUseCase = createSubunitUseCase,
            updateSubunitUseCase = updateSubunitUseCase,
            getGroupByIdUseCase = getGroupByIdUseCase,
            getGroupSubunitsFlowUseCase = getGroupSubunitsFlowUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            subunitUiMapper = subunitUiMapper,
            shareDistributionService = shareDistributionService
        )
    }

    private fun setupDefaultMocks(subunits: List<Subunit> = emptyList()) {
        coEvery { getGroupByIdUseCase("group-1") } returns testGroup
        coEvery { getMemberProfilesUseCase(testGroup.members) } returns testMemberProfiles
        every { getGroupSubunitsFlowUseCase("group-1") } returns flowOf(subunits)
        every {
            subunitUiMapper.toMemberUiModelList(any(), any(), any(), any())
        } returns testMemberUiModels
        // Stub formatShareAsPercentage — called by toggleMember/updateMemberShare/edit-mode pre-fill
        every { subunitUiMapper.formatShareAsPercentage(any()) } answers {
            val share = firstArg<BigDecimal>()
            val percent = share.multiply(BigDecimal("100"))
            if (percent.stripTrailingZeros().scale() <= 0) {
                percent.toLong().toString()
            } else {
                percent.toPlainString()
            }
        }
    }

    @Nested
    @DisplayName("Create Mode")
    inner class CreateMode {

        @Test
        fun `initial state is loading`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val state = viewModel.uiState.value
            assertTrue(state.isLoading)
        }

        @Test
        fun `loads empty form after init in create mode`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isEditing)
            assertEquals("", state.name)
            assertTrue(state.selectedMemberIds.isEmpty())
            assertEquals(3, state.availableMembers.size)
            assertEquals(CreateEditSubunitStep.NAME, state.currentStep)

            collectJob.cancel()
        }

        @Test
        fun `UpdateName updates name and clears error`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.UpdateName("Family"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Family", state.name)
            assertNull(state.nameError)

            collectJob.cancel()
        }

        @Test
        fun `ToggleMember adds and removes members`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            // Add a member
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-1"))
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.selectedMemberIds.size)
            assertTrue(viewModel.uiState.value.selectedMemberIds.contains("user-1"))

            // Remove the member
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-1"))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.selectedMemberIds.isEmpty())

            collectJob.cancel()
        }

        @Test
        fun `ToggleMember distributes shares evenly`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            // Toggle two members on
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-1"))
            advanceUntilIdle()
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-2"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2, state.selectedMemberIds.size)
            assertEquals("50", state.memberShares["user-1"])
            assertEquals("50", state.memberShares["user-2"])

            collectJob.cancel()
        }

        @Test
        fun `UpdateMemberShare redistributes remaining to other members`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            // Select two members
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-1"))
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-2"))
            advanceUntilIdle()

            // Update user-1's share to 60%
            viewModel.onEvent(CreateEditSubunitUiEvent.UpdateMemberShare("user-1", "60"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("60", state.memberShares["user-1"])
            assertEquals("40", state.memberShares["user-2"])

            collectJob.cancel()
        }

        @Test
        fun `Save shows name error when name is blank`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.Save)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.nameError)
            assertTrue(state.nameError is UiText.StringResource)
            assertEquals(
                R.string.subunit_error_name_empty,
                (state.nameError as UiText.StringResource).resId
            )

            collectJob.cancel()
        }

        @Test
        fun `Save shows members error when no members selected`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.UpdateName("Family"))
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.Save)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.membersError)
            assertTrue(state.membersError is UiText.StringResource)
            assertEquals(
                R.string.subunit_error_no_members,
                (state.membersError as UiText.StringResource).resId
            )

            collectJob.cancel()
        }

        @Test
        fun `creates subunit and emits success then navigate back`() = runTest(testDispatcher) {
            setupDefaultMocks()
            coEvery { createSubunitUseCase(any(), any()) } returns Result.success("new-sub-id")
            createViewModel()

            val actions = mutableListOf<CreateEditSubunitUiAction>()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.UpdateName("Family"))
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-3"))
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.Save)
            advanceUntilIdle()

            coVerify { createSubunitUseCase("group-1", any()) }
            assertTrue(actions.any { it is CreateEditSubunitUiAction.ShowSuccess })
            assertTrue(actions.any { it is CreateEditSubunitUiAction.NavigateBack })

            collectJob.cancel()
            actionsJob.cancel()
        }

        @Test
        fun `emits error action when save fails`() = runTest(testDispatcher) {
            setupDefaultMocks()
            coEvery { createSubunitUseCase(any(), any()) } returns Result.failure(Exception("Network error"))
            createViewModel()

            val actions = mutableListOf<CreateEditSubunitUiAction>()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.UpdateName("Family"))
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-3"))
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.Save)
            advanceUntilIdle()

            assertTrue(actions.any { it is CreateEditSubunitUiAction.ShowError })
            assertFalse(actions.any { it is CreateEditSubunitUiAction.NavigateBack })

            collectJob.cancel()
            actionsJob.cancel()
        }
    }

    @Nested
    @DisplayName("Wizard Navigation")
    inner class WizardNavigation {

        @Test
        fun `initial step is NAME`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            assertEquals(CreateEditSubunitStep.NAME, viewModel.uiState.value.currentStep)

            collectJob.cancel()
        }

        @Test
        fun `NextStep advances from NAME to MEMBERS`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()

            assertEquals(CreateEditSubunitStep.MEMBERS, viewModel.uiState.value.currentStep)

            collectJob.cancel()
        }

        @Test
        fun `NextStep advances through all steps`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()
            assertEquals(CreateEditSubunitStep.MEMBERS, viewModel.uiState.value.currentStep)

            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()
            assertEquals(CreateEditSubunitStep.SHARES, viewModel.uiState.value.currentStep)

            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()
            assertEquals(CreateEditSubunitStep.REVIEW, viewModel.uiState.value.currentStep)

            collectJob.cancel()
        }

        @Test
        fun `NextStep does not advance past REVIEW`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            // Navigate to REVIEW
            repeat(3) { viewModel.onEvent(CreateEditSubunitUiEvent.NextStep) }
            advanceUntilIdle()
            assertEquals(CreateEditSubunitStep.REVIEW, viewModel.uiState.value.currentStep)

            // Try to go past REVIEW
            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()
            assertEquals(CreateEditSubunitStep.REVIEW, viewModel.uiState.value.currentStep)

            collectJob.cancel()
        }

        @Test
        fun `PreviousStep goes back from MEMBERS to NAME`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()
            assertEquals(CreateEditSubunitStep.MEMBERS, viewModel.uiState.value.currentStep)

            viewModel.onEvent(CreateEditSubunitUiEvent.PreviousStep)
            advanceUntilIdle()
            assertEquals(CreateEditSubunitStep.NAME, viewModel.uiState.value.currentStep)

            collectJob.cancel()
        }

        @Test
        fun `PreviousStep on NAME emits NavigateBack`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val actions = mutableListOf<CreateEditSubunitUiAction>()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            assertEquals(CreateEditSubunitStep.NAME, viewModel.uiState.value.currentStep)

            viewModel.onEvent(CreateEditSubunitUiEvent.PreviousStep)
            advanceUntilIdle()

            assertTrue(actions.any { it is CreateEditSubunitUiAction.NavigateBack })

            collectJob.cancel()
            actionsJob.cancel()
        }

        @Test
        fun `NextStep clears errors`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            // Trigger a name error
            viewModel.onEvent(CreateEditSubunitUiEvent.Save)
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.nameError)

            // NextStep should clear errors
            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.nameError)

            collectJob.cancel()
        }

        @Test
        fun `NextStep blocks on SHARES when share exceeds 100 percent`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            // Select two members and navigate to SHARES
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-1"))
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-2"))
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep) // NAME → MEMBERS
            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep) // MEMBERS → SHARES
            advanceUntilIdle()
            assertEquals(CreateEditSubunitStep.SHARES, viewModel.uiState.value.currentStep)

            // Set an out-of-range share (306 %)
            viewModel.onEvent(CreateEditSubunitUiEvent.UpdateMemberShare("user-1", "306"))
            advanceUntilIdle()

            // Attempt to advance — should stay on SHARES with error
            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()

            assertEquals(CreateEditSubunitStep.SHARES, viewModel.uiState.value.currentStep)
            assertNotNull(viewModel.uiState.value.sharesError)
            assertTrue(viewModel.uiState.value.sharesError is UiText.StringResource)
            assertEquals(
                R.string.subunit_error_share_out_of_range,
                (viewModel.uiState.value.sharesError as UiText.StringResource).resId
            )

            collectJob.cancel()
        }

        @Test
        fun `NextStep blocks on SHARES when shares do not sum to 100 percent`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            // Select two members and navigate to SHARES
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-1"))
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-2"))
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep) // NAME → MEMBERS
            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep) // MEMBERS → SHARES
            advanceUntilIdle()

            // Lock user-2 at 50% before editing user-1 so redistribution
            // returns empty (no unlocked others) and user-2 keeps its value.
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleShareLock("user-2"))
            advanceUntilIdle()

            // Now set user-1 to 30% — user-2 stays locked at 50% → total = 80%
            viewModel.onEvent(CreateEditSubunitUiEvent.UpdateMemberShare("user-1", "30"))
            advanceUntilIdle()

            // Attempt to advance — should stay on SHARES with sum error
            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()

            assertEquals(CreateEditSubunitStep.SHARES, viewModel.uiState.value.currentStep)
            assertNotNull(viewModel.uiState.value.sharesError)
            assertTrue(viewModel.uiState.value.sharesError is UiText.StringResource)
            assertEquals(
                R.string.subunit_error_shares_dont_sum,
                (viewModel.uiState.value.sharesError as UiText.StringResource).resId
            )

            collectJob.cancel()
        }

        @Test
        fun `NextStep advances from SHARES when shares are valid`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            // Select two members — shares auto-distribute to 50/50
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-1"))
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-2"))
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep) // NAME → MEMBERS
            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep) // MEMBERS → SHARES
            advanceUntilIdle()
            assertEquals(CreateEditSubunitStep.SHARES, viewModel.uiState.value.currentStep)

            // Shares are valid (50/50) — should advance to REVIEW
            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()

            assertEquals(CreateEditSubunitStep.REVIEW, viewModel.uiState.value.currentStep)
            assertNull(viewModel.uiState.value.sharesError)

            collectJob.cancel()
        }

        @Test
        fun `Editing share clears shares error`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            // Select two members, navigate to SHARES, enter bad value
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-1"))
            viewModel.onEvent(CreateEditSubunitUiEvent.ToggleMember("user-2"))
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep) // NAME → MEMBERS
            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep) // MEMBERS → SHARES
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.UpdateMemberShare("user-1", "306"))
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.sharesError)

            // Editing a share should clear the error
            viewModel.onEvent(CreateEditSubunitUiEvent.UpdateMemberShare("user-1", "60"))
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.sharesError)

            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("JumpToStep")
    inner class JumpToStep {

        @Test
        fun `JumpToStep navigates to the target step`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            // Advance to MEMBERS
            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()
            assertEquals(CreateEditSubunitStep.MEMBERS, viewModel.uiState.value.currentStep)

            // When — jump back to step 0 (NAME)
            viewModel.onEvent(CreateEditSubunitUiEvent.JumpToStep(0))
            advanceUntilIdle()

            // Then
            assertEquals(CreateEditSubunitStep.NAME, viewModel.uiState.value.currentStep)
            collectJob.cancel()
        }

        @Test
        fun `JumpToStep clears all step-level errors`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            // Trigger a name error
            viewModel.onEvent(CreateEditSubunitUiEvent.Save)
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.nameError)

            // Advance to members step (which clears errors via NextStep)
            viewModel.onEvent(CreateEditSubunitUiEvent.UpdateName("My Subunit"))
            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()

            // When — jump back to step 0
            viewModel.onEvent(CreateEditSubunitUiEvent.JumpToStep(0))
            advanceUntilIdle()

            // Then — all errors cleared
            assertNull(viewModel.uiState.value.nameError)
            assertNull(viewModel.uiState.value.membersError)
            assertNull(viewModel.uiState.value.sharesError)
            collectJob.cancel()
        }

        @Test
        fun `JumpToStep is a no-op for out-of-bounds index`() = runTest(testDispatcher) {
            setupDefaultMocks()
            createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", null)
            advanceUntilIdle()

            // Advance one step
            viewModel.onEvent(CreateEditSubunitUiEvent.NextStep)
            advanceUntilIdle()
            val stepBefore = viewModel.uiState.value.currentStep

            // When — wildly out of bounds
            viewModel.onEvent(CreateEditSubunitUiEvent.JumpToStep(999))
            advanceUntilIdle()

            // Then — step unchanged
            assertEquals(stepBefore, viewModel.uiState.value.currentStep)
            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("Edit Mode")
    inner class EditMode {

        @Test
        fun `loads pre-filled form in edit mode`() = runTest(testDispatcher) {
            setupDefaultMocks(subunits = listOf(testSubunit))
            createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.init("group-1", "sub-1")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isEditing)
            assertEquals("Couple", state.name)
            assertEquals(2, state.selectedMemberIds.size)
            assertTrue(state.selectedMemberIds.contains("user-1"))
            assertTrue(state.selectedMemberIds.contains("user-2"))

            collectJob.cancel()
        }

        @Test
        fun `updates subunit and emits success then navigate back`() = runTest(testDispatcher) {
            setupDefaultMocks(subunits = listOf(testSubunit))
            coEvery { updateSubunitUseCase(any(), any()) } returns Result.success(Unit)
            createViewModel()

            val actions = mutableListOf<CreateEditSubunitUiAction>()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            viewModel.init("group-1", "sub-1")
            advanceUntilIdle()

            viewModel.onEvent(CreateEditSubunitUiEvent.Save)
            advanceUntilIdle()

            coVerify { updateSubunitUseCase("group-1", any()) }
            assertTrue(actions.any { it is CreateEditSubunitUiAction.ShowSuccess })
            assertTrue(actions.any { it is CreateEditSubunitUiAction.NavigateBack })

            collectJob.cancel()
            actionsJob.cancel()
        }
    }
}
