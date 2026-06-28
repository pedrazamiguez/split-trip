package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import es.pedrazamiguez.splittrip.domain.service.impl.EmailValidationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetSupportedCurrenciesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.UpdateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.SearchUsersByEmailUseCase
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateEditGroupImageEventHandlerImpl
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateEditGroupNavigationEventHandlerImpl
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateEditGroupSubmitEventHandlerImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
class CreateEditGroupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getGroupByIdUseCase: GetGroupByIdUseCase
    private lateinit var createGroupUseCase: CreateGroupUseCase
    private lateinit var updateGroupUseCase: UpdateGroupUseCase
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
    private lateinit var appConfigService: AppConfigService
    private lateinit var viewModel: CreateEditGroupViewModel

    private val testUser1 = User(userId = "user-1", email = "alice@example.com", displayName = "Alice")
    private val testUser2 = User(userId = "user-2", email = "bob@example.com", displayName = "Bob")

    private val testGroupId = "group-123"
    private val testGroup = Group(
        id = testGroupId,
        name = "Japan Trip",
        description = "Trip to Japan",
        currency = "JPY",
        extraCurrencies = listOf("USD")
    )
    private val currencyUiModelJPY = CurrencyUiModel("JPY", "JPY - ¥", 0, "Japanese Yen", "Yen")
    private val currencyUiModelUSD = CurrencyUiModel("USD", "USD - $", 2, "US Dollar", "Dolar")

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getGroupByIdUseCase = mockk(relaxed = true)
        createGroupUseCase = mockk(relaxed = true)
        updateGroupUseCase = mockk(relaxed = true)
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
        appConfigService = mockk(relaxed = true) {
            every { defaultCurrencyCode } returns MutableStateFlow("EUR")
        }

        every { getUserDefaultCurrencyUseCase() } returns flowOf("EUR")
        every { getUserGroupsFlowUseCase() } returns flowOf(emptyList())
        every { featureGateService.isFeatureEnabled(any()) } returns flowOf(true)
        coEvery { featureGateService.checkLimit(any(), any()) } returns flowOf(LimitResult.Allowed)
        coEvery { getSupportedCurrenciesUseCase() } returns Result.success(emptyList())
        every { groupUiMapper.toCurrencyUiModels(any()) } returns
            persistentListOf(currencyUiModelJPY, currencyUiModelUSD)
        coEvery { getGroupByIdUseCase(testGroupId) } returns testGroup

        viewModel = createViewModel()
    }

    private fun createViewModel(): CreateEditGroupViewModel {
        val navigationEventHandler = CreateEditGroupNavigationEventHandlerImpl()
        val imageEventHandler = CreateEditGroupImageEventHandlerImpl(groupImageStorageService, featureGateService)
        val submitEventHandler = CreateEditGroupSubmitEventHandlerImpl(
            createGroupUseCase = createGroupUseCase,
            updateGroupUseCase = updateGroupUseCase,
            getUserGroupsFlowUseCase = getUserGroupsFlowUseCase,
            featureGateService = featureGateService,
            telemetryTracker = telemetryTracker,
            appConfigService = appConfigService
        )
        return CreateEditGroupViewModel(
            navigationEventHandler = navigationEventHandler,
            imageEventHandler = imageEventHandler,
            submitEventHandler = submitEventHandler,
            getGroupByIdUseCase = getGroupByIdUseCase,
            getSupportedCurrenciesUseCase = getSupportedCurrenciesUseCase,
            getUserDefaultCurrencyUseCase = getUserDefaultCurrencyUseCase,
            searchUsersByEmailUseCase = searchUsersByEmailUseCase,
            emailValidationService = emailValidationService,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            groupUiMapper = groupUiMapper,
            featureGateService = featureGateService,
            appConfigService = appConfigService,
            defaultDispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun onEvent(event: CreateEditGroupUiEvent) {
        viewModel.onEvent(event) {}
    }

    @Nested
    inner class CreateMode {

        @BeforeEach
        fun initCreate() {
            viewModel.init(null)
        }

        @Test
        fun `initial state shows create mode steps`() = runTest(testDispatcher) {
            advanceUntilIdle()
            val state = viewModel.uiState.value
            assertFalse(state.isEditMode)
            assertTrue(
                state.steps.contains(
                    es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupStep.MEMBERS
                )
            )
        }

        @Test
        fun `MemberSelected adds user to selectedMembers`() = runTest(testDispatcher) {
            onEvent(CreateEditGroupUiEvent.MemberSelected(testUser1))
            assertEquals(1, viewModel.uiState.value.selectedMembers.size)
            assertEquals("user-1", viewModel.uiState.value.selectedMembers[0].userId)
        }

        @Test
        fun `passes selected member userIds to CreateGroupUseCase`() = runTest(testDispatcher) {
            val groupSlot = slot<Group>()
            coEvery { createGroupUseCase(capture(groupSlot), any()) } returns Result.success("group-id")

            onEvent(CreateEditGroupUiEvent.NameChanged("Trip"))
            onEvent(CreateEditGroupUiEvent.MemberSelected(testUser1))
            onEvent(CreateEditGroupUiEvent.MemberSelected(testUser2))

            viewModel.onEvent(CreateEditGroupUiEvent.Submit) {}
            advanceUntilIdle()

            val capturedGroup = groupSlot.captured
            assertTrue("user-1" in capturedGroup.members)
            assertTrue("user-2" in capturedGroup.members)
            assertEquals(2, capturedGroup.members.size)
        }
    }

    @Nested
    inner class EditMode {

        @BeforeEach
        fun initEdit() {
            viewModel.init(testGroupId)
        }

        @Test
        fun `loads group and currency information on initialization`() = runTest(testDispatcher) {
            advanceUntilIdle()
            val state = viewModel.uiState.value
            assertTrue(state.isEditMode)
            assertEquals("Japan Trip", state.groupName)
            assertEquals("Trip to Japan", state.groupDescription)
            assertFalse(
                state.steps.contains(
                    es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupStep.MEMBERS
                )
            )
        }

        @Test
        fun `dispatches update on save submit`() = runTest(testDispatcher) {
            coEvery { updateGroupUseCase(any()) } returns Result.success(Unit)
            advanceUntilIdle()

            onEvent(CreateEditGroupUiEvent.NameChanged("Updated Trip"))
            viewModel.onEvent(CreateEditGroupUiEvent.Submit) {}
            advanceUntilIdle()

            coVerify(exactly = 1) {
                updateGroupUseCase(
                    match {
                        it.id == testGroupId && it.name == "Updated Trip"
                    }
                )
            }
        }
    }

    @Nested
    inner class WizardNavigation {

        @BeforeEach
        fun initWizard() {
            viewModel.init(null)
        }

        @Test
        fun `NextStep advances to the next step`() = runTest(testDispatcher) {
            advanceUntilIdle()
            val initialStep = viewModel.uiState.value.currentStep

            onEvent(CreateEditGroupUiEvent.NextStep)

            val newStep = viewModel.uiState.value.currentStep
            assertTrue(newStep != initialStep)
        }

        @Test
        fun `JumpToStep navigates to the target step`() = runTest(testDispatcher) {
            advanceUntilIdle()
            onEvent(CreateEditGroupUiEvent.NextStep)
            onEvent(CreateEditGroupUiEvent.NextStep)
            val steps = viewModel.uiState.value.steps
            assertEquals(2, steps.indexOf(viewModel.uiState.value.currentStep))

            onEvent(CreateEditGroupUiEvent.JumpToStep(0))

            assertEquals(steps[0], viewModel.uiState.value.currentStep)
        }
    }

    @Nested
    inner class FieldEditingEvents {

        @BeforeEach
        fun initCreate() {
            viewModel.init(null)
        }

        @Test
        fun `NameChanged updates groupName in state`() = runTest(testDispatcher) {
            onEvent(CreateEditGroupUiEvent.NameChanged("My Trip"))
            assertEquals("My Trip", viewModel.uiState.value.groupName)
        }

        @Test
        fun `NameChanged with blank name sets isNameValid false`() = runTest(testDispatcher) {
            onEvent(CreateEditGroupUiEvent.NameChanged(""))
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isNameValid)
        }

        @Test
        fun `NameChanged with non-blank name sets isNameValid true`() = runTest(testDispatcher) {
            onEvent(CreateEditGroupUiEvent.NameChanged("Valid"))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isNameValid)
        }

        @Test
        fun `DescriptionChanged updates groupDescription in state`() = runTest(testDispatcher) {
            onEvent(CreateEditGroupUiEvent.DescriptionChanged("A description"))
            assertEquals("A description", viewModel.uiState.value.groupDescription)
        }
    }

    @Nested
    inner class CurrencyEvents {

        @BeforeEach
        fun initCreate() {
            viewModel.init(null)
            advanceUntilIdle()
        }

        @Test
        fun `CurrencySelected updates selectedCurrency`() = runTest(testDispatcher) {
            onEvent(CreateEditGroupUiEvent.CurrencySelected(currencyUiModelJPY.code))
            assertEquals(currencyUiModelJPY, viewModel.uiState.value.selectedCurrency)
        }

        @Test
        fun `ExtraCurrencyToggled adds a currency to extraCurrencies`() = runTest(testDispatcher) {
            onEvent(CreateEditGroupUiEvent.ExtraCurrencyToggled(currencyUiModelUSD.code))
            assertTrue(viewModel.uiState.value.extraCurrencies.contains(currencyUiModelUSD))
        }

        @Test
        fun `ExtraCurrencyToggled removes already-added currency`() = runTest(testDispatcher) {
            onEvent(CreateEditGroupUiEvent.ExtraCurrencyToggled(currencyUiModelUSD.code))
            onEvent(CreateEditGroupUiEvent.ExtraCurrencyToggled(currencyUiModelUSD.code))
            assertFalse(viewModel.uiState.value.extraCurrencies.contains(currencyUiModelUSD))
        }
    }

    @Nested
    inner class MemberManagement {

        @BeforeEach
        fun initCreate() {
            viewModel.init(null)
        }

        @Test
        fun `MemberRemoved removes user from selectedMembers`() = runTest(testDispatcher) {
            onEvent(CreateEditGroupUiEvent.MemberSelected(testUser1))
            onEvent(CreateEditGroupUiEvent.MemberSelected(testUser2))
            assertEquals(2, viewModel.uiState.value.selectedMembers.size)

            onEvent(CreateEditGroupUiEvent.MemberRemoved(testUser1))
            assertEquals(1, viewModel.uiState.value.selectedMembers.size)
            assertEquals("user-2", viewModel.uiState.value.selectedMembers[0].userId)
        }

        @Test
        fun `MemberSelected does not add duplicate members`() = runTest(testDispatcher) {
            onEvent(CreateEditGroupUiEvent.MemberSelected(testUser1))
            onEvent(CreateEditGroupUiEvent.MemberSelected(testUser1))
            assertEquals(1, viewModel.uiState.value.selectedMembers.size)
        }

        @Test
        fun `UnregisteredMemberDisplayNameChanged updates name for pending member`() = runTest(testDispatcher) {
            val pendingUser = User(userId = "p-1", email = "pending@test.com", isPending = true)
            onEvent(CreateEditGroupUiEvent.MemberSelected(pendingUser))
            onEvent(CreateEditGroupUiEvent.UnregisteredMemberDisplayNameChanged("p-1", "John"))
            assertEquals(
                "John",
                viewModel.uiState.value.selectedMembers
                    .find { it.userId == "p-1" }?.displayName
            )
        }
    }

    @Nested
    inner class ImageManagement {

        @BeforeEach
        fun initCreate() {
            viewModel.init(null)
        }

        @Test
        fun `cleans temp images on init`() = runTest(testDispatcher) {
            advanceUntilIdle()
            coVerify(exactly = 1) { groupImageStorageService.cleanTempGroupImages() }
        }

        @Test
        fun `GroupImagePicked saves temp image and updates state`() = runTest(testDispatcher) {
            val pickedUri = "content://picker/image.jpg"
            val tempUri = "file:///temp/group_temp_123.webp"
            coEvery { groupImageStorageService.saveTempGroupImage(pickedUri) } returns tempUri

            onEvent(CreateEditGroupUiEvent.GroupImagePicked(pickedUri))
            advanceUntilIdle()

            coVerify(exactly = 1) { groupImageStorageService.saveTempGroupImage(pickedUri) }
            assertEquals(tempUri, viewModel.uiState.value.localGroupImagePath)
        }

        @Test
        fun `GroupImageRemoved clears image path`() = runTest(testDispatcher) {
            coEvery { groupImageStorageService.saveTempGroupImage(any()) } returns "file:///temp/img.webp"
            onEvent(CreateEditGroupUiEvent.GroupImagePicked("content://img.jpg"))
            advanceUntilIdle()

            onEvent(CreateEditGroupUiEvent.GroupImageRemoved)
            assertFalse(viewModel.uiState.value.localGroupImagePath?.isNotEmpty() == true)
        }

        @Test
        fun `ShowImageSourceSheet shows and hides the sheet`() = runTest(testDispatcher) {
            onEvent(CreateEditGroupUiEvent.ShowImageSourceSheet(true))
            assertTrue(viewModel.uiState.value.showImageSourceSheet)

            onEvent(CreateEditGroupUiEvent.ShowImageSourceSheet(false))
            assertFalse(viewModel.uiState.value.showImageSourceSheet)
        }
    }

    @Nested
    inner class MemberSearch {

        @BeforeEach
        fun initCreate() {
            viewModel.init(null)
        }

        @Test
        fun `MemberSearchQueryChanged with invalid email clears search results`() = runTest(testDispatcher) {
            onEvent(CreateEditGroupUiEvent.MemberSearchQueryChanged("not-an-email"))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.memberSearchResults.isEmpty())
            coVerify(exactly = 0) { searchUsersByEmailUseCase(any()) }
        }

        @Test
        fun `MemberSearchQueryChanged with valid email calls use case after debounce`() = runTest(testDispatcher) {
            val userList = listOf(testUser1)
            coEvery { searchUsersByEmailUseCase("alice@example.com") } returns Result.success(userList)

            onEvent(CreateEditGroupUiEvent.MemberSearchQueryChanged("alice@example.com"))
            advanceUntilIdle()

            coVerify(atLeast = 1) { searchUsersByEmailUseCase("alice@example.com") }
        }

        @Test
        fun `MemberSearchQueryChanged with empty query clears results`() = runTest(testDispatcher) {
            coEvery { searchUsersByEmailUseCase(any()) } returns Result.success(listOf(testUser1))
            onEvent(CreateEditGroupUiEvent.MemberSearchQueryChanged("alice@example.com"))
            advanceUntilIdle()

            onEvent(CreateEditGroupUiEvent.MemberSearchQueryChanged(""))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.memberSearchResults.isEmpty())
        }
    }
}
