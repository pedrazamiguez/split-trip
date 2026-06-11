package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkEmailPasswordUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.UnlinkProviderUseCase
import es.pedrazamiguez.splittrip.features.profile.R
import es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action.ProfileUiAction
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.ProfileUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileAccountLinkHandlerImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var linkGoogleAccountUseCase: LinkGoogleAccountUseCase
    private lateinit var linkEmailPasswordUseCase: LinkEmailPasswordUseCase
    private lateinit var unlinkProviderUseCase: UnlinkProviderUseCase

    private lateinit var handler: ProfileAccountLinkHandlerImpl
    private lateinit var stateFlow: MutableStateFlow<ProfileUiState>
    private lateinit var actionsChannel: Channel<ProfileUiAction>
    private var loadProfileCalled = false

    private val testProfile = ProfileUiModel(
        displayName = "Test User",
        email = "test@example.com",
        profileImageUrl = "https://example.com/photo.jpg",
        memberSinceText = "June 2024"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        linkGoogleAccountUseCase = mockk()
        linkEmailPasswordUseCase = mockk()
        unlinkProviderUseCase = mockk()

        handler = ProfileAccountLinkHandlerImpl(
            linkGoogleAccountUseCase = linkGoogleAccountUseCase,
            linkEmailPasswordUseCase = linkEmailPasswordUseCase,
            unlinkProviderUseCase = unlinkProviderUseCase
        )

        stateFlow = MutableStateFlow(ProfileUiState())
        actionsChannel = Channel(Channel.UNLIMITED)
        loadProfileCalled = false

        handler.bind(
            stateFlow = stateFlow,
            actionsChannel = actionsChannel,
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher),
            loadProfile = { loadProfileCalled = true }
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `handleLinkGoogleAccount success sets isLinking loads profile and emits success action`() = runTest(
        testDispatcher
    ) {
        coEvery { linkGoogleAccountUseCase("token") } returns Result.success(Unit)

        handler.handleLinkGoogleAccount("token")
        advanceUntilIdle()

        assertFalse(stateFlow.value.isLinking)
        assertTrue(loadProfileCalled)

        val action = actionsChannel.tryReceive().getOrNull()
        assertNotNull(action)
        assertTrue(action is ProfileUiAction.ShowSuccess)
        val message = (action as ProfileUiAction.ShowSuccess).message as UiText.StringResource
        assertEquals(R.string.profile_link_success, message.resId)
    }

    @Test
    fun `handleLinkGoogleAccount failure sets isLinking and emits error action`() = runTest(testDispatcher) {
        coEvery { linkGoogleAccountUseCase("token") } returns Result.failure(Exception("Link failed"))

        handler.handleLinkGoogleAccount("token")
        advanceUntilIdle()

        assertFalse(stateFlow.value.isLinking)
        assertFalse(loadProfileCalled)

        val action = actionsChannel.tryReceive().getOrNull()
        assertNotNull(action)
        assertTrue(action is ProfileUiAction.ShowError)
        val message = (action as ProfileUiAction.ShowError).message as UiText.StringResource
        assertEquals(R.string.profile_link_error_failed, message.resId)
    }

    @Test
    fun `handleShowLinkEmailDialog sets correct dialog states`() {
        stateFlow.value = stateFlow.value.copy(
            showLinkEmailDialog = false,
            linkPasswordInput = "some-pwd",
            linkConfirmPasswordInput = "some-pwd",
            linkPasswordError = UiText.DynamicString("error")
        )

        handler.handleShowLinkEmailDialog()

        val state = stateFlow.value
        assertTrue(state.showLinkEmailDialog)
        assertEquals("", state.linkPasswordInput)
        assertEquals("", state.linkConfirmPasswordInput)
        assertNull(state.linkPasswordError)
    }

    @Test
    fun `handleDismissLinkEmailDialog hides the dialog`() {
        stateFlow.value = stateFlow.value.copy(showLinkEmailDialog = true)

        handler.handleDismissLinkEmailDialog()

        assertFalse(stateFlow.value.showLinkEmailDialog)
    }

    @Test
    fun `handleLinkPasswordChanged updates password input`() {
        handler.handleLinkPasswordChanged("pwd")

        assertEquals("pwd", stateFlow.value.linkPasswordInput)
    }

    @Test
    fun `handleLinkConfirmPasswordChanged updates confirm password input`() {
        handler.handleLinkConfirmPasswordChanged("confirm-pwd")

        assertEquals("confirm-pwd", stateFlow.value.linkConfirmPasswordInput)
    }

    @Test
    fun `handleSubmitLinkEmailPassword success clears dialog and emits success`() =
        runTest(testDispatcher) {
            stateFlow.value = stateFlow.value.copy(
                profile = testProfile,
                linkPasswordInput = "password123",
                linkConfirmPasswordInput = "password123",
                showLinkEmailDialog = true
            )
            coEvery { linkEmailPasswordUseCase("test@example.com", "password123") } returns Result.success(Unit)

            handler.handleSubmitLinkEmailPassword()
            advanceUntilIdle()

            assertFalse(stateFlow.value.isLinking)
            assertFalse(stateFlow.value.showLinkEmailDialog)
            assertTrue(loadProfileCalled)

            val action = actionsChannel.tryReceive().getOrNull()
            assertNotNull(action)
            assertTrue(action is ProfileUiAction.ShowSuccess)
            val message = (action as ProfileUiAction.ShowSuccess).message as UiText.StringResource
            assertEquals(R.string.profile_link_success, message.resId)
        }

    @Test
    fun `handleSubmitLinkEmailPassword failure updates linkPasswordError`() = runTest(testDispatcher) {
        stateFlow.value = stateFlow.value.copy(
            profile = testProfile,
            linkPasswordInput = "password123",
            linkConfirmPasswordInput = "password123",
            showLinkEmailDialog = true
        )
        coEvery { linkEmailPasswordUseCase("test@example.com", "password123") } returns
            Result.failure(Exception("Failed"))

        handler.handleSubmitLinkEmailPassword()
        advanceUntilIdle()

        assertFalse(stateFlow.value.isLinking)
        assertTrue(stateFlow.value.showLinkEmailDialog)
        assertFalse(loadProfileCalled)

        val state = stateFlow.value
        assertNotNull(state.linkPasswordError)
        assertTrue(state.linkPasswordError is UiText.StringResource)
        assertEquals(R.string.profile_link_error_failed, (state.linkPasswordError as UiText.StringResource).resId)
    }

    @Test
    fun `handleSubmitLinkEmailPassword short password sets error and does not call usecase`() = runTest(
        testDispatcher
    ) {
        stateFlow.value = stateFlow.value.copy(
            profile = testProfile,
            linkPasswordInput = "123",
            linkConfirmPasswordInput = "123"
        )

        handler.handleSubmitLinkEmailPassword()
        advanceUntilIdle()

        coVerify(exactly = 0) { linkEmailPasswordUseCase(any(), any()) }
        assertFalse(loadProfileCalled)

        val state = stateFlow.value
        assertNotNull(state.linkPasswordError)
        assertTrue(state.linkPasswordError is UiText.StringResource)
        assertEquals(
            R.string.profile_link_error_password_length,
            (state.linkPasswordError as UiText.StringResource).resId
        )
    }

    @Test
    fun `handleSubmitLinkEmailPassword password mismatch sets error and does not call usecase`() = runTest(
        testDispatcher
    ) {
        stateFlow.value = stateFlow.value.copy(
            profile = testProfile,
            linkPasswordInput = "password123",
            linkConfirmPasswordInput = "password456"
        )

        handler.handleSubmitLinkEmailPassword()
        advanceUntilIdle()

        coVerify(exactly = 0) { linkEmailPasswordUseCase(any(), any()) }
        assertFalse(loadProfileCalled)

        val state = stateFlow.value
        assertNotNull(state.linkPasswordError)
        assertTrue(state.linkPasswordError is UiText.StringResource)
        assertEquals(
            R.string.profile_link_error_passwords_match,
            (state.linkPasswordError as UiText.StringResource).resId
        )
    }

    @Test
    fun `handleSubmitLinkEmailPassword missing email returns early and does not call usecase`() = runTest(
        testDispatcher
    ) {
        stateFlow.value = stateFlow.value.copy(
            profile = null,
            linkPasswordInput = "password123",
            linkConfirmPasswordInput = "password123"
        )

        handler.handleSubmitLinkEmailPassword()
        advanceUntilIdle()

        coVerify(exactly = 0) { linkEmailPasswordUseCase(any(), any()) }
        assertFalse(loadProfileCalled)
        assertNull(stateFlow.value.linkPasswordError)
    }

    @Test
    fun `handleUnlinkProvider success sets isLinking loads profile and emits success action`() = runTest(
        testDispatcher
    ) {
        stateFlow.value = stateFlow.value.copy(
            linkedProviders = persistentListOf(AuthProviderType.EMAIL_PASSWORD, AuthProviderType.GOOGLE)
        )
        coEvery { unlinkProviderUseCase(AuthProviderType.GOOGLE) } returns Result.success(Unit)

        handler.handleUnlinkProvider(AuthProviderType.GOOGLE)
        advanceUntilIdle()

        assertFalse(stateFlow.value.isLinking)
        assertTrue(loadProfileCalled)

        val action = actionsChannel.tryReceive().getOrNull()
        assertNotNull(action)
        assertTrue(action is ProfileUiAction.ShowSuccess)
        val message = (action as ProfileUiAction.ShowSuccess).message as UiText.StringResource
        assertEquals(R.string.profile_unlink_success, message.resId)
    }

    @Test
    fun `handleUnlinkProvider failure sets isLinking and emits error action`() = runTest(testDispatcher) {
        stateFlow.value = stateFlow.value.copy(
            linkedProviders = persistentListOf(AuthProviderType.EMAIL_PASSWORD, AuthProviderType.GOOGLE)
        )
        coEvery { unlinkProviderUseCase(AuthProviderType.GOOGLE) } returns Result.failure(Exception("Failed"))

        handler.handleUnlinkProvider(AuthProviderType.GOOGLE)
        advanceUntilIdle()

        assertFalse(stateFlow.value.isLinking)
        assertFalse(loadProfileCalled)

        val action = actionsChannel.tryReceive().getOrNull()
        assertNotNull(action)
        assertTrue(action is ProfileUiAction.ShowError)
        val message = (action as ProfileUiAction.ShowError).message as UiText.StringResource
        assertEquals(R.string.profile_unlink_error_failed, message.resId)
    }

    @Test
    fun `handleUnlinkProvider last provider emits error`() =
        runTest(testDispatcher) {
            stateFlow.value = stateFlow.value.copy(
                linkedProviders = persistentListOf(AuthProviderType.EMAIL_PASSWORD)
            )

            handler.handleUnlinkProvider(AuthProviderType.EMAIL_PASSWORD)
            advanceUntilIdle()

            coVerify(exactly = 0) { unlinkProviderUseCase(any()) }
            assertFalse(loadProfileCalled)

            val action = actionsChannel.tryReceive().getOrNull()
            assertNotNull(action)
            assertTrue(action is ProfileUiAction.ShowError)
            val message = (action as ProfileUiAction.ShowError).message as UiText.StringResource
            assertEquals(R.string.profile_unlink_error_last_provider, message.resId)
        }
}
