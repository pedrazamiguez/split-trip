package es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupNameUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetSelectedGroupUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SharedViewModel")
class SharedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getSelectedGroupIdUseCase: GetSelectedGroupIdUseCase
    private lateinit var getSelectedGroupNameUseCase: GetSelectedGroupNameUseCase
    private lateinit var getSelectedGroupCurrencyUseCase: GetSelectedGroupCurrencyUseCase
    private lateinit var setSelectedGroupUseCase: SetSelectedGroupUseCase
    private lateinit var observeSelectedGroupUseCase: ObserveSelectedGroupUseCase

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getSelectedGroupIdUseCase = mockk()
        getSelectedGroupNameUseCase = mockk()
        getSelectedGroupCurrencyUseCase = mockk()
        setSelectedGroupUseCase = mockk(relaxed = true)
        observeSelectedGroupUseCase = mockk()
        every { observeSelectedGroupUseCase() } returns flowOf(null)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SharedViewModel = SharedViewModel(
        getSelectedGroupIdUseCase = getSelectedGroupIdUseCase,
        getSelectedGroupNameUseCase = getSelectedGroupNameUseCase,
        getSelectedGroupCurrencyUseCase = getSelectedGroupCurrencyUseCase,
        setSelectedGroupUseCase = setSelectedGroupUseCase,
        observeSelectedGroupUseCase = observeSelectedGroupUseCase
    )

    @Nested
    @DisplayName("selectedGroupId")
    inner class SelectedGroupId {

        @Test
        fun `initial value is null before use case emits`() = runTest(testDispatcher) {
            // Given
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            // When
            val viewModel = createViewModel()

            // Then
            assertNull(viewModel.selectedGroupId.value)
        }

        @Test
        fun `emits group id from use case`() = runTest(testDispatcher) {
            // Given
            val expectedId = "group-123"
            every { getSelectedGroupIdUseCase() } returns flowOf(expectedId)
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            // When
            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.selectedGroupId.collect {} }
            advanceUntilIdle()

            // Then
            assertEquals(expectedId, viewModel.selectedGroupId.value)

            collectJob.cancel()
        }

        @Test
        fun `emits null when no group is selected`() = runTest(testDispatcher) {
            // Given
            every { getSelectedGroupIdUseCase() } returns flowOf(null)
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            // When
            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.selectedGroupId.collect {} }
            advanceUntilIdle()

            // Then
            assertNull(viewModel.selectedGroupId.value)

            collectJob.cancel()
        }

        @Test
        fun `reflects upstream changes reactively`() = runTest(testDispatcher) {
            // Given
            val groupIdFlow = MutableStateFlow<String?>(null)
            every { getSelectedGroupIdUseCase() } returns groupIdFlow
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.selectedGroupId.collect {} }
            advanceUntilIdle()

            // Initially null
            assertNull(viewModel.selectedGroupId.value)

            // When - upstream emits a new group id
            groupIdFlow.value = "group-456"
            advanceUntilIdle()

            // Then
            assertEquals("group-456", viewModel.selectedGroupId.value)

            // When - upstream clears the group
            groupIdFlow.value = null
            advanceUntilIdle()

            // Then
            assertNull(viewModel.selectedGroupId.value)

            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("selectedGroupName")
    inner class SelectedGroupName {

        @Test
        fun `initial value is null before use case emits`() = runTest(testDispatcher) {
            // Given
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            // When
            val viewModel = createViewModel()

            // Then
            assertNull(viewModel.selectedGroupName.value)
        }

        @Test
        fun `emits group name from use case`() = runTest(testDispatcher) {
            // Given
            val expectedName = "Summer Trip 2025"
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf(expectedName)
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            // When
            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.selectedGroupName.collect {} }
            advanceUntilIdle()

            // Then
            assertEquals(expectedName, viewModel.selectedGroupName.value)

            collectJob.cancel()
        }

        @Test
        fun `emits null when no group is selected`() = runTest(testDispatcher) {
            // Given
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf(null)
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            // When
            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.selectedGroupName.collect {} }
            advanceUntilIdle()

            // Then
            assertNull(viewModel.selectedGroupName.value)

            collectJob.cancel()
        }

        @Test
        fun `reflects upstream changes reactively`() = runTest(testDispatcher) {
            // Given
            val groupNameFlow = MutableStateFlow<String?>(null)
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns groupNameFlow
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.selectedGroupName.collect {} }
            advanceUntilIdle()

            // Initially null
            assertNull(viewModel.selectedGroupName.value)

            // When
            groupNameFlow.value = "Winter Retreat"
            advanceUntilIdle()

            // Then
            assertEquals("Winter Retreat", viewModel.selectedGroupName.value)

            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("selectGroup")
    inner class SelectGroup {

        @Test
        fun `delegates to SetSelectedGroupUseCase with id and name`() = runTest(testDispatcher) {
            // Given
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()
            coEvery { setSelectedGroupUseCase(any(), any(), any()) } returns Unit

            val viewModel = createViewModel()

            // When
            viewModel.selectGroup("group-789", "Beach Vacation")
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                setSelectedGroupUseCase("group-789", "Beach Vacation", null)
            }
        }

        @Test
        fun `delegates null values to clear selection`() = runTest(testDispatcher) {
            // Given
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()
            coEvery { setSelectedGroupUseCase(any(), any(), any()) } returns Unit

            val viewModel = createViewModel()

            // When
            viewModel.selectGroup(null, null)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                setSelectedGroupUseCase(null, null, null)
            }
        }

        @Test
        fun `updates flows when use case writes to DataStore`() = runTest(testDispatcher) {
            // Given - simulate DataStore-backed flows that update when written
            val groupIdFlow = MutableStateFlow<String?>(null)
            val groupNameFlow = MutableStateFlow<String?>(null)
            val groupCurrencyFlow = MutableStateFlow<String?>(null)
            every { getSelectedGroupIdUseCase() } returns groupIdFlow
            every { getSelectedGroupNameUseCase() } returns groupNameFlow
            every { getSelectedGroupCurrencyUseCase() } returns groupCurrencyFlow
            coEvery { setSelectedGroupUseCase(any(), any(), any()) } coAnswers {
                // Simulate DataStore behavior: writing updates the observed flows
                groupIdFlow.value = firstArg()
                groupNameFlow.value = secondArg()
                groupCurrencyFlow.value = thirdArg()
            }

            val viewModel = createViewModel()
            val idJob = backgroundScope.launch { viewModel.selectedGroupId.collect {} }
            val nameJob = backgroundScope.launch { viewModel.selectedGroupName.collect {} }
            val currencyJob = backgroundScope.launch { viewModel.selectedGroupCurrency.collect {} }
            advanceUntilIdle()

            // Verify initially null
            assertNull(viewModel.selectedGroupId.value)
            assertNull(viewModel.selectedGroupName.value)
            assertNull(viewModel.selectedGroupCurrency.value)

            // When
            viewModel.selectGroup("group-abc", "Road Trip", "EUR")
            advanceUntilIdle()

            // Then
            assertEquals("group-abc", viewModel.selectedGroupId.value)
            assertEquals("Road Trip", viewModel.selectedGroupName.value)
            assertEquals("EUR", viewModel.selectedGroupCurrency.value)

            idJob.cancel()
            nameJob.cancel()
            currencyJob.cancel()
        }
    }
}
