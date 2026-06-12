package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.profile.presentation.mapper.ProfileUiMapper
import es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action.ProfileUiAction
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.ProfileUiEvent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase
    private lateinit var observeCurrentUserProfileUseCase: ObserveCurrentUserProfileUseCase
    private lateinit var profileUiMapper: ProfileUiMapper
    private lateinit var viewModel: ProfileViewModel

    private val testUser = User(
        userId = "user-123",
        email = "test@example.com",
        displayName = "Test User",
        profileImagePath = "https://example.com/photo.jpg",
        createdAt = LocalDateTime.of(2024, 6, 15, 10, 30)
    )

    private val testProfileUiModel = ProfileUiModel(
        displayName = "Test User",
        email = "test@example.com",
        profileImageUrl = "https://example.com/photo.jpg",
        bio = ""
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getCurrentUserProfileUseCase = mockk()
        observeCurrentUserProfileUseCase = mockk()
        profileUiMapper = mockk()

        every { observeCurrentUserProfileUseCase() } returns kotlinx.coroutines.flow.flowOf(null)
        every { profileUiMapper.toProfileUiModel(testUser) } returns testProfileUiModel
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = ProfileViewModel(
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
            observeCurrentUserProfileUseCase = observeCurrentUserProfileUseCase,
            profileUiMapper = profileUiMapper
        )
    }

    @Nested
    inner class InitialLoad {

        @Test
        fun `loads profile successfully on init`() = runTest(testDispatcher) {
            // Given
            every { observeCurrentUserProfileUseCase() } returns kotlinx.coroutines.flow.flowOf(testUser)
            coEvery { getCurrentUserProfileUseCase() } returns testUser

            // When
            createViewModel()
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.profile)
            assertEquals("Test User", state.profile?.displayName)
            assertEquals("test@example.com", state.profile?.email)
            assertEquals("https://example.com/photo.jpg", state.profile?.profileImageUrl)
            assertEquals("", state.profile?.bio)
            assertFalse(state.hasError)
        }

        @Test
        fun `sets error state when user is null`() = runTest(testDispatcher) {
            // Given
            coEvery { getCurrentUserProfileUseCase() } returns null

            // When
            createViewModel()
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.profile)
            assertTrue(state.hasError)
        }

        @Test
        fun `emits ShowError action when user is null`() = runTest(testDispatcher) {
            // Given — init loads null, so set up accordingly
            coEvery { getCurrentUserProfileUseCase() } returns null
            createViewModel()

            // Start collecting actions
            val emittedActions = mutableListOf<ProfileUiAction>()
            val collectJob = backgroundScope.launch {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When — reload triggers another null → ShowError
            viewModel.onEvent(ProfileUiEvent.LoadProfile)
            advanceUntilIdle()

            // Then — actions from init + reload (both emit ShowError)
            assertTrue(emittedActions.isNotEmpty())
            assertTrue(emittedActions.all { it is ProfileUiAction.ShowError })

            collectJob.cancel()
        }

        @Test
        fun `sets error state when use case throws exception`() = runTest(testDispatcher) {
            // Given
            coEvery { getCurrentUserProfileUseCase() } throws RuntimeException("Network error")

            // When
            createViewModel()
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.profile)
            assertTrue(state.hasError)
        }

        @Test
        fun `emits ShowError action when use case throws exception`() = runTest(testDispatcher) {
            // Given — init throws, set up accordingly
            coEvery { getCurrentUserProfileUseCase() } throws RuntimeException("Network error")
            createViewModel()

            // Start collecting actions
            val emittedActions = mutableListOf<ProfileUiAction>()
            val collectJob = backgroundScope.launch {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When — reload triggers another exception → ShowError
            viewModel.onEvent(ProfileUiEvent.LoadProfile)
            advanceUntilIdle()

            // Then — actions from init + reload (both emit ShowError)
            assertTrue(emittedActions.isNotEmpty())
            assertTrue(emittedActions.all { it is ProfileUiAction.ShowError })

            collectJob.cancel()
        }

        @Test
        fun `updates profile state when observe flow emits`() = runTest(testDispatcher) {
            // Given
            val userFlow = kotlinx.coroutines.flow.MutableSharedFlow<User?>()
            every { observeCurrentUserProfileUseCase() } returns userFlow
            coEvery { getCurrentUserProfileUseCase() } returns null

            createViewModel()
            advanceUntilIdle()

            // Initially null
            assertNull(viewModel.uiState.value.profile)

            // When - flow emits a user
            userFlow.emit(testUser)
            advanceUntilIdle()

            // Then - UI updates
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.profile)
            assertEquals("Test User", state.profile?.displayName)
            assertFalse(state.hasError)
        }
    }

    @Nested
    inner class ReloadProfile {

        @Test
        fun `reloads profile on LoadProfile event`() = runTest(testDispatcher) {
            // Given - first load returns null
            val userFlow = kotlinx.coroutines.flow.MutableSharedFlow<User?>()
            every { observeCurrentUserProfileUseCase() } returns userFlow
            coEvery { getCurrentUserProfileUseCase() } returns null
            createViewModel()
            advanceUntilIdle()

            // Verify initial error state
            assertNull(viewModel.uiState.value.profile)

            // Given - second load returns user
            coEvery { getCurrentUserProfileUseCase() } returns testUser

            // When - reload triggers and flow emits user
            viewModel.onEvent(ProfileUiEvent.LoadProfile)
            userFlow.emit(testUser)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.profile)
            assertEquals("Test User", state.profile?.displayName)
            assertFalse(state.hasError)
        }
    }
}
