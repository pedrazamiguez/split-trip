package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import es.pedrazamiguez.splittrip.domain.service.impl.EmailValidationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetSupportedCurrenciesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.SearchUsersByEmailUseCase
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateGroupImageEventHandlerImpl
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateGroupNavigationEventHandlerImpl
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateGroupSubmitEventHandlerImpl
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class CreateGroupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var createGroupUseCase: CreateGroupUseCase
    private lateinit var getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase
    private lateinit var featureGateService: FeatureGateService
    private lateinit var getSupportedCurrenciesUseCase: GetSupportedCurrenciesUseCase
    private lateinit var getUserDefaultCurrencyUseCase: GetUserDefaultCurrencyUseCase
    private lateinit var searchUsersByEmailUseCase: SearchUsersByEmailUseCase
    private lateinit var emailValidationService: EmailValidationService
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var groupUiMapper: GroupUiMapper
    private lateinit var groupImageStorageService: GroupImageStorageService
    private lateinit var telemetryTracker: TelemetryTracker
    private lateinit var viewModel: CreateGroupViewModel

    private val testUser1 = User(
        userId = "user-1",
        email = "alice@example.com",
        displayName = "Alice"
    )

    private val testUser2 = User(
        userId = "user-2",
        email = "bob@example.com",
        displayName = "Bob"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        createGroupUseCase = mockk(relaxed = true)
        getUserGroupsFlowUseCase = mockk(relaxed = true)
        featureGateService = mockk(relaxed = true)
        getSupportedCurrenciesUseCase = mockk(relaxed = true)
        getUserDefaultCurrencyUseCase = mockk(relaxed = true)
        searchUsersByEmailUseCase = mockk(relaxed = true)
        getMemberProfilesUseCase = mockk(relaxed = true)
        groupUiMapper = mockk(relaxed = true)
        groupImageStorageService = mockk(relaxed = true)
        telemetryTracker = mockk(relaxed = true)
        emailValidationService = EmailValidationServiceImpl()

        every { getUserDefaultCurrencyUseCase() } returns flowOf("EUR")
        every { getUserGroupsFlowUseCase() } returns flowOf(emptyList())
        every { featureGateService.isFeatureEnabled(any()) } returns flowOf(true)
        coEvery { featureGateService.checkLimit(any(), any()) } returns flowOf(LimitResult.Allowed)
        coEvery { getSupportedCurrenciesUseCase(any()) } returns Result.success(emptyList())
        every { groupUiMapper.toCurrencyUiModels(any()) } returns persistentListOf()

        viewModel = createViewModel()
    }

    private fun createViewModel(): CreateGroupViewModel {
        val navigationEventHandler = CreateGroupNavigationEventHandlerImpl()
        val imageEventHandler = CreateGroupImageEventHandlerImpl(groupImageStorageService, featureGateService)
        val submitEventHandler = CreateGroupSubmitEventHandlerImpl(
            createGroupUseCase = createGroupUseCase,
            getUserGroupsFlowUseCase = getUserGroupsFlowUseCase,
            featureGateService = featureGateService,
            telemetryTracker = telemetryTracker
        )
        return CreateGroupViewModel(
            createGroupNavigationEventHandler = navigationEventHandler,
            createGroupImageEventHandler = imageEventHandler,
            createGroupSubmitEventHandler = submitEventHandler,
            getSupportedCurrenciesUseCase = getSupportedCurrenciesUseCase,
            getUserDefaultCurrencyUseCase = getUserDefaultCurrencyUseCase,
            searchUsersByEmailUseCase = searchUsersByEmailUseCase,
            emailValidationService = emailValidationService,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            groupUiMapper = groupUiMapper,
            featureGateService = featureGateService,
            defaultDispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun onEvent(event: CreateGroupUiEvent) {
        viewModel.onEvent(event) {}
    }

    @Nested
    inner class MemberSearch {

        @Test
        fun `does not trigger search for partial email`() = runTest(testDispatcher) {
            // Given — not a valid email
            onEvent(CreateGroupUiEvent.MemberSearchQueryChanged("john@gma"))
            advanceUntilIdle()

            // Then — use case never called
            coVerify(exactly = 0) { searchUsersByEmailUseCase(any()) }
            assertTrue(viewModel.uiState.value.memberSearchResults.isEmpty())
        }

        @Test
        fun `does not trigger search for short query`() = runTest(testDispatcher) {
            onEvent(CreateGroupUiEvent.MemberSearchQueryChanged("ab"))
            advanceUntilIdle()

            coVerify(exactly = 0) { searchUsersByEmailUseCase(any()) }
        }

        @Test
        fun `triggers search for valid email after debounce`() = runTest(testDispatcher) {
            // Given
            coEvery { searchUsersByEmailUseCase("alice@example.com") } returns Result.success(listOf(testUser1))

            // When
            onEvent(CreateGroupUiEvent.MemberSearchQueryChanged("alice@example.com"))

            // Then — not yet called (debounce)
            advanceTimeBy(200)
            coVerify(exactly = 0) { searchUsersByEmailUseCase(any()) }

            // After debounce
            advanceTimeBy(200)
            coVerify(exactly = 1) { searchUsersByEmailUseCase("alice@example.com") }
            assertEquals(1, viewModel.uiState.value.memberSearchResults.size)
            assertEquals("user-1", viewModel.uiState.value.memberSearchResults[0].userId)
        }

        @Test
        fun `debounce cancels previous search when query changes rapidly`() = runTest(testDispatcher) {
            // Given
            coEvery { searchUsersByEmailUseCase("alice@example.com") } returns Result.success(listOf(testUser1))
            coEvery { searchUsersByEmailUseCase("bob@example.com") } returns Result.success(listOf(testUser2))

            // When — type first email, then quickly change to second
            onEvent(CreateGroupUiEvent.MemberSearchQueryChanged("alice@example.com"))
            advanceTimeBy(100)
            onEvent(CreateGroupUiEvent.MemberSearchQueryChanged("bob@example.com"))
            advanceUntilIdle()

            // Then — only second search was executed
            coVerify(exactly = 0) { searchUsersByEmailUseCase("alice@example.com") }
            coVerify(exactly = 1) { searchUsersByEmailUseCase("bob@example.com") }
            assertEquals("user-2", viewModel.uiState.value.memberSearchResults[0].userId)
        }

        @Test
        fun `filters out already selected users from search results`() = runTest(testDispatcher) {
            // Given — user-1 is already selected
            onEvent(CreateGroupUiEvent.MemberSelected(testUser1))

            coEvery { searchUsersByEmailUseCase("alice@example.com") } returns
                Result.success(listOf(testUser1))

            // When
            onEvent(CreateGroupUiEvent.MemberSearchQueryChanged("alice@example.com"))
            advanceUntilIdle()

            // Then — search result is filtered because user-1 is already selected
            assertTrue(viewModel.uiState.value.memberSearchResults.isEmpty())
        }

        @Test
        fun `triggers search for unregistered email and returns pending user in results`() = runTest(testDispatcher) {
            // Given — email search returns empty list (unregistered)
            val unregisteredEmail = "unregistered@example.com"
            coEvery { searchUsersByEmailUseCase(unregisteredEmail) } returns Result.success(emptyList())

            // When
            onEvent(CreateGroupUiEvent.MemberSearchQueryChanged(unregisteredEmail))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { searchUsersByEmailUseCase(unregisteredEmail) }
            val results = viewModel.uiState.value.memberSearchResults
            assertEquals(1, results.size)
            assertTrue(results[0].isPending)
            assertEquals(unregisteredEmail, results[0].email)
            assertEquals(User.generatePendingUserId(unregisteredEmail), results[0].userId)
        }

        @Test
        fun `does not return pending user in search results if already selected`() = runTest(testDispatcher) {
            // Given — pending user is already selected
            val unregisteredEmail = "unregistered@example.com"
            val pendingUser = User(
                userId = User.generatePendingUserId(unregisteredEmail),
                email = unregisteredEmail,
                isPending = true
            )
            onEvent(CreateGroupUiEvent.MemberSelected(pendingUser))

            coEvery { searchUsersByEmailUseCase(unregisteredEmail) } returns Result.success(emptyList())

            // When
            onEvent(CreateGroupUiEvent.MemberSearchQueryChanged(unregisteredEmail))
            advanceUntilIdle()

            // Then — not in search results because it's already selected
            assertTrue(viewModel.uiState.value.memberSearchResults.isEmpty())
        }

        @Test
        fun `search failure clears results`() = runTest(testDispatcher) {
            // Given
            coEvery { searchUsersByEmailUseCase(any()) } returns
                Result.failure(RuntimeException("Network error"))

            // When
            onEvent(CreateGroupUiEvent.MemberSearchQueryChanged("alice@example.com"))
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.memberSearchResults.isEmpty())
            assertFalse(viewModel.uiState.value.isSearchingMembers)
        }
    }

    @Nested
    inner class MemberSelection {

        @Test
        fun `MemberSelected adds user to selectedMembers`() = runTest(testDispatcher) {
            onEvent(CreateGroupUiEvent.MemberSelected(testUser1))

            assertEquals(1, viewModel.uiState.value.selectedMembers.size)
            assertEquals("user-1", viewModel.uiState.value.selectedMembers[0].userId)
        }

        @Test
        fun `MemberSelected clears search results`() = runTest(testDispatcher) {
            // Given — populate search results first
            coEvery { searchUsersByEmailUseCase(any()) } returns Result.success(listOf(testUser1))
            onEvent(CreateGroupUiEvent.MemberSearchQueryChanged("alice@example.com"))
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.memberSearchResults.size)

            // When
            onEvent(CreateGroupUiEvent.MemberSelected(testUser1))

            // Then
            assertTrue(viewModel.uiState.value.memberSearchResults.isEmpty())
        }

        @Test
        fun `MemberSelected does not duplicate already selected user`() = runTest(testDispatcher) {
            onEvent(CreateGroupUiEvent.MemberSelected(testUser1))
            onEvent(CreateGroupUiEvent.MemberSelected(testUser1))

            assertEquals(1, viewModel.uiState.value.selectedMembers.size)
        }

        @Test
        fun `MemberRemoved removes user from selectedMembers`() = runTest(testDispatcher) {
            onEvent(CreateGroupUiEvent.MemberSelected(testUser1))
            onEvent(CreateGroupUiEvent.MemberSelected(testUser2))
            assertEquals(2, viewModel.uiState.value.selectedMembers.size)

            onEvent(CreateGroupUiEvent.MemberRemoved(testUser1))

            assertEquals(1, viewModel.uiState.value.selectedMembers.size)
            assertEquals("user-2", viewModel.uiState.value.selectedMembers[0].userId)
        }
    }

    @Nested
    inner class CurrencyLoading {

        @Test
        fun `loads currencies eagerly on init`() = runTest(testDispatcher) {
            // Given — mocks already configured in setUp, ViewModel already created
            advanceUntilIdle()

            // Then — currency use cases were called during init
            coVerify(exactly = 1) { getSupportedCurrenciesUseCase(any()) }
            coVerify(exactly = 1) { getUserDefaultCurrencyUseCase() }
        }

        @Test
        fun `populates state with mapped currencies on success`() = runTest(testDispatcher) {
            // Given
            val currencies = listOf(
                Currency("EUR", "€", "Euro", 2),
                Currency("USD", "$", "US Dollar", 2)
            )
            val mappedCurrencies = persistentListOf(
                CurrencyUiModel("EUR", "EUR (€)", 2, "Euro"),
                CurrencyUiModel("USD", "USD ($)", 2, "US Dollar")
            )
            coEvery { getSupportedCurrenciesUseCase(any()) } returns Result.success(currencies)
            every { groupUiMapper.toCurrencyUiModels(currencies) } returns mappedCurrencies

            // Re-create ViewModel to trigger init with the new stubs
            viewModel = createViewModel()
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertEquals(2, state.availableCurrencies.size)
            assertEquals("EUR", state.selectedCurrency?.code)
            assertFalse(state.isLoadingCurrencies)
        }

        @Test
        fun `emits error action on currency load failure`() = runTest(testDispatcher) {
            // Given
            coEvery { getSupportedCurrenciesUseCase(any()) } returns
                Result.failure(RuntimeException("Network error"))

            // Re-create ViewModel to trigger init with the failure stub
            viewModel = createViewModel()

            val actions = mutableListOf<CreateGroupUiAction>()
            val collectJob = launch(start = CoroutineStart.UNDISPATCHED) {
                viewModel.actions.collect { actions.add(it) }
            }
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.availableCurrencies.isEmpty())
            assertFalse(state.isLoadingCurrencies)
            assertEquals(1, actions.size)
            assertTrue(actions[0] is CreateGroupUiAction.ShowError)
            collectJob.cancel()
        }

        @Test
        fun `invokes currency load only once during ViewModel init`() = runTest(testDispatcher) {
            // Drain setUp's ViewModel pending coroutines first
            advanceUntilIdle()

            // Given — configure stubs for a real currency load
            val currencies = listOf(Currency("EUR", "€", "Euro", 2))
            val mappedCurrencies = persistentListOf(
                CurrencyUiModel("EUR", "EUR (€)", 2, "Euro")
            )
            coEvery { getSupportedCurrenciesUseCase(any()) } returns Result.success(currencies)
            every { groupUiMapper.toCurrencyUiModels(currencies) } returns mappedCurrencies

            // Clear recorded calls from setUp's ViewModel init (keeps stubs)
            clearMocks(
                getSupportedCurrenciesUseCase,
                getUserDefaultCurrencyUseCase,
                answers = false
            )

            // When — create a fresh ViewModel whose init triggers loadCurrencies
            viewModel = createViewModel()
            advanceUntilIdle()

            // Then — exactly one call to each use case from init
            coVerify(exactly = 1) { getSupportedCurrenciesUseCase(any()) }
            coVerify(exactly = 1) { getUserDefaultCurrencyUseCase() }
            assertEquals(1, viewModel.uiState.value.availableCurrencies.size)
            assertFalse(viewModel.uiState.value.isLoadingCurrencies)
        }
    }

    @Nested
    inner class SubmitCreateGroup {

        @Test
        fun `passes selected member userIds to CreateGroupUseCase`() = runTest(testDispatcher) {
            // Given
            val groupSlot = slot<Group>()
            coEvery { createGroupUseCase(capture(groupSlot), any()) } returns Result.success("group-id")

            onEvent(CreateGroupUiEvent.NameChanged("Trip"))
            onEvent(CreateGroupUiEvent.MemberSelected(testUser1))
            onEvent(CreateGroupUiEvent.MemberSelected(testUser2))

            // When
            viewModel.onEvent(CreateGroupUiEvent.SubmitCreateGroup) {}
            advanceUntilIdle()

            // Then
            val capturedGroup = groupSlot.captured
            assertTrue("user-1" in capturedGroup.members)
            assertTrue("user-2" in capturedGroup.members)
            assertEquals(2, capturedGroup.members.size)
        }

        @Test
        fun `creates group with empty members when none selected`() = runTest(testDispatcher) {
            // Given
            val groupSlot = slot<Group>()
            coEvery { createGroupUseCase(capture(groupSlot), any()) } returns Result.success("group-id")

            onEvent(CreateGroupUiEvent.NameChanged("Solo Trip"))

            // When
            viewModel.onEvent(CreateGroupUiEvent.SubmitCreateGroup) {}
            advanceUntilIdle()

            // Then
            assertTrue(groupSlot.captured.members.isEmpty())
        }

        @Test
        fun `emits ShowSuccess action on successful creation`() = runTest(testDispatcher) {
            // Given
            coEvery { createGroupUseCase(any(), any()) } returns Result.success("group-id")
            onEvent(CreateGroupUiEvent.NameChanged("Trip"))

            // When
            val actions = mutableListOf<CreateGroupUiAction>()
            val collectJob = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(CreateGroupUiEvent.SubmitCreateGroup) {}
            advanceUntilIdle()

            // Then
            assertEquals(1, actions.size)
            assertTrue(actions[0] is CreateGroupUiAction.ShowSuccess)
            collectJob.cancel()
        }

        @Test
        fun `tracks telemetry event on successful creation`() = runTest(testDispatcher) {
            // Given
            coEvery { createGroupUseCase(any(), any()) } returns Result.success("group-id")
            onEvent(CreateGroupUiEvent.NameChanged("Trip"))

            // When
            viewModel.onEvent(CreateGroupUiEvent.SubmitCreateGroup) {}
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                telemetryTracker.trackEvent("group_created", any())
            }
        }

        @Test
        fun `emits ShowError action on creation failure`() = runTest(testDispatcher) {
            // Given
            coEvery { createGroupUseCase(any(), any()) } returns Result.failure(RuntimeException("Failed"))
            onEvent(CreateGroupUiEvent.NameChanged("Trip"))

            // When
            val actions = mutableListOf<CreateGroupUiAction>()
            val collectJob = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(CreateGroupUiEvent.SubmitCreateGroup) {}
            advanceUntilIdle()

            // Then
            assertEquals(1, actions.size)
            assertTrue(actions[0] is CreateGroupUiAction.ShowError)
            collectJob.cancel()
        }

        @Test
        fun `does not submit when name is blank`() = runTest(testDispatcher) {
            // Given — name not set (blank)
            viewModel.onEvent(CreateGroupUiEvent.SubmitCreateGroup) {}
            advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.isNameValid)
            coVerify(exactly = 0) { createGroupUseCase(any(), any()) }
        }
    }

    @Nested
    inner class WizardNavigation {

        @Test
        fun `NextStep advances to the next step`() = runTest(testDispatcher) {
            advanceUntilIdle()
            val initialStep = viewModel.uiState.value.currentStep

            onEvent(CreateGroupUiEvent.NextStep)

            val newStep = viewModel.uiState.value.currentStep
            assertTrue(newStep != initialStep)
        }

        @Test
        fun `PreviousStep on first step emits NavigateBack`() = runTest(testDispatcher) {
            advanceUntilIdle()
            val actions = mutableListOf<CreateGroupUiAction>()
            val collectJob = launch { viewModel.actions.collect { actions.add(it) } }

            onEvent(CreateGroupUiEvent.PreviousStep)
            advanceUntilIdle()

            assertTrue(actions.any { it is CreateGroupUiAction.NavigateBack })
            collectJob.cancel()
        }

        @Test
        fun `JumpToStep navigates to the target step`() = runTest(testDispatcher) {
            advanceUntilIdle()
            // Advance two steps
            onEvent(CreateGroupUiEvent.NextStep)
            onEvent(CreateGroupUiEvent.NextStep)
            val steps = viewModel.uiState.value.steps
            assertEquals(2, steps.indexOf(viewModel.uiState.value.currentStep))

            // When — jump back to step 0
            onEvent(CreateGroupUiEvent.JumpToStep(0))

            // Then
            assertEquals(steps[0], viewModel.uiState.value.currentStep)
        }

        @Test
        fun `JumpToStep clears any existing error`() = runTest(testDispatcher) {
            advanceUntilIdle()
            // Trigger a blank-name error by submitting
            viewModel.onEvent(CreateGroupUiEvent.SubmitCreateGroup) {}
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isNameValid)

            // Advance so we can jump back
            onEvent(CreateGroupUiEvent.NameChanged("Trip"))
            onEvent(CreateGroupUiEvent.NextStep)
            onEvent(CreateGroupUiEvent.JumpToStep(0))

            assertNull(viewModel.uiState.value.error)
        }

        @Test
        fun `JumpToStep is a no-op for out-of-bounds index`() = runTest(testDispatcher) {
            advanceUntilIdle()
            val stepBefore = viewModel.uiState.value.currentStep

            onEvent(CreateGroupUiEvent.JumpToStep(999))

            assertEquals(stepBefore, viewModel.uiState.value.currentStep)
        }
    }

    @Nested
    inner class MemberQrScanning {

        @Test
        fun `MemberScanned adds partial user immediately`() = runTest(testDispatcher) {
            // When
            onEvent(CreateGroupUiEvent.MemberScanned(userId = "user-123", email = "scanned@example.com"))

            // Then
            val selected = viewModel.uiState.value.selectedMembers
            assertEquals(1, selected.size)
            val addedUser = selected.first()
            assertEquals("user-123", addedUser.userId)
            assertEquals("scanned@example.com", addedUser.email)
            assertNull(addedUser.displayName)
        }

        @Test
        fun `MemberScanned starts background lookup and updates with full user profile upon success`() = runTest(
            testDispatcher
        ) {
            val fullProfile = User(
                userId = "user-123",
                email = "scanned@example.com",
                displayName = "Scanned User",
                profileImagePath = "path/to/avatar",
                bio = "Hello world"
            )
            coEvery { getMemberProfilesUseCase(listOf("user-123")) } returns mapOf("user-123" to fullProfile)

            // When
            onEvent(CreateGroupUiEvent.MemberScanned(userId = "user-123", email = "scanned@example.com"))
            advanceUntilIdle()

            // Then
            val selected = viewModel.uiState.value.selectedMembers
            assertEquals(1, selected.size)
            val addedUser = selected.first()
            assertEquals("user-123", addedUser.userId)
            assertEquals("scanned@example.com", addedUser.email)
            assertEquals("Scanned User", addedUser.displayName)
            assertEquals("path/to/avatar", addedUser.profileImagePath)
            assertEquals("Hello world", addedUser.bio)
        }

        @Test
        fun `MemberScanned keeps partial user if background lookup fails`() = runTest(testDispatcher) {
            coEvery { getMemberProfilesUseCase(any()) } throws RuntimeException("Network error")

            // When
            onEvent(CreateGroupUiEvent.MemberScanned(userId = "user-123", email = "scanned@example.com"))
            advanceUntilIdle()

            // Then
            val selected = viewModel.uiState.value.selectedMembers
            assertEquals(1, selected.size)
            val addedUser = selected.first()
            assertEquals("user-123", addedUser.userId)
            assertEquals("scanned@example.com", addedUser.email)
            assertNull(addedUser.displayName)
        }

        @Test
        fun `MemberScanned does not duplicate already selected user`() = runTest(testDispatcher) {
            val existing = User(userId = "user-123", email = "scanned@example.com", displayName = "Existing")
            viewModel.onEvent(CreateGroupUiEvent.MemberSelected(existing)) {}

            // When
            onEvent(CreateGroupUiEvent.MemberScanned(userId = "user-123", email = "scanned@example.com"))
            advanceUntilIdle()

            // Then
            val selected = viewModel.uiState.value.selectedMembers
            assertEquals(1, selected.size)
            assertEquals("Existing", selected.first().displayName)
        }
    }

    @Nested
    inner class ImageSelection {

        @Test
        fun `cleans temp images on init`() = runTest(testDispatcher) {
            advanceUntilIdle()
            coVerify(exactly = 1) { groupImageStorageService.cleanTempGroupImages() }
        }

        @Test
        fun `GroupImagePicked saves temp image and updates state`() = runTest(testDispatcher) {
            // Given
            val pickedUri = "content://picker/image.jpg"
            val tempUri = "file:///temp/group_temp_123.webp"
            coEvery { groupImageStorageService.saveTempGroupImage(pickedUri) } returns tempUri

            // When
            onEvent(CreateGroupUiEvent.GroupImagePicked(pickedUri))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { groupImageStorageService.saveTempGroupImage(pickedUri) }
            assertEquals(tempUri, viewModel.uiState.value.localGroupImagePath)
            assertFalse(viewModel.uiState.value.isLoading)
        }

        @Test
        fun `GroupImagePicked sets error on failure`() = runTest(testDispatcher) {
            // Given
            val pickedUri = "content://picker/image.jpg"
            coEvery { groupImageStorageService.saveTempGroupImage(pickedUri) } throws
                RuntimeException("Processing failed")

            // When
            onEvent(CreateGroupUiEvent.GroupImagePicked(pickedUri))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { groupImageStorageService.saveTempGroupImage(pickedUri) }
            assertNull(viewModel.uiState.value.localGroupImagePath)
            assertNotNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }

        @Test
        fun `GroupImageRemoved clears localGroupImagePath`() = runTest(testDispatcher) {
            // Given
            val tempUri = "file:///temp/group_temp_123.webp"
            coEvery { groupImageStorageService.saveTempGroupImage(any()) } returns tempUri
            onEvent(CreateGroupUiEvent.GroupImagePicked("content://picker/image.jpg"))
            advanceUntilIdle()
            assertEquals(tempUri, viewModel.uiState.value.localGroupImagePath)

            // When
            onEvent(CreateGroupUiEvent.GroupImageRemoved)

            // Then
            assertNull(viewModel.uiState.value.localGroupImagePath)
        }

        @Test
        fun `ShowImageSourceSheet updates sheet visibility state`() = runTest(testDispatcher) {
            // When
            onEvent(CreateGroupUiEvent.ShowImageSourceSheet(true))

            // Then
            assertTrue(viewModel.uiState.value.showImageSourceSheet)

            // When
            onEvent(CreateGroupUiEvent.ShowImageSourceSheet(false))

            // Then
            assertFalse(viewModel.uiState.value.showImageSourceSheet)
        }
    }

    @Nested
    inner class UnregisteredTravelerNames {

        @Test
        fun `UnregisteredMemberDisplayNameChanged updates display name of pending user`() = runTest(testDispatcher) {
            // Given
            val pendingUser = User(userId = "pending-123", email = "pending@example.com", isPending = true)
            onEvent(CreateGroupUiEvent.MemberSelected(pendingUser))
            advanceUntilIdle()

            // When
            onEvent(CreateGroupUiEvent.UnregisteredMemberDisplayNameChanged("pending-123", "  Jake  "))
            advanceUntilIdle()

            // Then
            val selected = viewModel.uiState.value.selectedMembers
            assertEquals(1, selected.size)
            assertEquals("Jake", selected.first().displayName)
        }

        @Test
        fun `UnregisteredMemberDisplayNameChanged sets display name to null if blank`() = runTest(testDispatcher) {
            // Given
            val pendingUser =
                User(userId = "pending-123", email = "pending@example.com", displayName = "Jake", isPending = true)
            onEvent(CreateGroupUiEvent.MemberSelected(pendingUser))
            advanceUntilIdle()

            // When
            onEvent(CreateGroupUiEvent.UnregisteredMemberDisplayNameChanged("pending-123", "   "))
            advanceUntilIdle()

            // Then
            val selected = viewModel.uiState.value.selectedMembers
            assertEquals(1, selected.size)
            assertNull(selected.first().displayName)
        }

        @Test
        fun `UnregisteredMemberDisplayNameChanged does not modify other users`() = runTest(testDispatcher) {
            // Given
            val pendingUser1 =
                User(userId = "pending-1", email = "p1@example.com", displayName = "Jake", isPending = true)
            val pendingUser2 =
                User(userId = "pending-2", email = "p2@example.com", displayName = "Elwood", isPending = true)
            onEvent(CreateGroupUiEvent.MemberSelected(pendingUser1))
            onEvent(CreateGroupUiEvent.MemberSelected(pendingUser2))
            advanceUntilIdle()

            // When
            onEvent(CreateGroupUiEvent.UnregisteredMemberDisplayNameChanged("pending-1", "Joliet Jake"))
            advanceUntilIdle()

            // Then
            val selected = viewModel.uiState.value.selectedMembers
            assertEquals(2, selected.size)
            assertEquals("Joliet Jake", selected.find { it.userId == "pending-1" }?.displayName)
            assertEquals("Elwood", selected.find { it.userId == "pending-2" }?.displayName)
        }
    }
}
