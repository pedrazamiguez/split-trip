package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.model.CropRect
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.model.ValidationResult
import es.pedrazamiguez.splittrip.domain.service.ProfileImageStorageService
import es.pedrazamiguez.splittrip.domain.service.UserValidationService
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserProfileUseCase
import es.pedrazamiguez.splittrip.features.profile.R
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action.EditProfileUiAction
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.EditProfileUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class EditProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase
    private lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase
    private lateinit var userValidationService: UserValidationService
    private lateinit var profileImageStorageService: ProfileImageStorageService
    private lateinit var viewModel: EditProfileViewModel

    private val testUser = User(
        userId = "user-123",
        email = "test@example.com",
        displayName = "Test User",
        profileImagePath = "https://example.com/photo.jpg",
        createdAt = LocalDateTime.of(2024, 6, 15, 10, 30),
        bio = "Hello",
        syncStatus = es.pedrazamiguez.splittrip.domain.enums.SyncStatus.SYNCED
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getCurrentUserProfileUseCase = mockk()
        updateUserProfileUseCase = mockk()
        userValidationService = mockk()
        profileImageStorageService = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = EditProfileViewModel(
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
            updateUserProfileUseCase = updateUserProfileUseCase,
            userValidationService = userValidationService,
            profileImageStorageService = profileImageStorageService
        )
    }

    @Nested
    inner class InitialLoad {

        @Test
        fun `loads profile details on init successfully`() = runTest(testDispatcher) {
            // Given
            coEvery { getCurrentUserProfileUseCase() } returns testUser

            // When
            createViewModel()
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals("Test User", state.displayName)
            assertEquals("Hello", state.bio)
            assertEquals("https://example.com/photo.jpg", state.avatarUrl)
        }

        @Test
        fun `emits ShowNotification when user profile is null`() = runTest(testDispatcher) {
            // Given
            coEvery { getCurrentUserProfileUseCase() } returns null

            // Start collecting actions
            val emittedActions = mutableListOf<EditProfileUiAction>()
            createViewModel()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(emittedActions.isNotEmpty())
            val action = emittedActions.first() as EditProfileUiAction.ShowNotification
            assertTrue(action.message is UiText.StringResource)
            assertEquals(R.string.profile_error_loading, (action.message as UiText.StringResource).resId)

            collectJob.cancel()
        }

        @Test
        fun `emits ShowNotification and sets isLoading false when getCurrentUserProfile throws exception`() = runTest(
            testDispatcher
        ) {
            // Given
            coEvery { getCurrentUserProfileUseCase() } throws RuntimeException("DB failure")

            // Start collecting actions
            val emittedActions = mutableListOf<EditProfileUiAction>()
            createViewModel()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(emittedActions.isNotEmpty())
            val action = emittedActions.first() as EditProfileUiAction.ShowNotification
            assertTrue(action.message is UiText.StringResource)
            assertEquals(R.string.profile_error_loading, (action.message as UiText.StringResource).resId)

            collectJob.cancel()
        }
    }

    @Nested
    inner class FormFields {

        @Test
        fun `OnDisplayNameChanged updates display name and resets error`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(EditProfileUiEvent.OnDisplayNameChanged("New Name"))

            // Then
            assertEquals("New Name", viewModel.uiState.value.displayName)
            assertNull(viewModel.uiState.value.displayNameError)
        }

        @Test
        fun `OnBioChanged updates bio and resets error`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(EditProfileUiEvent.OnBioChanged("New Bio"))

            // Then
            assertEquals("New Bio", viewModel.uiState.value.bio)
            assertNull(viewModel.uiState.value.bioError)
        }
    }

    @Nested
    inner class AvatarCropFlow {

        @Test
        fun `OnAvatarPicked sets showCropOverlay and path`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(EditProfileUiEvent.OnAvatarPicked("content://uri", "image/png"))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showCropOverlay)
            assertEquals("content://uri", state.cropSourceUri)
            assertEquals("image/png", state.localAvatarMimeType)
        }

        @Test
        fun `OnCropConfirmed invokes storage service and updates local path on success`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(EditProfileUiEvent.OnAvatarPicked("content://uri", "image/png"))

            val cropRect = CropRect(0f, 0f, 1f, 1f)
            coEvery {
                profileImageStorageService.saveAndCompressAvatar("user-123", "content://uri", cropRect)
            } returns "file://local/path.webp"

            // When
            viewModel.onEvent(EditProfileUiEvent.OnCropConfirmed(cropRect))
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertFalse(state.showCropOverlay)
            assertNull(state.cropSourceUri)
            assertEquals("file://local/path.webp", state.localAvatarPath)
            assertTrue(state.avatarUpdatedTime > 0L)
        }

        @Test
        fun `OnCropConfirmed emits ShowNotification on storage service error`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(EditProfileUiEvent.OnAvatarPicked("content://uri", "image/png"))

            val cropRect = CropRect(0f, 0f, 1f, 1f)
            coEvery {
                profileImageStorageService.saveAndCompressAvatar("user-123", "content://uri", cropRect)
            } throws RuntimeException("Out of Memory")

            val emittedActions = mutableListOf<EditProfileUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When
            viewModel.onEvent(EditProfileUiEvent.OnCropConfirmed(cropRect))
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertFalse(state.showCropOverlay)
            assertTrue(emittedActions.isNotEmpty())
            val action = emittedActions.first() as EditProfileUiAction.ShowNotification
            assertTrue(action.message is UiText.StringResource)
            assertEquals(R.string.edit_profile_error_image_process, (action.message as UiText.StringResource).resId)

            collectJob.cancel()
        }

        @Test
        fun `OnCropConfirmed ignores consecutive calls when isSaving is true`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(EditProfileUiEvent.OnAvatarPicked("content://uri", "image/png"))

            val cropRect = CropRect(0f, 0f, 1f, 1f)
            coEvery {
                profileImageStorageService.saveAndCompressAvatar("user-123", "content://uri", cropRect)
            } coAnswers {
                kotlinx.coroutines.delay(100)
                "file://local/path.webp"
            }

            // When
            viewModel.onEvent(EditProfileUiEvent.OnCropConfirmed(cropRect))
            viewModel.onEvent(EditProfileUiEvent.OnCropConfirmed(cropRect))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                profileImageStorageService.saveAndCompressAvatar("user-123", "content://uri", cropRect)
            }
        }

        @Test
        fun `OnCropCancelled resets crop overlay state`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(EditProfileUiEvent.OnAvatarPicked("content://uri", "image/png"))

            // When
            viewModel.onEvent(EditProfileUiEvent.OnCropCancelled)

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.showCropOverlay)
            assertNull(state.cropSourceUri)
        }

        @Test
        fun `OnAvatarRemoved resets avatar state`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            // When
            viewModel.onEvent(EditProfileUiEvent.OnAvatarRemoved)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.avatarUrl)
            assertNull(state.localAvatarPath)
        }
    }

    @Nested
    inner class SaveProfileFlow {

        @Test
        fun `OnSaveClicked performs validation and saves profile successfully`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(EditProfileUiEvent.OnDisplayNameChanged("New Name"))
            viewModel.onEvent(EditProfileUiEvent.OnBioChanged("New Bio"))

            every { userValidationService.validateDisplayName("New Name") } returns ValidationResult.Valid
            every { userValidationService.validateBio("New Bio") } returns ValidationResult.Valid

            coEvery {
                updateUserProfileUseCase("user-123", "New Name", "New Bio", "https://example.com/photo.jpg")
            } returns Result.success(Unit)

            val emittedActions = mutableListOf<EditProfileUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When
            viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertEquals(2, emittedActions.size)
            assertTrue(emittedActions[0] is EditProfileUiAction.ShowNotification)
            assertEquals(
                R.string.edit_profile_success_saved,
                ((emittedActions[0] as EditProfileUiAction.ShowNotification).message as UiText.StringResource).resId
            )
            assertTrue(emittedActions[1] is EditProfileUiAction.NavigateBack)

            collectJob.cancel()
        }

        @Test
        fun `OnSaveClicked displays name error when display name is blank`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(EditProfileUiEvent.OnDisplayNameChanged(""))

            every { userValidationService.validateDisplayName("") } returns
                ValidationResult.Invalid("Display name cannot be empty")
            every { userValidationService.validateBio(any()) } returns ValidationResult.Valid

            // When
            viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertNotNull(state.displayNameError)
            assertEquals(
                R.string.edit_profile_error_name_empty,
                (state.displayNameError as UiText.StringResource).resId
            )
            coVerify(exactly = 0) { updateUserProfileUseCase(any(), any(), any(), any()) }
        }

        @Test
        fun `OnSaveClicked displays bio error when bio is too long`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(EditProfileUiEvent.OnBioChanged("long bio..."))

            every { userValidationService.validateDisplayName("Test User") } returns ValidationResult.Valid
            every { userValidationService.validateBio("long bio...") } returns
                ValidationResult.Invalid("Bio cannot exceed 150 characters")

            // When
            viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertNotNull(state.bioError)
            assertEquals(R.string.edit_profile_error_bio_length, (state.bioError as UiText.StringResource).resId)
            coVerify(exactly = 0) { updateUserProfileUseCase(any(), any(), any(), any()) }
        }

        @Test
        fun `OnSaveClicked emits error notification on saveUseCase failure`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns testUser
            createViewModel()
            advanceUntilIdle()

            every { userValidationService.validateDisplayName("Test User") } returns ValidationResult.Valid
            every { userValidationService.validateBio("Hello") } returns ValidationResult.Valid

            coEvery {
                updateUserProfileUseCase("user-123", "Test User", "Hello", "https://example.com/photo.jpg")
            } returns Result.failure(RuntimeException("Network timeout"))

            val emittedActions = mutableListOf<EditProfileUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            // When
            viewModel.onEvent(EditProfileUiEvent.OnSaveClicked)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertTrue(emittedActions.isNotEmpty())
            val action = emittedActions.first() as EditProfileUiAction.ShowNotification
            assertTrue(action.message is UiText.DynamicString)
            assertEquals("Network timeout", (action.message as UiText.DynamicString).value)

            collectJob.cancel()
        }
    }
}
