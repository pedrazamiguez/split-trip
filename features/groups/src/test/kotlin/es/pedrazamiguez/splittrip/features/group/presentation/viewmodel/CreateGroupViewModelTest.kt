package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.service.impl.EmailValidationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetSupportedCurrenciesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.SearchUsersByEmailUseCase
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateGroupUiEvent
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateGroupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var createGroupUseCase: CreateGroupUseCase
    private lateinit var getSupportedCurrenciesUseCase: GetSupportedCurrenciesUseCase
    private lateinit var getUserDefaultCurrencyUseCase: GetUserDefaultCurrencyUseCase
    private lateinit var searchUsersByEmailUseCase: SearchUsersByEmailUseCase
    private lateinit var emailValidationService: EmailValidationService
    private lateinit var groupUiMapper: GroupUiMapper
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
        getSupportedCurrenciesUseCase = mockk(relaxed = true)
        getUserDefaultCurrencyUseCase = mockk(relaxed = true)
        searchUsersByEmailUseCase = mockk(relaxed = true)
        groupUiMapper = mockk(relaxed = true)
        emailValidationService = EmailValidationServiceImpl()

        every { getUserDefaultCurrencyUseCase() } returns flowOf("EUR")
        coEvery { getSupportedCurrenciesUseCase(any()) } returns Result.success(emptyList())
        every { groupUiMapper.toCurrencyUiModels(any()) } returns persistentListOf()

        viewModel = CreateGroupViewModel(
            createGroupUseCase = createGroupUseCase,
            getSupportedCurrenciesUseCase = getSupportedCurrenciesUseCase,
            getUserDefaultCurrencyUseCase = getUserDefaultCurrencyUseCase,
            searchUsersByEmailUseCase = searchUsersByEmailUseCase,
            emailValidationService = emailValidationService,
            groupUiMapper = groupUiMapper,
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
            viewModel = CreateGroupViewModel(
                createGroupUseCase = createGroupUseCase,
                getSupportedCurrenciesUseCase = getSupportedCurrenciesUseCase,
                getUserDefaultCurrencyUseCase = getUserDefaultCurrencyUseCase,
                searchUsersByEmailUseCase = searchUsersByEmailUseCase,
                emailValidationService = emailValidationService,
                groupUiMapper = groupUiMapper,
                defaultDispatcher = testDispatcher
            )
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
            viewModel = CreateGroupViewModel(
                createGroupUseCase = createGroupUseCase,
                getSupportedCurrenciesUseCase = getSupportedCurrenciesUseCase,
                getUserDefaultCurrencyUseCase = getUserDefaultCurrencyUseCase,
                searchUsersByEmailUseCase = searchUsersByEmailUseCase,
                emailValidationService = emailValidationService,
                groupUiMapper = groupUiMapper,
                defaultDispatcher = testDispatcher
            )

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
            viewModel = CreateGroupViewModel(
                createGroupUseCase = createGroupUseCase,
                getSupportedCurrenciesUseCase = getSupportedCurrenciesUseCase,
                getUserDefaultCurrencyUseCase = getUserDefaultCurrencyUseCase,
                searchUsersByEmailUseCase = searchUsersByEmailUseCase,
                emailValidationService = emailValidationService,
                groupUiMapper = groupUiMapper,
                defaultDispatcher = testDispatcher
            )
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
            coEvery { createGroupUseCase(capture(groupSlot)) } returns Result.success("group-id")

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
            coEvery { createGroupUseCase(capture(groupSlot)) } returns Result.success("group-id")

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
            coEvery { createGroupUseCase(any()) } returns Result.success("group-id")
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
        fun `emits ShowError action on creation failure`() = runTest(testDispatcher) {
            // Given
            coEvery { createGroupUseCase(any()) } returns Result.failure(RuntimeException("Failed"))
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
            coVerify(exactly = 0) { createGroupUseCase(any()) }
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
}
