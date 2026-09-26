package es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
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
    private lateinit var observeGroupUseCase: ObserveGroupUseCase

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getSelectedGroupIdUseCase = mockk()
        getSelectedGroupNameUseCase = mockk()
        getSelectedGroupCurrencyUseCase = mockk()
        setSelectedGroupUseCase = mockk(relaxed = true)
        observeSelectedGroupUseCase = mockk(relaxed = true)
        observeGroupUseCase = mockk(relaxed = true)
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
        observeSelectedGroupUseCase = observeSelectedGroupUseCase,
        observeGroupUseCase = observeGroupUseCase
    )

    @Nested
    @DisplayName("isInitialLoadComplete")
    inner class IsInitialLoadComplete {

        @Test
        fun `initial value is false before use case emits`() = runTest(testDispatcher) {
            every { observeSelectedGroupUseCase() } returns flowOf()
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            val viewModel = createViewModel()

            assertEquals(false, viewModel.isInitialLoadComplete.value)
        }

        @Test
        fun `emits true once use case emits null`() = runTest(testDispatcher) {
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.isInitialLoadComplete.collect {} }
            advanceUntilIdle()

            assertEquals(true, viewModel.isInitialLoadComplete.value)

            collectJob.cancel()
        }

        @Test
        fun `emits true once use case emits group`() = runTest(testDispatcher) {
            every { observeSelectedGroupUseCase() } returns flowOf(mockk<Group>())
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.isInitialLoadComplete.collect {} }
            advanceUntilIdle()

            assertEquals(true, viewModel.isInitialLoadComplete.value)

            collectJob.cancel()
        }
    }

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

        @Test
        fun `emits group name from selectedGroup when group model is present`() = runTest(testDispatcher) {
            val group = Group(id = "group-1", name = "Alpine Adventure", currency = "CHF")
            every { observeSelectedGroupUseCase() } returns flowOf(group)
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf("Old Stored Name")
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.selectedGroupName.collect {} }
            advanceUntilIdle()

            assertEquals("Alpine Adventure", viewModel.selectedGroupName.value)

            collectJob.cancel()
        }

        @Test
        fun `reflects updated group name reactively when selectedGroup emits update`() = runTest(testDispatcher) {
            val groupFlow = MutableStateFlow<Group?>(Group(id = "group-1", name = "Old Name", currency = "EUR"))
            every { observeSelectedGroupUseCase() } returns groupFlow
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf("Fallback Name")
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.selectedGroupName.collect {} }
            advanceUntilIdle()

            assertEquals("Old Name", viewModel.selectedGroupName.value)

            groupFlow.value = Group(id = "group-1", name = "New Name", currency = "EUR")
            advanceUntilIdle()

            assertEquals("New Name", viewModel.selectedGroupName.value)

            collectJob.cancel()
        }

        @Test
        fun `falls back to preference when selectedGroup is null`() = runTest(testDispatcher) {
            every { observeSelectedGroupUseCase() } returns flowOf(null)
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf("Fallback From DataStore")
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.selectedGroupName.collect {} }
            advanceUntilIdle()

            assertEquals("Fallback From DataStore", viewModel.selectedGroupName.value)

            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("selectedGroupCurrency")
    inner class SelectedGroupCurrency {

        @Test
        fun `initial value is null before use case emits`() = runTest(testDispatcher) {
            every { observeSelectedGroupUseCase() } returns flowOf()
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            val viewModel = createViewModel()

            assertNull(viewModel.selectedGroupCurrency.value)
        }

        @Test
        fun `emits group currency from selectedGroup when group model is present`() = runTest(testDispatcher) {
            val group = Group(id = "group-1", name = "Japan Trip", currency = "JPY")
            every { observeSelectedGroupUseCase() } returns flowOf(group)
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf("USD")

            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.selectedGroupCurrency.collect {} }
            advanceUntilIdle()

            assertEquals("JPY", viewModel.selectedGroupCurrency.value)

            collectJob.cancel()
        }

        @Test
        fun `reflects updated group currency reactively when selectedGroup emits update`() = runTest(testDispatcher) {
            val groupFlow = MutableStateFlow<Group?>(Group(id = "group-1", name = "Trip", currency = "EUR"))
            every { observeSelectedGroupUseCase() } returns groupFlow
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf("USD")

            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.selectedGroupCurrency.collect {} }
            advanceUntilIdle()

            assertEquals("EUR", viewModel.selectedGroupCurrency.value)

            groupFlow.value = Group(id = "group-1", name = "Trip", currency = "GBP")
            advanceUntilIdle()

            assertEquals("GBP", viewModel.selectedGroupCurrency.value)

            collectJob.cancel()
        }

        @Test
        fun `falls back to preference when selectedGroup is null`() = runTest(testDispatcher) {
            every { observeSelectedGroupUseCase() } returns flowOf(null)
            every { getSelectedGroupIdUseCase() } returns flowOf()
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf("EUR")

            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.selectedGroupCurrency.collect {} }
            advanceUntilIdle()

            assertEquals("EUR", viewModel.selectedGroupCurrency.value)

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
            val groupFlow = MutableStateFlow<Group?>(null)
            every { getSelectedGroupIdUseCase() } returns groupIdFlow
            every { getSelectedGroupNameUseCase() } returns groupNameFlow
            every { getSelectedGroupCurrencyUseCase() } returns groupCurrencyFlow
            every { observeSelectedGroupUseCase() } returns groupFlow
            every { observeGroupUseCase(any()) } answers { groupFlow }
            coEvery { setSelectedGroupUseCase(any(), any(), any()) } coAnswers {
                // Simulate DataStore behavior: writing updates the observed flows
                val id = arg<String?>(0)
                val name = arg<String?>(1)
                val currency = arg<String?>(2)
                groupIdFlow.value = id
                groupNameFlow.value = name
                groupCurrencyFlow.value = currency
                groupFlow.value = if (id != null) {
                    Group(id = id, name = name ?: "", currency = currency ?: "EUR")
                } else {
                    null
                }
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

    @Nested
    @DisplayName("stale selectedGroupId reconciliation")
    inner class StaleSelectedGroupIdReconciliation {

        @Test
        fun `clears selectedGroupId when group resolves to null for non-null stored id`() = runTest(testDispatcher) {
            val groupIdFlow = MutableStateFlow<String?>("stale-id")
            val groupFlow = MutableStateFlow<Group?>(null)
            every { getSelectedGroupIdUseCase() } returns groupIdFlow
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()
            every { observeSelectedGroupUseCase() } returns groupFlow
            every { observeGroupUseCase("stale-id") } answers { groupFlow }

            val viewModel = createViewModel()
            advanceUntilIdle()

            coVerify(exactly = 1) { setSelectedGroupUseCase(null, null, null) }
        }

        @Test
        fun `does not clear when both selectedGroupId and selectedGroup are null`() = runTest(testDispatcher) {
            val groupIdFlow = MutableStateFlow<String?>(null)
            val groupFlow = MutableStateFlow<Group?>(null)
            every { getSelectedGroupIdUseCase() } returns groupIdFlow
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()
            every { observeSelectedGroupUseCase() } returns groupFlow
            every { observeGroupUseCase(any()) } answers { groupFlow }

            val viewModel = createViewModel()
            advanceUntilIdle()

            coVerify(exactly = 0) { setSelectedGroupUseCase(any(), any(), any()) }
        }

        @Test
        fun `does not clear when selectedGroup resolves successfully`() = runTest(testDispatcher) {
            val groupIdFlow = MutableStateFlow<String?>("group-456")
            val groupFlow = MutableStateFlow<Group?>(mockk())

            every { getSelectedGroupIdUseCase() } returns groupIdFlow
            every { observeSelectedGroupUseCase() } returns groupFlow
            every { observeGroupUseCase("group-456") } answers { groupFlow }
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()

            val viewModel = createViewModel()
            advanceUntilIdle()

            coVerify(exactly = 0) { setSelectedGroupUseCase(any(), any(), any()) }
        }

        @Test
        fun `clears only once even if selectedGroup emits null multiple times`() = runTest(testDispatcher) {
            val groupIdFlow = MutableStateFlow<String?>("stale-id")
            val groupFlow = MutableStateFlow<Group?>(null)
            every { getSelectedGroupIdUseCase() } returns groupIdFlow
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()
            every { observeSelectedGroupUseCase() } returns groupFlow
            every { observeGroupUseCase("stale-id") } answers { groupFlow }

            val viewModel = createViewModel()
            advanceUntilIdle()

            groupFlow.value = null
            advanceUntilIdle()

            coVerify(exactly = 1) { setSelectedGroupUseCase(null, null, null) }
        }
    }

    @Nested
    @DisplayName("auto clear selection on delete")
    inner class AutoClearSelectionOnDelete {

        @Test
        fun `clears selection when selectedGroup emits null`() = runTest(testDispatcher) {
            val groupIdFlow = MutableStateFlow<String?>("group-123")
            val groupFlow = MutableStateFlow<Group?>(mockk())

            every { getSelectedGroupIdUseCase() } returns groupIdFlow
            every { getSelectedGroupNameUseCase() } returns flowOf()
            every { getSelectedGroupCurrencyUseCase() } returns flowOf()
            every { observeSelectedGroupUseCase() } returns groupFlow
            every { observeGroupUseCase(any()) } answers { groupFlow }

            val viewModel = createViewModel()
            advanceUntilIdle()

            coVerify(exactly = 0) { setSelectedGroupUseCase(any(), any(), any()) }

            groupFlow.value = null
            advanceUntilIdle()

            coVerify(exactly = 1) { setSelectedGroupUseCase(null, null, null) }
        }
    }
}
