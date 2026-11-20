package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewType
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveMemberException
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.impl.EmailValidationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.SearchUsersByEmailUseCase
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateEditGroupMemberEventHandlerImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val searchUsersByEmailUseCase: SearchUsersByEmailUseCase = mockk()
    private val emailValidationService = EmailValidationServiceImpl()
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase = mockk()
    private val authenticationService: AuthenticationService = mockk()
    private val telemetryTracker: TelemetryTracker = mockk(relaxed = true)

    private lateinit var stateFlow: MutableStateFlow<CreateEditGroupUiState>
    private lateinit var actionsFlow: MutableSharedFlow<CreateEditGroupUiAction>
    private lateinit var handler: CreateEditGroupMemberEventHandlerImpl

    private val currentUser = User(userId = "current-id", email = "me@test.com", displayName = "Me")
    private val otherUser = User(userId = "other-id", email = "other@test.com", displayName = "Other")

    @BeforeEach
    fun setup() {
        every { authenticationService.currentUserId() } returns currentUser.userId
        every { authenticationService.currentUserEmail() } returns currentUser.email
        coEvery { searchUsersByEmailUseCase(any()) } returns Result.success(emptyList())
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()

        stateFlow = MutableStateFlow(
            CreateEditGroupUiState(
                currentUserEmail = currentUser.email,
                maxMembersLimit = 2,
                isGroupPro = false
            )
        )
        actionsFlow = MutableSharedFlow()
        handler = CreateEditGroupMemberEventHandlerImpl(
            searchUsersByEmailUseCase = searchUsersByEmailUseCase,
            emailValidationService = emailValidationService,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            authenticationService = authenticationService,
            telemetryTracker = telemetryTracker
        )
    }

    private fun bindTo(scope: TestScope) {
        handler.bind(stateFlow, actionsFlow, scope)
    }

    @Test
    fun `handleSearchQueryChanged ignores short or invalid email and clears results`() = runTest(testDispatcher) {
        bindTo(this)
        stateFlow.value = stateFlow.value.copy(memberSearchResults = persistentListOf(otherUser))

        handler.handleSearchQueryChanged("ab")
        advanceUntilIdle()

        assertTrue(stateFlow.value.memberSearchResults.isEmpty())
        coVerify(exactly = 0) { searchUsersByEmailUseCase(any()) }

        handler.handleSearchQueryChanged("not-an-email")
        advanceUntilIdle()

        assertTrue(stateFlow.value.memberSearchResults.isEmpty())
        coVerify(exactly = 0) { searchUsersByEmailUseCase(any()) }
    }

    @Test
    fun `handleSearchQueryChanged debounces valid email and filters current user and already selected members`() =
        runTest(testDispatcher) {
            bindTo(this)
            val alreadySelected = User(userId = "selected-id", email = "selected@test.com")
            stateFlow.value = stateFlow.value.copy(selectedMembers = persistentListOf(alreadySelected))
            coEvery { searchUsersByEmailUseCase("other@test.com") } returns Result.success(
                listOf(currentUser, alreadySelected, otherUser)
            )

            handler.handleSearchQueryChanged("other@test.com")
            advanceUntilIdle()

            assertEquals(listOf(otherUser), stateFlow.value.memberSearchResults)
        }

    @Test
    fun `handleSearchQueryChanged builds pending user when search returns no matches`() = runTest(testDispatcher) {
        bindTo(this)
        coEvery { searchUsersByEmailUseCase("newbie@test.com") } returns Result.success(emptyList())

        handler.handleSearchQueryChanged("newbie@test.com")
        advanceUntilIdle()

        assertEquals(1, stateFlow.value.memberSearchResults.size)
        assertEquals("newbie@test.com", stateFlow.value.memberSearchResults.first().email)
        assertTrue(stateFlow.value.memberSearchResults.first().isPending)
    }

    @Test
    fun `handleMemberSelected adds member and clears search results when under limit`() = runTest(testDispatcher) {
        bindTo(this)
        stateFlow.value = stateFlow.value.copy(memberSearchResults = persistentListOf(otherUser))

        handler.handleMemberSelected(otherUser)

        assertEquals(listOf(otherUser), stateFlow.value.selectedMembers)
        assertTrue(stateFlow.value.memberSearchResults.isEmpty())
    }

    @Test
    fun `handleMemberSelected when limit reached for free group sets EXPANDED_GROUP_MEMBERS prompt`() =
        runTest(testDispatcher) {
            bindTo(this)
            val m1 = User(userId = "m1", email = "m1@test.com")
            val m2 = User(userId = "m2", email = "m2@test.com")
            stateFlow.value = stateFlow.value.copy(
                selectedMembers = persistentListOf(m1, m2),
                memberSearchResults = persistentListOf(otherUser),
                maxMembersLimit = 2,
                isGroupPro = false
            )
            val actions = mutableListOf<CreateEditGroupUiAction>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { actionsFlow.collect { actions.add(it) } }

            handler.handleMemberSelected(otherUser)
            advanceUntilIdle()

            assertEquals(
                ProFeaturePreviewPrompt(
                    previewType = ProFeaturePreviewType.EXPANDED_GROUP_MEMBERS,
                    entrySource = MonetizationTelemetry.EntrySource.GROUP_MEMBER_LIMIT
                ),
                stateFlow.value.proPreviewPrompt
            )
            assertTrue(stateFlow.value.memberSearchResults.isEmpty())
            assertTrue(actions.none { it is CreateEditGroupUiAction.ShowError })
            verify(exactly = 1) {
                telemetryTracker.trackEvent(
                    MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                    mapOf(
                        MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.LIMIT,
                        MonetizationTelemetry.Param.GATE_NAME to "max_members_per_group",
                        MonetizationTelemetry.Param.ENTRY_POINT to
                            MonetizationTelemetry.GateEntryPoint.GROUP_MEMBER_SELECT,
                        MonetizationTelemetry.Param.UPGRADE_PROMPTED to true
                    )
                )
            }
            job.cancel()
        }

    @Test
    fun `handleMemberSelected when limit reached for pro group emits ShowError without proPreviewPrompt`() =
        runTest(testDispatcher) {
            bindTo(this)
            val m1 = User(userId = "m1", email = "m1@test.com")
            val m2 = User(userId = "m2", email = "m2@test.com")
            stateFlow.value = stateFlow.value.copy(
                selectedMembers = persistentListOf(m1, m2),
                memberSearchResults = persistentListOf(otherUser),
                maxMembersLimit = 2,
                isGroupPro = true
            )
            val actions = mutableListOf<CreateEditGroupUiAction>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { actionsFlow.collect { actions.add(it) } }

            handler.handleMemberSelected(otherUser)
            advanceUntilIdle()

            assertNull(stateFlow.value.proPreviewPrompt)
            assertTrue(stateFlow.value.memberSearchResults.isEmpty())
            assertTrue(actions.any { it is CreateEditGroupUiAction.ShowError })
            verify(exactly = 1) {
                telemetryTracker.trackEvent(
                    MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                    mapOf(
                        MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.LIMIT,
                        MonetizationTelemetry.Param.GATE_NAME to "max_members_per_group",
                        MonetizationTelemetry.Param.ENTRY_POINT to
                            MonetizationTelemetry.GateEntryPoint.GROUP_MEMBER_SELECT,
                        MonetizationTelemetry.Param.UPGRADE_PROMPTED to false
                    )
                )
            }
            job.cancel()
        }

    @Test
    fun `handleMemberScanned rejects self with ShowError`() = runTest(testDispatcher) {
        bindTo(this)
        val actions = mutableListOf<CreateEditGroupUiAction>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { actionsFlow.collect { actions.add(it) } }

        handler.handleMemberScanned(currentUser.userId, currentUser.email)
        advanceUntilIdle()

        assertTrue(actions.any { it is CreateEditGroupUiAction.ShowError })
        assertTrue(stateFlow.value.selectedMembers.isEmpty())
        job.cancel()
    }

    @Test
    fun `handleMemberScanned when limit reached for free group sets EXPANDED_GROUP_MEMBERS prompt`() =
        runTest(testDispatcher) {
            bindTo(this)
            val m1 = User(userId = "m1", email = "m1@test.com")
            val m2 = User(userId = "m2", email = "m2@test.com")
            stateFlow.value = stateFlow.value.copy(
                selectedMembers = persistentListOf(m1, m2),
                maxMembersLimit = 2,
                isGroupPro = false
            )

            handler.handleMemberScanned(otherUser.userId, otherUser.email)
            advanceUntilIdle()

            assertEquals(
                ProFeaturePreviewPrompt(
                    previewType = ProFeaturePreviewType.EXPANDED_GROUP_MEMBERS,
                    entrySource = MonetizationTelemetry.EntrySource.GROUP_MEMBER_LIMIT
                ),
                stateFlow.value.proPreviewPrompt
            )
            verify(exactly = 1) {
                telemetryTracker.trackEvent(
                    MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                    mapOf(
                        MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.LIMIT,
                        MonetizationTelemetry.Param.GATE_NAME to "max_members_per_group",
                        MonetizationTelemetry.Param.ENTRY_POINT to
                            MonetizationTelemetry.GateEntryPoint.GROUP_MEMBER_SCAN,
                        MonetizationTelemetry.Param.UPGRADE_PROMPTED to true
                    )
                )
            }
        }

    @Test
    fun `handleMemberRemoved removes removable member and blocks non-removable member with ShowError`() =
        runTest(testDispatcher) {
            bindTo(this)
            val nonRemovable = User(userId = "locked-id", email = "locked@test.com")
            stateFlow.value = stateFlow.value.copy(
                isEditMode = true,
                selectedMembers = persistentListOf(otherUser, nonRemovable),
                nonRemovableMembers = persistentMapOf(
                    nonRemovable.userId to CannotRemoveMemberException.Reason.NON_ZERO_BALANCE
                )
            )
            val actions = mutableListOf<CreateEditGroupUiAction>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { actionsFlow.collect { actions.add(it) } }

            handler.handleMemberRemoved(otherUser)
            assertEquals(listOf(nonRemovable), stateFlow.value.selectedMembers)

            handler.handleMemberRemoved(nonRemovable)
            advanceUntilIdle()

            assertEquals(listOf(nonRemovable), stateFlow.value.selectedMembers)
            assertTrue(actions.any { it is CreateEditGroupUiAction.ShowError })
            job.cancel()
        }

    @Test
    fun `handleUnregisteredMemberDisplayNameChanged updates matching member display name`() = runTest(testDispatcher) {
        bindTo(this)
        val pending = User(userId = "pending-1", email = "pending@test.com", displayName = null)
        stateFlow.value = stateFlow.value.copy(selectedMembers = persistentListOf(pending))

        handler.handleUnregisteredMemberDisplayNameChanged("pending-1", "Alex")

        assertEquals("Alex", stateFlow.value.selectedMembers.first().displayName)
    }

    @Test
    fun `handleMemberCapacityUpsellClicked sets EXPANDED_GROUP_MEMBERS proPreviewPrompt and tracks gate`() =
        runTest(testDispatcher) {
            bindTo(this)
            val actions = mutableListOf<CreateEditGroupUiAction>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { actionsFlow.collect { actions.add(it) } }

            handler.handleMemberCapacityUpsellClicked()
            advanceUntilIdle()

            assertEquals(
                ProFeaturePreviewPrompt(
                    previewType = ProFeaturePreviewType.EXPANDED_GROUP_MEMBERS,
                    entrySource = MonetizationTelemetry.EntrySource.GROUP_MEMBER_LIMIT
                ),
                stateFlow.value.proPreviewPrompt
            )
            assertTrue(actions.none { it is CreateEditGroupUiAction.NavigateToSubscriptions })
            verify(exactly = 1) {
                telemetryTracker.trackEvent(
                    MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                    mapOf(
                        MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.LIMIT,
                        MonetizationTelemetry.Param.GATE_NAME to "max_members_per_group",
                        MonetizationTelemetry.Param.ENTRY_POINT to
                            MonetizationTelemetry.GateEntryPoint.GROUP_MEMBER_UPSELL,
                        MonetizationTelemetry.Param.UPGRADE_PROMPTED to true
                    )
                )
            }
            job.cancel()
        }
}
