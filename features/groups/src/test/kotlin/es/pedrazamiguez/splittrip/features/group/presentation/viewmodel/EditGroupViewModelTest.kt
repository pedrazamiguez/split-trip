package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetSupportedCurrenciesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.UpdateGroupUseCase
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.EditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.EditGroupUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
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
class EditGroupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getGroupByIdUseCase: GetGroupByIdUseCase
    private lateinit var updateGroupUseCase: UpdateGroupUseCase
    private lateinit var getSupportedCurrenciesUseCase: GetSupportedCurrenciesUseCase
    private lateinit var groupImageStorageService: GroupImageStorageService
    private lateinit var groupUiMapper: GroupUiMapper
    private lateinit var featureGateService: FeatureGateService
    private lateinit var viewModel: EditGroupViewModel

    private val testGroupId = "group-123"
    private val testGroup = Group(
        id = testGroupId,
        name = "Japan Trip",
        description = "Trip to Japan",
        currency = "JPY",
        extraCurrencies = listOf("USD")
    )
    private val testCurrency = Currency(code = "JPY", symbol = "¥", defaultName = "Japanese Yen", decimalDigits = 0)
    private val testCurrencyUSD = Currency(code = "USD", symbol = "$", defaultName = "US Dollar", decimalDigits = 2)

    private val currencyUiModelJPY = CurrencyUiModel(
        code = "JPY",
        displayText = "JPY - Yen japonés",
        decimalDigits = 0,
        defaultName = "Japanese Yen",
        localizedName = "Yen japonés"
    )
    private val currencyUiModelUSD = CurrencyUiModel(
        code = "USD",
        displayText = "USD - Dólar estadounidense",
        decimalDigits = 2,
        defaultName = "US Dollar",
        localizedName = "Dólar estadounidense"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getGroupByIdUseCase = mockk()
        updateGroupUseCase = mockk()
        getSupportedCurrenciesUseCase = mockk()
        groupImageStorageService = mockk(relaxed = true)
        groupUiMapper = mockk()
        featureGateService = mockk()

        every { featureGateService.isFeatureEnabled(any()) } returns flowOf(true)
        coEvery { getSupportedCurrenciesUseCase() } returns Result.success(listOf(testCurrency, testCurrencyUSD))
        every { groupUiMapper.toCurrencyUiModels(any()) } returns
            kotlinx.collections.immutable.persistentListOf(currencyUiModelJPY, currencyUiModelUSD)
        coEvery { getGroupByIdUseCase(testGroupId) } returns testGroup

        viewModel = EditGroupViewModel(
            getGroupByIdUseCase = getGroupByIdUseCase,
            updateGroupUseCase = updateGroupUseCase,
            getSupportedCurrenciesUseCase = getSupportedCurrenciesUseCase,
            groupImageStorageService = groupImageStorageService,
            groupUiMapper = groupUiMapper,
            featureGateService = featureGateService
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    inner class Initialization {

        @Test
        fun `loads group and currency information on initialization`() = runTest {
            viewModel.initGroup(testGroupId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals("Japan Trip", state.groupName)
            assertEquals("Trip to Japan", state.groupDescription)
            assertEquals(currencyUiModelJPY, state.selectedCurrency)
            assertEquals(listOf(currencyUiModelUSD), state.extraCurrencies)
            assertEquals(listOf(currencyUiModelJPY, currencyUiModelUSD), state.availableCurrencies)
        }
    }

    @Nested
    inner class Validation {

        @Test
        fun `validates group name not empty`() = runTest {
            viewModel.initGroup(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(EditGroupUiEvent.NameChanged(""))
            viewModel.onEvent(EditGroupUiEvent.SaveClicked)

            val state = viewModel.uiState.value
            assertFalse(state.isNameValid)
            coVerify(exactly = 0) { updateGroupUseCase(any()) }
        }
    }

    @Nested
    inner class Saving {

        @Test
        fun `dispatches update on save click`() = runTest {
            coEvery { updateGroupUseCase(any()) } returns Result.success(Unit)
            viewModel.initGroup(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(EditGroupUiEvent.NameChanged("Updated Trip"))
            viewModel.onEvent(EditGroupUiEvent.SaveClicked)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                updateGroupUseCase(
                    match {
                        it.id == testGroupId && it.name == "Updated Trip"
                    }
                )
            }
        }

        @Test
        fun `propagates success notification action on successful save`() = runTest {
            coEvery { updateGroupUseCase(any()) } returns Result.success(Unit)
            viewModel.initGroup(testGroupId)
            advanceUntilIdle()

            val actions = mutableListOf<EditGroupUiAction>()
            val job = launch {
                viewModel.actions.toList(actions)
            }

            viewModel.onEvent(EditGroupUiEvent.SaveClicked)
            advanceUntilIdle()

            assertTrue(
                actions.any {
                    it is EditGroupUiAction.ShowNotification &&
                        it.message == UiText.StringResource(R.string.group_edit_success_saved)
                }
            )
            assertTrue(actions.any { it is EditGroupUiAction.NavigateBack })

            job.cancel()
        }

        @Test
        fun `propagates error notification action on failed save`() = runTest {
            coEvery { updateGroupUseCase(any()) } returns Result.failure(RuntimeException("Firestore error"))
            viewModel.initGroup(testGroupId)
            advanceUntilIdle()

            val actions = mutableListOf<EditGroupUiAction>()
            val job = launch {
                viewModel.actions.toList(actions)
            }

            viewModel.onEvent(EditGroupUiEvent.SaveClicked)
            advanceUntilIdle()

            assertTrue(
                actions.any {
                    it is EditGroupUiAction.ShowNotification &&
                        it.message == UiText.StringResource(R.string.group_error_creation_failed)
                }
            )
            assertFalse(actions.any { it is EditGroupUiAction.NavigateBack })

            job.cancel()
        }
    }

    @Nested
    inner class InitializationFailure {

        @Test
        fun `propagates error action when group is not found`() = runTest {
            coEvery { getGroupByIdUseCase(testGroupId) } returns null

            val actions = mutableListOf<EditGroupUiAction>()
            val job = launch {
                viewModel.actions.toList(actions)
            }

            viewModel.initGroup(testGroupId)
            advanceUntilIdle()

            assertTrue(
                actions.any {
                    it is EditGroupUiAction.ShowNotification &&
                        it.message == UiText.StringResource(R.string.group_detail_error_loading)
                }
            )
            assertTrue(actions.any { it is EditGroupUiAction.NavigateBack })

            job.cancel()
        }
    }

    @Nested
    inner class Events {

        @Test
        fun `handles description change event`() = runTest {
            viewModel.initGroup(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(EditGroupUiEvent.DescriptionChanged("New Description"))
            assertEquals("New Description", viewModel.uiState.value.groupDescription)
        }

        @Test
        fun `handles currency selection event`() = runTest {
            viewModel.initGroup(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(EditGroupUiEvent.CurrencySelected("USD"))
            assertEquals(currencyUiModelUSD, viewModel.uiState.value.selectedCurrency)
        }

        @Test
        fun `handles currency selection event when currency is currently an extra currency`() = runTest {
            viewModel.initGroup(testGroupId)
            advanceUntilIdle()

            // extraCurrencies starts with USD. If we select USD as primary, USD should be removed from extra
            viewModel.onEvent(EditGroupUiEvent.CurrencySelected("USD"))
            assertEquals(currencyUiModelUSD, viewModel.uiState.value.selectedCurrency)
            assertTrue(viewModel.uiState.value.extraCurrencies.isEmpty())
        }

        @Test
        fun `handles extra currency toggling to add and remove extra currencies`() = runTest {
            viewModel.initGroup(testGroupId)
            advanceUntilIdle()

            // USD is already in extraCurrencies, so toggling it should remove it
            viewModel.onEvent(EditGroupUiEvent.ExtraCurrencyToggled("USD"))
            assertTrue(viewModel.uiState.value.extraCurrencies.isEmpty())

            // Toggling JPY (not in extra) should add JPY
            viewModel.onEvent(EditGroupUiEvent.ExtraCurrencyToggled("JPY"))
            assertEquals(listOf(currencyUiModelJPY), viewModel.uiState.value.extraCurrencies)
        }

        @Test
        fun `handles group image removal event`() = runTest {
            viewModel.initGroup(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(EditGroupUiEvent.GroupImageRemoved)
            assertNull(viewModel.uiState.value.localGroupImagePath)
            assertNull(viewModel.uiState.value.imageUrl)
        }

        @Test
        fun `handles show image source sheet event`() = runTest {
            viewModel.onEvent(EditGroupUiEvent.ShowImageSourceSheet(true))
            assertTrue(viewModel.uiState.value.showImageSourceSheet)

            viewModel.onEvent(EditGroupUiEvent.ShowImageSourceSheet(false))
            assertFalse(viewModel.uiState.value.showImageSourceSheet)
        }

        @Test
        fun `handles group image picked success`() = runTest {
            coEvery { groupImageStorageService.saveTempGroupImage("uri-123") } returns "temp-uri-123"

            viewModel.onEvent(EditGroupUiEvent.GroupImagePicked("uri-123"))
            advanceUntilIdle()

            assertEquals("temp-uri-123", viewModel.uiState.value.localGroupImagePath)
            assertNull(viewModel.uiState.value.imageUrl)
            assertFalse(viewModel.uiState.value.isLoading)
        }

        @Test
        fun `handles group image picked failure`() = runTest {
            coEvery { groupImageStorageService.saveTempGroupImage("uri-123") } throws
                RuntimeException("Error processing")

            val actions = mutableListOf<EditGroupUiAction>()
            val job = launch {
                viewModel.actions.toList(actions)
            }

            viewModel.onEvent(EditGroupUiEvent.GroupImagePicked("uri-123"))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(
                actions.any {
                    it is EditGroupUiAction.ShowNotification &&
                        it.message == UiText.StringResource(R.string.group_error_image_processing_failed)
                }
            )

            job.cancel()
        }
    }

    @Nested
    inner class ImageCleaning {

        @Test
        fun `cleans temp images on init and clear`() = runTest {
            coVerify(exactly = 1) { groupImageStorageService.cleanTempGroupImages() }
        }

        @Test
        fun `handles clean temp images failure gracefully`() = runTest {
            coEvery { groupImageStorageService.cleanTempGroupImages() } throws RuntimeException("Cleanup failed")
            // Initiating viewmodel again to trigger cleanTempGroupImages
            EditGroupViewModel(
                getGroupByIdUseCase = getGroupByIdUseCase,
                updateGroupUseCase = updateGroupUseCase,
                getSupportedCurrenciesUseCase = getSupportedCurrenciesUseCase,
                groupImageStorageService = groupImageStorageService,
                groupUiMapper = groupUiMapper,
                featureGateService = featureGateService
            )
            advanceUntilIdle()
            coVerify { groupImageStorageService.cleanTempGroupImages() }
        }
    }
}
