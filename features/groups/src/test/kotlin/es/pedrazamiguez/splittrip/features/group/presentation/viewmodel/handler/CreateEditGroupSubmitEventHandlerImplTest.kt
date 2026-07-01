package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import es.pedrazamiguez.splittrip.domain.usecase.group.AddGroupMembersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.RemoveGroupMemberUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.UpdateGroupUseCase
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.jupiter.api.Assertions.assertFalse
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
    private lateinit var featureGateService: FeatureGateService
    private lateinit var telemetryTracker: TelemetryTracker
    private lateinit var appConfigService: AppConfigService
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
        featureGateService = mockk(relaxed = true)
        telemetryTracker = mockk(relaxed = true)
        appConfigService = mockk(relaxed = true) {
            every { defaultCurrencyCode } returns MutableStateFlow("EUR")
            every { maxMembersPerGroup } returns MutableStateFlow(10)
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
            removeGroupMemberUseCase = removeGroupMemberUseCase
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
            coEvery { featureGateService.checkLimit(any(), any()) } returns flowOf(
                LimitResult.Blocked(limit = GatedLimit.MAX_GROUPS_COUNT, upgradeRequired = true)
            )
            stateFlow.value = CreateEditGroupUiState(groupName = "New Group", isEditMode = false)

            val actions = mutableListOf<CreateEditGroupUiAction>()
            val collectJob = launch { actionsFlow.collect { actions.add(it) } }

            handler.handleSubmit {}
            advanceUntilIdle()

            assertTrue(actions.any { it is CreateEditGroupUiAction.ShowError })
            collectJob.cancel()
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
}
