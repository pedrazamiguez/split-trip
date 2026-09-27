package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import es.pedrazamiguez.splittrip.domain.usecase.group.AddGroupMembersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.RemoveGroupMemberUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.UpdateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetSelectedGroupUseCase
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateEditGroupSubmitEventHandlerImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var createGroupUseCase: CreateGroupUseCase
    private lateinit var updateGroupUseCase: UpdateGroupUseCase
    private lateinit var addGroupMembersUseCase: AddGroupMembersUseCase
    private lateinit var removeGroupMemberUseCase: RemoveGroupMemberUseCase
    private lateinit var getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase
    private lateinit var setSelectedGroupUseCase: SetSelectedGroupUseCase
    private lateinit var getSelectedGroupIdUseCase: GetSelectedGroupIdUseCase
    private lateinit var featureGateService: FeatureGateService
    private lateinit var telemetryTracker: TelemetryTracker
    private lateinit var appConfigService: AppConfigService
    private lateinit var authenticationService: AuthenticationService
    private lateinit var stateFlow: MutableStateFlow<CreateEditGroupUiState>
    private lateinit var actionsFlow: MutableSharedFlow<CreateEditGroupUiAction>
    private lateinit var handler: CreateEditGroupSubmitEventHandlerImpl

    private val testGroup = Group(
        id = "group-123",
        name = "Japan Trip",
        description = "A trip to Japan",
        currency = "JPY",
        extraCurrencies = listOf("USD"),
        members = listOf("user-1", "user-2")
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        createGroupUseCase = mockk(relaxed = true)
        updateGroupUseCase = mockk(relaxed = true)
        addGroupMembersUseCase = mockk(relaxed = true)
        removeGroupMemberUseCase = mockk(relaxed = true)
        getUserGroupsFlowUseCase = mockk(relaxed = true)
        setSelectedGroupUseCase = mockk(relaxed = true)
        getSelectedGroupIdUseCase = mockk(relaxed = true) {
            every { this@mockk.invoke() } returns flowOf(null)
        }
        featureGateService = mockk(relaxed = true)
        telemetryTracker = mockk(relaxed = true)
        appConfigService = mockk(relaxed = true) {
            every { defaultCurrencyCode } returns MutableStateFlow("EUR")
            every { maxMembersPerGroup } returns MutableStateFlow(10)
        }
        authenticationService = mockk(relaxed = true) {
            every { currentUserId() } returns "current-user"
        }
        stateFlow = MutableStateFlow(CreateEditGroupUiState(groupName = "My Trip"))
        actionsFlow = MutableSharedFlow(replay = 1)
        handler = CreateEditGroupSubmitEventHandlerImpl(
            createGroupUseCase = createGroupUseCase,
            updateGroupUseCase = updateGroupUseCase,
            getUserGroupsFlowUseCase = getUserGroupsFlowUseCase,
            featureGateService = featureGateService,
            telemetryTracker = telemetryTracker,
            appConfigService = appConfigService,
            addGroupMembersUseCase = addGroupMembersUseCase,
            removeGroupMemberUseCase = removeGroupMemberUseCase,
            setSelectedGroupUseCase = setSelectedGroupUseCase,
            getSelectedGroupIdUseCase = getSelectedGroupIdUseCase,
            authenticationService = authenticationService
        )
        handler.bind(stateFlow, actionsFlow, kotlinx.coroutines.MainScope())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    inner class Validation {

        @Test
        fun `does not submit when name is blank`() = runTest(testDispatcher) {
            stateFlow.value = CreateEditGroupUiState(groupName = "")

            handler.handleSubmit {}
            advanceUntilIdle()

            coVerify(exactly = 0) { createGroupUseCase(any(), any()) }
            assertFalse(stateFlow.value.isNameValid)
        }
    }

    @Nested
    inner class CreateMode {

        @BeforeEach
        fun setUpCreate() {
            every { getUserGroupsFlowUseCase() } returns flowOf(emptyList())
            coEvery { featureGateService.checkLimit(any(), any()) } returns flowOf(LimitResult.Allowed)
        }

        @Test
        fun `creates group on submit when limits allowed`() = runTest(testDispatcher) {
            coEvery { createGroupUseCase(any(), any()) } returns Result.success("new-group-id")
            stateFlow.value = CreateEditGroupUiState(groupName = "New Group", isEditMode = false)

            handler.handleSubmit {}
            advanceUntilIdle()

            coVerify(exactly = 1) { createGroupUseCase(any(), any()) }
        }

        @Test
        fun `emits ShowSuccess on successful create`() = runTest(testDispatcher) {
            coEvery { createGroupUseCase(any(), any()) } returns Result.success("new-group-id")
            stateFlow.value = CreateEditGroupUiState(groupName = "New Group", isEditMode = false)

            val actions = mutableListOf<CreateEditGroupUiAction>()
            val collectJob = launch { actionsFlow.collect { actions.add(it) } }

            handler.handleSubmit {}
            advanceUntilIdle()

            assertTrue(actions.any { it is CreateEditGroupUiAction.ShowSuccess })
            collectJob.cancel()
        }

        @Test
        fun `emits ShowError on failed create`() = runTest(testDispatcher) {
            coEvery { createGroupUseCase(any(), any()) } returns Result.failure(RuntimeException("fail"))
            stateFlow.value = CreateEditGroupUiState(groupName = "New Group", isEditMode = false)

            val actions = mutableListOf<CreateEditGroupUiAction>()
            val collectJob = launch { actionsFlow.collect { actions.add(it) } }

            handler.handleSubmit {}
            advanceUntilIdle()

            assertTrue(actions.any { it is CreateEditGroupUiAction.ShowError })
            collectJob.cancel()
        }

        @Test
        fun `emits ShowError when group limit is blocked`() = runTest(testDispatcher) {
            coEvery { featureGateService.checkLimit(any(), any(), any()) } returns flowOf(
                LimitResult.Blocked(limit = GatedLimit.MAX_OWNED_GROUPS_COUNT, upgradeRequired = true)
            )
            stateFlow.value = CreateEditGroupUiState(groupName = "New Group", isEditMode = false)

            val actions = mutableListOf<CreateEditGroupUiAction>()
            val collectJob = launch { actionsFlow.collect { actions.add(it) } }

            handler.handleSubmit {}
            advanceUntilIdle()

            assertTrue(actions.any { it is CreateEditGroupUiAction.ShowError })
            assertTrue(stateFlow.value.showUpgradeDialog)
            assertEquals(
                UiText.StringResource(DesignSystemR.string.upgrade_dialog_title),
                stateFlow.value.upgradeDialogTitle
            )
            assertEquals(
                UiText.StringResource(R.string.group_error_limit_groups_exceeded),
                stateFlow.value.upgradeDialogMessage
            )
            collectJob.cancel()
        }

        @Test
        fun `does not show upgrade dialog when group limit has no upgrade required`() = runTest(testDispatcher) {
            coEvery { featureGateService.checkLimit(any(), any(), any()) } returns flowOf(
                LimitResult.Blocked(limit = GatedLimit.MAX_OWNED_GROUPS_COUNT, upgradeRequired = false)
            )
            stateFlow.value = CreateEditGroupUiState(groupName = "New Group", isEditMode = false)

            val actions = mutableListOf<CreateEditGroupUiAction>()
            val collectJob = launch { actionsFlow.collect { actions.add(it) } }

            handler.handleSubmit {}
            advanceUntilIdle()

            assertTrue(actions.any { it is CreateEditGroupUiAction.ShowError })
            assertFalse(stateFlow.value.showUpgradeDialog)
            assertNull(stateFlow.value.upgradeDialogTitle)
            assertNull(stateFlow.value.upgradeDialogMessage)
            collectJob.cancel()
        }

        @Test
        fun `participating in other groups does not block creating an owned group`() = runTest(testDispatcher) {
            val participatingGroups = listOf(
                Group(id = "g1", createdBy = "other-user-1"),
                Group(id = "g2", createdBy = "other-user-2"),
                Group(id = "g3", createdBy = "other-user-3")
            )
            every { getUserGroupsFlowUseCase() } returns flowOf(participatingGroups)
            every { authenticationService.currentUserId() } returns "current-user"
            coEvery {
                featureGateService.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 0)
            } returns flowOf(LimitResult.Allowed)
            coEvery {
                featureGateService.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, any(), groupId = null)
            } returns flowOf(LimitResult.Allowed)
            coEvery { createGroupUseCase(any(), any()) } returns Result.success("new-group-id")

            stateFlow.value = CreateEditGroupUiState(groupName = "My New Group", isEditMode = false)

            handler.handleSubmit {}
            advanceUntilIdle()

            coVerify(exactly = 1) { featureGateService.checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, 0) }
            coVerify(exactly = 1) { createGroupUseCase(any(), any()) }
        }
    }

    @Nested
    @org.junit.jupiter.api.DisplayName("createGroup — auto-selection")
    inner class AutoSelection {

        @BeforeEach
        fun setUpCreate() {
            every { getUserGroupsFlowUseCase() } returns flowOf(emptyList())
            coEvery { featureGateService.checkLimit(any(), any()) } returns flowOf(LimitResult.Allowed)
        }

        @Test
        fun `auto-selects newly created group on success`() = runTest(testDispatcher) {
            coEvery { createGroupUseCase(any(), any()) } returns Result.success("new-group-id")
            stateFlow.value = CreateEditGroupUiState(groupName = "My Trip", selectedCurrency = null, isEditMode = false)

            handler.handleSubmit {}
            advanceUntilIdle()

            coVerify(exactly = 1) { setSelectedGroupUseCase("new-group-id", "My Trip", "EUR") }
        }

        @Test
        fun `does not auto-select when group creation fails`() = runTest(testDispatcher) {
            coEvery { createGroupUseCase(any(), any()) } returns Result.failure(RuntimeException("fail"))
            stateFlow.value = CreateEditGroupUiState(groupName = "My Trip", isEditMode = false)

            handler.handleSubmit {}
            advanceUntilIdle()

            coVerify(exactly = 0) { setSelectedGroupUseCase(any(), any(), any()) }
        }
    }

    @Nested
    inner class EditMode {

        @BeforeEach
        fun setUpEdit() {
            handler.setInitialGroup(testGroup)
            stateFlow.value = CreateEditGroupUiState(
                groupName = "Updated Trip",
                isEditMode = true,
                groupId = "group-123"
            )
        }

        @Test
        fun `updates group on submit in edit mode`() = runTest(testDispatcher) {
            coEvery { updateGroupUseCase(any()) } returns Result.success(Unit)

            handler.handleSubmit {}
            advanceUntilIdle()

            coVerify(exactly = 1) { updateGroupUseCase(match { it.id == "group-123" && it.name == "Updated Trip" }) }
        }

        @Test
        fun `emits ShowSuccess on successful update`() = runTest(testDispatcher) {
            coEvery { updateGroupUseCase(any()) } returns Result.success(Unit)

            val actions = mutableListOf<CreateEditGroupUiAction>()
            val collectJob = launch { actionsFlow.collect { actions.add(it) } }

            handler.handleSubmit {}
            advanceUntilIdle()

            assertTrue(actions.any { it is CreateEditGroupUiAction.ShowSuccess })
            collectJob.cancel()
        }

        @Test
        fun `emits ShowError on failed update`() = runTest(testDispatcher) {
            coEvery { updateGroupUseCase(any()) } returns Result.failure(RuntimeException("update failed"))

            val actions = mutableListOf<CreateEditGroupUiAction>()
            val collectJob = launch { actionsFlow.collect { actions.add(it) } }

            handler.handleSubmit {}
            advanceUntilIdle()

            assertTrue(actions.any { it is CreateEditGroupUiAction.ShowError })
            collectJob.cancel()
        }

        @Test
        fun `does not call create use case in edit mode`() = runTest(testDispatcher) {
            coEvery { updateGroupUseCase(any()) } returns Result.success(Unit)

            handler.handleSubmit {}
            advanceUntilIdle()

            coVerify(exactly = 0) { createGroupUseCase(any(), any()) }
        }

        @Test
        fun `tracks no telemetry event on group update`() = runTest(testDispatcher) {
            coEvery { updateGroupUseCase(any()) } returns Result.success(Unit)

            handler.handleSubmit {}
            advanceUntilIdle()

            coVerify(exactly = 0) { telemetryTracker.trackEvent(any(), any()) }
        }

        @Test
        fun `updates selected group preferences when edited group is currently selected`() =
            runTest(testDispatcher) {
                stateFlow.value = stateFlow.value.copy(
                    selectedCurrency = CurrencyUiModel("JPY", "JPY - ¥", 0, "Japanese Yen", "Yen")
                )
                every { getSelectedGroupIdUseCase() } returns flowOf(testGroup.id)
                coEvery { updateGroupUseCase(any()) } returns Result.success(Unit)

                handler.handleSubmit {}
                advanceUntilIdle()

                coVerify(exactly = 1) { setSelectedGroupUseCase(testGroup.id, "Updated Trip", "JPY") }
            }

        @Test
        fun `does not update selected group preferences when edited group is not currently selected`() =
            runTest(testDispatcher) {
                every { getSelectedGroupIdUseCase() } returns flowOf("other-group")
                coEvery { updateGroupUseCase(any()) } returns Result.success(Unit)

                handler.handleSubmit {}
                advanceUntilIdle()

                coVerify(exactly = 0) { setSelectedGroupUseCase(any(), any(), any()) }
            }

        @Test
        fun `blocks group update when adding members exceeds MAX_MEMBERS_PER_GROUP`() = runTest(testDispatcher) {
            handler.setInitialGroup(testGroup)
            val newMember = User(userId = "user-3", email = "user3@test.com")
            stateFlow.value = CreateEditGroupUiState(
                groupName = "Updated Group",
                isEditMode = true,
                selectedMembers = persistentListOf(
                    User(userId = "user-1", email = "user1@test.com"),
                    User(userId = "user-2", email = "user2@test.com"),
                    newMember
                )
            )

            coEvery {
                featureGateService.checkLimit(
                    limit = GatedLimit.MAX_MEMBERS_PER_GROUP,
                    currentCount = 3,
                    groupId = testGroup.id
                )
            } returns flowOf(LimitResult.Blocked(GatedLimit.MAX_MEMBERS_PER_GROUP, upgradeRequired = true))

            val actions = mutableListOf<CreateEditGroupUiAction>()
            val collectJob = launch { actionsFlow.collect { actions.add(it) } }

            handler.handleSubmit {}
            advanceUntilIdle()

            assertTrue(actions.any { it is CreateEditGroupUiAction.ShowError })
            assertTrue(stateFlow.value.showUpgradeDialog)
            assertEquals(
                UiText.StringResource(DesignSystemR.string.upgrade_dialog_title),
                stateFlow.value.upgradeDialogTitle
            )
            assertEquals(
                UiText.StringResource(R.string.group_error_limit_members_exceeded),
                stateFlow.value.upgradeDialogMessage
            )
            coVerify(exactly = 0) { updateGroupUseCase(any()) }
            collectJob.cancel()
        }

        @Test
        fun `blocks update without upgrade dialog when limit has no upgrade required`() = runTest(testDispatcher) {
            handler.setInitialGroup(testGroup)
            val newMember = User(userId = "user-3", email = "user3@test.com")
            stateFlow.value = CreateEditGroupUiState(
                groupName = "Updated Group",
                isEditMode = true,
                selectedMembers = persistentListOf(
                    User(userId = "user-1", email = "user1@test.com"),
                    User(userId = "user-2", email = "user2@test.com"),
                    newMember
                )
            )

            coEvery {
                featureGateService.checkLimit(
                    limit = GatedLimit.MAX_MEMBERS_PER_GROUP,
                    currentCount = 3,
                    groupId = testGroup.id
                )
            } returns flowOf(LimitResult.Blocked(GatedLimit.MAX_MEMBERS_PER_GROUP, upgradeRequired = false))

            val actions = mutableListOf<CreateEditGroupUiAction>()
            val collectJob = launch { actionsFlow.collect { actions.add(it) } }

            handler.handleSubmit {}
            advanceUntilIdle()

            assertTrue(actions.any { it is CreateEditGroupUiAction.ShowError })
            assertFalse(stateFlow.value.showUpgradeDialog)
            assertNull(stateFlow.value.upgradeDialogTitle)
            assertNull(stateFlow.value.upgradeDialogMessage)
            coVerify(exactly = 0) { updateGroupUseCase(any()) }
            collectJob.cancel()
        }
    }

    @Nested
    inner class SetInitialGroup {

        @Test
        fun `setInitialGroup stores group for edit submission`() = runTest(testDispatcher) {
            coEvery { updateGroupUseCase(any()) } returns Result.success(Unit)
            handler.setInitialGroup(testGroup)

            stateFlow.value = CreateEditGroupUiState(
                groupName = "Modified",
                isEditMode = true,
                groupId = testGroup.id
            )

            handler.handleSubmit {}
            advanceUntilIdle()

            coVerify(exactly = 1) { updateGroupUseCase(match { it.id == testGroup.id && it.name == "Modified" }) }
        }
    }

    @Nested
    inner class WhitespaceTrimming {

        @BeforeEach
        fun setUpCreate() {
            every { getUserGroupsFlowUseCase() } returns flowOf(emptyList())
            coEvery { featureGateService.checkLimit(any(), any()) } returns flowOf(LimitResult.Allowed)
            coEvery { createGroupUseCase(any(), any()) } returns Result.success("new-group-id")
        }

        @Test
        fun `createGroup_trimsLeadingAndTrailingWhitespaceFromGroupNameAndDescription`() = runTest(testDispatcher) {
            stateFlow.value = CreateEditGroupUiState(
                groupName = "  Japan Trip  ",
                groupDescription = "  A trip  ",
                isEditMode = false
            )

            handler.handleSubmit {}
            advanceUntilIdle()

            coVerify(exactly = 1) {
                createGroupUseCase(match { it.name == "Japan Trip" && it.description == "A trip" }, any())
            }
        }

        @Test
        fun `createGroup_usesWhitespaceOnlyNameAfterTrimCheck`() = runTest(testDispatcher) {
            stateFlow.value = CreateEditGroupUiState(groupName = "   ", isEditMode = false)

            handler.handleSubmit {}
            advanceUntilIdle()

            coVerify(exactly = 0) { createGroupUseCase(any(), any()) }
        }
    }
}
