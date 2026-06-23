package es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ContributionValidationService
import es.pedrazamiguez.splittrip.domain.service.impl.ContributionValidationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.AddContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.contribution.presentation.mapper.AddContributionUiMapper
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.action.AddContributionUiAction
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.event.AddContributionUiEvent
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.handler.ContributionConfigHandler
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.handler.ContributionSubmitHandler
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionStep
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AddContributionViewModel")
class AddContributionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var addContributionUseCase: AddContributionUseCase
    private lateinit var getGroupByIdUseCase: GetGroupByIdUseCase
    private lateinit var getGroupSubunitsUseCase: GetGroupSubunitsUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var authenticationService: AuthenticationService
    private lateinit var contributionValidationService: ContributionValidationService
    private lateinit var addContributionUiMapper: AddContributionUiMapper
    private lateinit var appConfigService: AppConfigService
    private lateinit var configHandler: ContributionConfigHandler
    private lateinit var submitHandler: ContributionSubmitHandler
    private lateinit var viewModel: AddContributionViewModel

    private val testGroup = Group(
        id = "group-1",
        name = "Trip",
        currency = "EUR",
        members = listOf("user-1", "user-2")
    )

    private val testSubunit = Subunit(
        id = "subunit-1",
        groupId = "group-1",
        name = "Couple A",
        memberIds = listOf("user-1", "user-2")
    )

    private val testMemberProfiles = mapOf(
        "user-1" to User(userId = "user-1", email = "user1@test.com", displayName = "Andrés"),
        "user-2" to User(userId = "user-2", email = "user2@test.com", displayName = "Ana")
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        addContributionUseCase = mockk(relaxed = true)
        getGroupByIdUseCase = mockk()
        getGroupSubunitsUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        authenticationService = mockk()
        contributionValidationService = ContributionValidationServiceImpl()
        addContributionUiMapper = mockk(relaxed = true)
        appConfigService = mockk()

        every { appConfigService.defaultCurrencyCode } returns MutableStateFlow("EUR")
        every { addContributionUiMapper.resolveCurrencySymbol(any()) } returns "€"
        every {
            addContributionUiMapper.formatInputAmountWithCurrency(any(), any())
        } returns "100,00 €"
        every {
            addContributionUiMapper.toMemberOptions(any(), any(), any())
        } answers {
            val memberIds = firstArg<List<String>>()
            val profiles = secondArg<Map<String, User>>()
            val currentUserId = thirdArg<String?>()
            memberIds.map { id ->
                MemberOptionUiModel(
                    userId = id,
                    displayName = profiles[id]?.displayName ?: id,
                    isCurrentUser = id == currentUserId
                )
            }.let { persistentListOf<MemberOptionUiModel>().addingAll(it) }
        }
        every {
            addContributionUiMapper.resolveDisplayName(any(), any())
        } answers {
            val userId = firstArg<String?>()
            val members = secondArg<ImmutableList<MemberOptionUiModel>>()
            members.firstOrNull { it.userId == userId }?.displayName ?: ""
        }

        coEvery { getMemberProfilesUseCase(any()) } returns testMemberProfiles

        // Co-create handlers like the DI module does
        configHandler = ContributionConfigHandler(
            getGroupByIdUseCase = getGroupByIdUseCase,
            getGroupSubunitsUseCase = getGroupSubunitsUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            authenticationService = authenticationService,
            addContributionUiMapper = addContributionUiMapper,
            appConfigService = appConfigService
        )

        submitHandler = ContributionSubmitHandler(
            addContributionUseCase = addContributionUseCase,
            contributionValidationService = contributionValidationService,
            groupCurrencyProvider = { configHandler.groupCurrency }
        )

        viewModel = AddContributionViewModel(
            configHandler = configHandler,
            submitHandler = submitHandler,
            contributionValidationService = contributionValidationService,
            addContributionUiMapper = addContributionUiMapper
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── setGroupContext (replaces LoadGroupConfig event) ─────────────────

    @Nested
    @DisplayName("setGroupContext")
    inner class SetGroupContext {

        @Test
        fun `happy path populates subunitOptions, members, and resets form`() =
            runTest(testDispatcher) {
                coEvery { getGroupByIdUseCase("group-1") } returns testGroup
                coEvery { authenticationService.currentUserId() } returns "user-1"
                coEvery { getGroupSubunitsUseCase("group-1") } returns listOf(testSubunit)

                viewModel.setGroupContext("group-1", "EUR")
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(1, state.subunitOptions.size)
                assertEquals("subunit-1", state.subunitOptions[0].id)
                assertEquals("EUR", state.groupCurrencyCode)
                assertEquals("€", state.groupCurrencySymbol)
                assertEquals(PayerType.USER, state.contributionScope)
                assertNull(state.selectedSubunitId)
                assertEquals("", state.amountInput)
                assertFalse(state.amountError)
            }

        @Test
        fun `loads group members and defaults selectedMemberId to current user`() =
            runTest(testDispatcher) {
                coEvery { getGroupByIdUseCase("group-1") } returns testGroup
                coEvery { authenticationService.currentUserId() } returns "user-1"
                coEvery { getGroupSubunitsUseCase("group-1") } returns emptyList()

                viewModel.setGroupContext("group-1", "EUR")
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(2, state.groupMembers.size)
                assertEquals("user-1", state.selectedMemberId)
                assertEquals("Andrés", state.selectedMemberDisplayName)
                assertTrue(state.groupMembers[0].isCurrentUser)
                assertFalse(state.groupMembers[1].isCurrentUser)
            }

        @Test
        fun `null groupId is a no-op`() = runTest(testDispatcher) {
            viewModel.setGroupContext(null, null)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.subunitOptions.isEmpty())
            assertTrue(viewModel.uiState.value.groupMembers.isEmpty())
            coVerify(exactly = 0) { getGroupByIdUseCase(any()) }
        }

        @Test
        fun `failure emits ShowError action`() = runTest(testDispatcher) {
            coEvery { getGroupByIdUseCase("group-1") } throws RuntimeException("Network")

            val emitted = mutableListOf<AddContributionUiAction>()
            val collectJob = launch {
                viewModel.actions.collect { emitted.add(it) }
            }

            viewModel.setGroupContext("group-1", "EUR")
            advanceUntilIdle()

            assertTrue(emitted.any { it is AddContributionUiAction.ShowError })
            collectJob.cancel()
        }

        @Test
        fun `filters subunits to only those containing current user`() =
            runTest(testDispatcher) {
                val otherSubunit = Subunit(
                    id = "subunit-2",
                    groupId = "group-1",
                    name = "Other Couple",
                    memberIds = listOf("user-3", "user-4")
                )
                coEvery { getGroupByIdUseCase("group-1") } returns testGroup
                coEvery { authenticationService.currentUserId() } returns "user-1"
                coEvery { getGroupSubunitsUseCase("group-1") } returns listOf(
                    testSubunit,
                    otherSubunit
                )

                viewModel.setGroupContext("group-1", "EUR")
                advanceUntilIdle()

                assertEquals(1, viewModel.uiState.value.subunitOptions.size)
                assertEquals("subunit-1", viewModel.uiState.value.subunitOptions[0].id)
            }

        @Test
        fun `calls getMemberProfilesUseCase with group members`() =
            runTest(testDispatcher) {
                coEvery { getGroupByIdUseCase("group-1") } returns testGroup
                coEvery { authenticationService.currentUserId() } returns "user-1"
                coEvery { getGroupSubunitsUseCase("group-1") } returns emptyList()

                viewModel.setGroupContext("group-1", "EUR")
                advanceUntilIdle()

                coVerify { getMemberProfilesUseCase(listOf("user-1", "user-2")) }
            }
    }

    // ── MemberSelected ───────────────────────────────────────────────────

    @Nested
    @DisplayName("MemberSelected")
    inner class MemberSelected {

        @Test
        fun `updates selectedMemberId and display name`() = runTest(testDispatcher) {
            seedGroup()

            viewModel.onEvent(AddContributionUiEvent.MemberSelected("user-2"))

            val state = viewModel.uiState.value
            assertEquals("user-2", state.selectedMemberId)
            assertEquals("Ana", state.selectedMemberDisplayName)
        }

        @Test
        fun `re-filters subunits for the selected member`() = runTest(testDispatcher) {
            val subunitForUser2Only = Subunit(
                id = "subunit-2",
                groupId = "group-1",
                name = "Couple B",
                memberIds = listOf("user-2", "user-3")
            )
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            coEvery { authenticationService.currentUserId() } returns "user-1"
            coEvery { getGroupSubunitsUseCase("group-1") } returns listOf(
                testSubunit,
                subunitForUser2Only
            )

            viewModel.setGroupContext("group-1", "EUR")
            advanceUntilIdle()

            // user-1 is in testSubunit and subunitForUser2Only is not for user-1
            assertEquals(1, viewModel.uiState.value.subunitOptions.size)
            assertEquals("subunit-1", viewModel.uiState.value.subunitOptions[0].id)

            // Select user-2 → should see both subunits (user-2 is in both)
            viewModel.onEvent(AddContributionUiEvent.MemberSelected("user-2"))
            val state = viewModel.uiState.value
            assertEquals(2, state.subunitOptions.size)
        }

        @Test
        fun `resets contributionScope and selectedSubunitId`() = runTest(testDispatcher) {
            seedGroup()

            // Set a SUBUNIT scope first
            viewModel.onEvent(
                AddContributionUiEvent.ContributionScopeSelected(
                    PayerType.SUBUNIT,
                    "subunit-1"
                )
            )
            assertEquals(PayerType.SUBUNIT, viewModel.uiState.value.contributionScope)

            // Switch member → scope should reset
            viewModel.onEvent(AddContributionUiEvent.MemberSelected("user-2"))

            val state = viewModel.uiState.value
            assertEquals(PayerType.USER, state.contributionScope)
            assertNull(state.selectedSubunitId)
        }

        private fun kotlinx.coroutines.test.TestScope.seedGroup() {
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            coEvery { authenticationService.currentUserId() } returns "user-1"
            coEvery { getGroupSubunitsUseCase("group-1") } returns listOf(testSubunit)
            viewModel.setGroupContext("group-1", "EUR")
            advanceUntilIdle()
        }
    }

    // ── UpdateAmount ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("UpdateAmount")
    inner class UpdateAmount {

        @Test
        fun `updates amountInput and clears error`() = runTest(testDispatcher) {
            viewModel.onEvent(AddContributionUiEvent.UpdateAmount("50"))

            assertEquals("50", viewModel.uiState.value.amountInput)
            assertFalse(viewModel.uiState.value.amountError)
        }
    }

    // ── NextStep / PreviousStep ─────────────────────────────────────────────

    @Nested
    @DisplayName("NextStep")
    inner class NextStep {

        @Test
        fun `advances from AMOUNT to SCOPE when amount is valid`() =
            runTest(testDispatcher) {
                seedGroup()
                viewModel.onEvent(AddContributionUiEvent.UpdateAmount("100"))
                viewModel.onEvent(AddContributionUiEvent.NextStep)

                assertEquals(AddContributionStep.SCOPE, viewModel.uiState.value.currentStep)
            }

        @Test
        fun `sets amountError when amount is invalid on AMOUNT step`() =
            runTest(testDispatcher) {
                viewModel.onEvent(AddContributionUiEvent.NextStep)

                assertTrue(viewModel.uiState.value.amountError)
                assertEquals(AddContributionStep.AMOUNT, viewModel.uiState.value.currentStep)
            }

        @Test
        fun `advancing to REVIEW formats amount with currency`() =
            runTest(testDispatcher) {
                seedGroup()
                viewModel.onEvent(AddContributionUiEvent.UpdateAmount("100"))
                viewModel.onEvent(AddContributionUiEvent.NextStep) // → SCOPE
                viewModel.onEvent(AddContributionUiEvent.NextStep) // → REVIEW

                assertEquals(AddContributionStep.REVIEW, viewModel.uiState.value.currentStep)
                assertEquals(
                    "100,00 €",
                    viewModel.uiState.value.formattedAmountWithCurrency
                )
            }

        private fun kotlinx.coroutines.test.TestScope.seedGroup() {
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            coEvery { authenticationService.currentUserId() } returns "user-1"
            coEvery { getGroupSubunitsUseCase("group-1") } returns emptyList()
            viewModel.setGroupContext("group-1", "EUR")
            advanceUntilIdle()
        }
    }

    @Nested
    @DisplayName("PreviousStep")
    inner class PreviousStep {

        @Test
        fun `goes back from SCOPE to AMOUNT`() = runTest(testDispatcher) {
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            coEvery { authenticationService.currentUserId() } returns "user-1"
            coEvery { getGroupSubunitsUseCase("group-1") } returns emptyList()
            viewModel.setGroupContext("group-1", "EUR")
            advanceUntilIdle()

            viewModel.onEvent(AddContributionUiEvent.UpdateAmount("100"))
            viewModel.onEvent(AddContributionUiEvent.NextStep) // → SCOPE
            viewModel.onEvent(AddContributionUiEvent.PreviousStep) // → AMOUNT

            assertEquals(AddContributionStep.AMOUNT, viewModel.uiState.value.currentStep)
        }

        @Test
        fun `emits NavigateBack on first step`() = runTest(testDispatcher) {
            val emitted = mutableListOf<AddContributionUiAction>()
            val collectJob = launch {
                viewModel.actions.collect { emitted.add(it) }
            }

            viewModel.onEvent(AddContributionUiEvent.PreviousStep)
            advanceUntilIdle()

            assertTrue(emitted.any { it is AddContributionUiAction.NavigateBack })
            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("JumpToStep")
    inner class JumpToStep {

        @Test
        fun `JumpToStep navigates to the target step`() = runTest(testDispatcher) {
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            coEvery { authenticationService.currentUserId() } returns "user-1"
            coEvery { getGroupSubunitsUseCase("group-1") } returns emptyList()
            viewModel.setGroupContext("group-1", "EUR")
            advanceUntilIdle()

            // Advance to last step
            viewModel.onEvent(AddContributionUiEvent.UpdateAmount("100"))
            viewModel.onEvent(AddContributionUiEvent.NextStep) // → SCOPE
            viewModel.onEvent(AddContributionUiEvent.NextStep) // → REVIEW
            assertEquals(AddContributionStep.REVIEW, viewModel.uiState.value.currentStep)

            // When — jump back to step 0
            viewModel.onEvent(AddContributionUiEvent.JumpToStep(0))

            // Then
            assertEquals(AddContributionStep.AMOUNT, viewModel.uiState.value.currentStep)
        }

        @Test
        fun `JumpToStep is a no-op for out-of-bounds index`() = runTest(testDispatcher) {
            // Given — on initial step
            assertEquals(AddContributionStep.AMOUNT, viewModel.uiState.value.currentStep)

            // When
            viewModel.onEvent(AddContributionUiEvent.JumpToStep(999))

            // Then — step unchanged
            assertEquals(AddContributionStep.AMOUNT, viewModel.uiState.value.currentStep)
        }
    }

    // ── ContributionScopeSelected ───────────────────────────────────────────

    @Nested
    @DisplayName("ContributionScopeSelected")
    inner class ContributionScopeSelected {

        @Test
        fun `USER scope clears subunitId`() = runTest(testDispatcher) {
            viewModel.onEvent(
                AddContributionUiEvent.ContributionScopeSelected(
                    PayerType.USER,
                    subunitId = "subunit-1"
                )
            )

            assertEquals(PayerType.USER, viewModel.uiState.value.contributionScope)
            assertNull(viewModel.uiState.value.selectedSubunitId)
        }

        @Test
        fun `SUBUNIT scope sets subunitId`() = runTest(testDispatcher) {
            viewModel.onEvent(
                AddContributionUiEvent.ContributionScopeSelected(
                    PayerType.SUBUNIT,
                    subunitId = "subunit-1"
                )
            )

            assertEquals(PayerType.SUBUNIT, viewModel.uiState.value.contributionScope)
            assertEquals("subunit-1", viewModel.uiState.value.selectedSubunitId)
        }
    }

    // ── Submit ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Submit")
    inner class Submit {

        @Test
        fun `no groupId set is a no-op`() = runTest(testDispatcher) {
            // Don't call setGroupContext → groupId stays null
            viewModel.onEvent(AddContributionUiEvent.Submit)
            advanceUntilIdle()

            coVerify(exactly = 0) { addContributionUseCase(any(), any()) }
        }

        @Test
        fun `invalid amount sets amountError`() = runTest(testDispatcher) {
            seedGroupWithAmount(null)

            viewModel.onEvent(AddContributionUiEvent.Submit)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.amountError)
            coVerify(exactly = 0) { addContributionUseCase(any(), any()) }
        }

        @Test
        fun `invalid subunitId emits ShowError`() = runTest(testDispatcher) {
            seedGroupWithAmount("100")

            viewModel.onEvent(
                AddContributionUiEvent.ContributionScopeSelected(
                    PayerType.SUBUNIT,
                    subunitId = "invalid-subunit"
                )
            )

            val emitted = mutableListOf<AddContributionUiAction>()
            val collectJob = launch {
                viewModel.actions.collect { emitted.add(it) }
            }

            viewModel.onEvent(AddContributionUiEvent.Submit)
            advanceUntilIdle()

            assertTrue(emitted.any { it is AddContributionUiAction.ShowError })
            collectJob.cancel()
        }

        @Test
        fun `happy path calls use case and emits ShowSuccess`() =
            runTest(testDispatcher) {
                seedGroupWithAmount("100")
                coEvery { addContributionUseCase(any(), any()) } just Runs

                val emitted = mutableListOf<AddContributionUiAction>()
                val collectJob = launch {
                    viewModel.actions.collect { emitted.add(it) }
                }

                var callbackInvoked = false
                viewModel.onEvent(AddContributionUiEvent.Submit) {
                    callbackInvoked = true
                }
                advanceUntilIdle()

                assertTrue(emitted.any { it is AddContributionUiAction.ShowSuccess })
                assertTrue(callbackInvoked)
                assertFalse(viewModel.uiState.value.isLoading)
                coVerify { addContributionUseCase("group-1", any()) }
                collectJob.cancel()
            }

        @Test
        fun `happy path passes selectedMemberId as Contribution userId`() =
            runTest(testDispatcher) {
                seedGroupWithAmount("100")
                coEvery { addContributionUseCase(any(), any()) } just Runs

                viewModel.onEvent(AddContributionUiEvent.Submit)
                advanceUntilIdle()

                coVerify {
                    addContributionUseCase(
                        "group-1",
                        match { it.userId == "user-1" }
                    )
                }
            }

        @Test
        fun `submit with impersonated member passes correct userId`() =
            runTest(testDispatcher) {
                seedGroupWithAmount("100")
                viewModel.onEvent(AddContributionUiEvent.MemberSelected("user-2"))
                coEvery { addContributionUseCase(any(), any()) } just Runs

                viewModel.onEvent(AddContributionUiEvent.Submit)
                advanceUntilIdle()

                coVerify {
                    addContributionUseCase(
                        "group-1",
                        match { it.userId == "user-2" }
                    )
                }
            }

        @Test
        fun `use case failure emits ShowError`() = runTest(testDispatcher) {
            seedGroupWithAmount("100")
            coEvery { addContributionUseCase(any(), any()) } throws RuntimeException("Boom")

            val emitted = mutableListOf<AddContributionUiAction>()
            val collectJob = launch {
                viewModel.actions.collect { emitted.add(it) }
            }

            viewModel.onEvent(AddContributionUiEvent.Submit)
            advanceUntilIdle()

            assertTrue(emitted.any { it is AddContributionUiAction.ShowError })
            assertFalse(viewModel.uiState.value.isLoading)
            collectJob.cancel()
        }

        private fun kotlinx.coroutines.test.TestScope.seedGroupWithAmount(amount: String?) {
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            coEvery { authenticationService.currentUserId() } returns "user-1"
            coEvery { getGroupSubunitsUseCase("group-1") } returns listOf(testSubunit)
            viewModel.setGroupContext("group-1", "EUR")
            advanceUntilIdle()
            if (amount != null) {
                viewModel.onEvent(AddContributionUiEvent.UpdateAmount(amount))
            }
        }
    }
}
