package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.usecase.auth.GetLinkedProvidersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkEmailPasswordUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.UnlinkProviderUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.profile.R
import es.pedrazamiguez.splittrip.features.profile.presentation.mapper.ProfileUiMapper
import es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action.ProfileUiAction
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.ProfileUiEvent
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.handler.ProfileAccountLinkHandlerImpl
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
    private lateinit var profileUiMapper: ProfileUiMapper
    private lateinit var linkGoogleAccountUseCase: LinkGoogleAccountUseCase
    private lateinit var linkEmailPasswordUseCase: LinkEmailPasswordUseCase
    private lateinit var unlinkProviderUseCase: UnlinkProviderUseCase
    private lateinit var getLinkedProvidersUseCase: GetLinkedProvidersUseCase
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
        memberSinceText = "June 2024"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getCurrentUserProfileUseCase = mockk()
        profileUiMapper = mockk()
        linkGoogleAccountUseCase = mockk()
        linkEmailPasswordUseCase = mockk()
        unlinkProviderUseCase = mockk()
        getLinkedProvidersUseCase = mockk()

        every { profileUiMapper.toProfileUiModel(testUser) } returns testProfileUiModel
        coEvery { getLinkedProvidersUseCase() } returns Result.success(listOf(AuthProviderType.EMAIL_PASSWORD))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = ProfileViewModel(
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
            profileUiMapper = profileUiMapper,
            getLinkedProvidersUseCase = getLinkedProvidersUseCase,
            profileAccountLinkHandler = ProfileAccountLinkHandlerImpl(
                linkGoogleAccountUseCase = linkGoogleAccountUseCase,
                linkEmailPasswordUseCase = linkEmailPasswordUseCase,
                unlinkProviderUseCase = unlinkProviderUseCase
            )
        )
    }

    @Nested
    inner class InitialLoad {

        @Test
        fun `loads profile successfully on init`() = runTest(testDispatcher) {
            // Given
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
            assertEquals("June 2024", state.profile?.memberSinceText)
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
            viewModel.onEvent(
                ProfileUiEvent.LoadProfile
            )
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
            viewModel.onEvent(
                ProfileUiEvent.LoadProfile
            )
            advanceUntilIdle()

            // Then — actions from init + reload (both emit ShowError)
            assertTrue(emittedActions.isNotEmpty())
            assertTrue(emittedActions.all { it is ProfileUiAction.ShowError })

            collectJob.cancel()
        }
    }

    @Nested
    inner class ReloadProfile {

        @Test
        fun `reloads profile on LoadProfile event`() = runTest(testDispatcher) {
            // Given - first load returns null
            coEvery { getCurrentUserProfileUseCase() } returns null
            createViewModel()
            advanceUntilIdle()

            // Verify initial error state
            assertNull(viewModel.uiState.value.profile)

            // Given - second load returns user
            coEvery { getCurrentUserProfileUseCase() } returns testUser

            // When
            viewModel.onEvent(
                ProfileUiEvent.LoadProfile
            )
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.profile)
            assertEquals("Test User", state.profile?.displayName)
            assertFalse(state.hasError)
        }
    }

    @Nested
    inner class AccountLinking {

        @Test
        fun `LinkGoogleAccount success refreshes profile and emits ShowSuccess`() = runTest(testDispatcher) {
            // Given
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            coEvery { linkGoogleAccountUseCase("google-token") } returns Result.success(Unit)
            coEvery { getLinkedProvidersUseCase() } returns
                Result.success(listOf(AuthProviderType.EMAIL_PASSWORD, AuthProviderType.GOOGLE))

            createViewModel()
            advanceUntilIdle()

            val emittedActions = mutableListOf<ProfileUiAction>()
            val collectJob = backgroundScope.launch {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When
            viewModel.onEvent(ProfileUiEvent.LinkGoogleAccount("google-token"))
            advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.isLinking)
            assertTrue(viewModel.uiState.value.linkedProviders.contains(AuthProviderType.GOOGLE))
            assertTrue(emittedActions.any { it is ProfileUiAction.ShowSuccess })

            collectJob.cancel()
        }

        @Test
        fun `LinkGoogleAccount failure emits ShowError`() = runTest(testDispatcher) {
            // Given
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            coEvery { linkGoogleAccountUseCase("google-token") } returns Result.failure(RuntimeException("Link failed"))

            createViewModel()
            advanceUntilIdle()

            val emittedActions = mutableListOf<ProfileUiAction>()
            val collectJob = launch {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When
            viewModel.onEvent(ProfileUiEvent.LinkGoogleAccount("google-token"))
            advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.isLinking)
            assertTrue(emittedActions.any { it is ProfileUiAction.ShowError })

            collectJob.cancel()
        }

        @Test
        fun `SubmitLinkEmailPassword success refreshes profile and emits ShowSuccess`() = runTest(testDispatcher) {
            // Given
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            coEvery { linkEmailPasswordUseCase("test@example.com", "password123") } returns Result.success(Unit)
            coEvery { getLinkedProvidersUseCase() } returns
                Result.success(listOf(AuthProviderType.EMAIL_PASSWORD, AuthProviderType.GOOGLE))

            createViewModel()
            advanceUntilIdle()

            val emittedActions = mutableListOf<ProfileUiAction>()
            val collectJob = backgroundScope.launch {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When
            viewModel.onEvent(ProfileUiEvent.LinkPasswordChanged("password123"))
            viewModel.onEvent(ProfileUiEvent.LinkConfirmPasswordChanged("password123"))
            viewModel.onEvent(ProfileUiEvent.SubmitLinkEmailPassword)
            advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.isLinking)
            assertFalse(viewModel.uiState.value.showLinkEmailDialog)
            assertTrue(emittedActions.any { it is ProfileUiAction.ShowSuccess })

            collectJob.cancel()
        }

        @Test
        fun `SubmitLinkEmailPassword short password sets error`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ProfileUiEvent.LinkPasswordChanged("123"))
            viewModel.onEvent(ProfileUiEvent.LinkConfirmPasswordChanged("123"))
            viewModel.onEvent(ProfileUiEvent.SubmitLinkEmailPassword)
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.linkPasswordError)
            assertTrue(viewModel.uiState.value.linkPasswordError is UiText.StringResource)
            assertEquals(
                R.string.profile_link_error_password_length,
                (viewModel.uiState.value.linkPasswordError as UiText.StringResource).resId
            )
        }

        @Test
        fun `SubmitLinkEmailPassword passwords mismatch sets error`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(ProfileUiEvent.LinkPasswordChanged("password123"))
            viewModel.onEvent(ProfileUiEvent.LinkConfirmPasswordChanged("password456"))
            viewModel.onEvent(ProfileUiEvent.SubmitLinkEmailPassword)
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.linkPasswordError)
            assertTrue(viewModel.uiState.value.linkPasswordError is UiText.StringResource)
            assertEquals(
                R.string.profile_link_error_passwords_match,
                (viewModel.uiState.value.linkPasswordError as UiText.StringResource).resId
            )
        }

        @Test
        fun `UnlinkProvider success refreshes profile and emits ShowSuccess`() = runTest(testDispatcher) {
            // Given
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            coEvery { unlinkProviderUseCase(AuthProviderType.GOOGLE) } returns Result.success(Unit)
            // Initial linked providers has both email and google
            coEvery { getLinkedProvidersUseCase() } returnsMany listOf(
                Result.success(listOf(AuthProviderType.EMAIL_PASSWORD, AuthProviderType.GOOGLE)), // init load
                Result.success(listOf(AuthProviderType.EMAIL_PASSWORD)) // after unlink reload
            )

            createViewModel()
            advanceUntilIdle()

            val emittedActions = mutableListOf<ProfileUiAction>()
            val collectJob = backgroundScope.launch {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When
            viewModel.onEvent(ProfileUiEvent.UnlinkProvider(AuthProviderType.GOOGLE))
            advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.isLinking)
            assertEquals(1, viewModel.uiState.value.linkedProviders.size)
            assertFalse(viewModel.uiState.value.linkedProviders.contains(AuthProviderType.GOOGLE))
            assertTrue(emittedActions.any { it is ProfileUiAction.ShowSuccess })

            collectJob.cancel()
        }

        @Test
        fun `UnlinkProvider for last remaining provider fails and emits ShowError`() = runTest(testDispatcher) {
            // Given - only EMAIL_PASSWORD is linked
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            coEvery { getLinkedProvidersUseCase() } returns Result.success(listOf(AuthProviderType.EMAIL_PASSWORD))

            createViewModel()
            advanceUntilIdle()

            val emittedActions = mutableListOf<ProfileUiAction>()
            val collectJob = launch {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When
            viewModel.onEvent(ProfileUiEvent.UnlinkProvider(AuthProviderType.EMAIL_PASSWORD))
            advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.isLinking)
            assertTrue(emittedActions.any { it is ProfileUiAction.ShowError })

            collectJob.cancel()
        }
    }
}
