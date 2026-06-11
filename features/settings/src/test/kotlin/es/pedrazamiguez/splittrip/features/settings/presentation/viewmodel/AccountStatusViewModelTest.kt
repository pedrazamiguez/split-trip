package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.usecase.auth.GetLinkedProvidersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkEmailPasswordUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.UnlinkProviderUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.AccountStatusUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.AccountStatusUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountStatusUiEvent
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AccountStatusViewModel")
class AccountStatusViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase
    private lateinit var getLinkedProvidersUseCase: GetLinkedProvidersUseCase
    private lateinit var linkGoogleAccountUseCase: LinkGoogleAccountUseCase
    private lateinit var linkEmailPasswordUseCase: LinkEmailPasswordUseCase
    private lateinit var unlinkProviderUseCase: UnlinkProviderUseCase
    private lateinit var accountStatusUiMapper: AccountStatusUiMapper
    private lateinit var viewModel: AccountStatusViewModel

    private val testUser = User(
        userId = "user-123",
        email = "test@example.com",
        displayName = "Test User",
        profileImagePath = null,
        createdAt = LocalDateTime.of(2026, 6, 11, 14, 20)
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getCurrentUserProfileUseCase = mockk()
        getLinkedProvidersUseCase = mockk()
        linkGoogleAccountUseCase = mockk()
        linkEmailPasswordUseCase = mockk()
        unlinkProviderUseCase = mockk()
        accountStatusUiMapper = mockk()

        every { accountStatusUiMapper.formatJoinDate(any()) } returns "June 11, 2026"
        coEvery { getCurrentUserProfileUseCase() } returns testUser
        coEvery { getLinkedProvidersUseCase() } returns Result.success(listOf(AuthProviderType.GOOGLE))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel() {
        viewModel = AccountStatusViewModel(
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
            getLinkedProvidersUseCase = getLinkedProvidersUseCase,
            linkGoogleAccountUseCase = linkGoogleAccountUseCase,
            linkEmailPasswordUseCase = linkEmailPasswordUseCase,
            unlinkProviderUseCase = unlinkProviderUseCase,
            accountStatusUiMapper = accountStatusUiMapper
        )
    }

    @Nested
    @DisplayName("InitialLoad")
    inner class InitialLoad {

        @Test
        fun `loads account status successfully on init`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals("test@example.com", state.email)
            assertEquals("June 11, 2026", state.joinDateText)
            assertEquals(1, state.linkedProviders.size)
            assertTrue(state.linkedProviders.contains(AuthProviderType.GOOGLE))
        }

        @Test
        fun `emits ShowError action when user profile is null`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns null
            createViewModel()

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            advanceUntilIdle()

            assertTrue(emittedActions.isNotEmpty())
            assertTrue(emittedActions.first() is AccountStatusUiAction.ShowError)
            collectJob.cancel()
        }

        @Test
        fun `emits ShowError action when usecase throws exception`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } throws RuntimeException("Network error")
            createViewModel()

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            advanceUntilIdle()

            assertTrue(emittedActions.isNotEmpty())
            assertTrue(emittedActions.first() is AccountStatusUiAction.ShowError)
            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("LinkGoogle")
    inner class LinkGoogle {

        @Test
        fun `links Google account successfully and reloads`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            coEvery { linkGoogleAccountUseCase("google-token") } returns Result.success(Unit)
            coEvery { getLinkedProvidersUseCase() } returns
                Result.success(listOf(AuthProviderType.GOOGLE, AuthProviderType.EMAIL_PASSWORD))

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            viewModel.onEvent(AccountStatusUiEvent.LinkGoogle("google-token"))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLinking)
            assertTrue(emittedActions.any { it is AccountStatusUiAction.ShowSuccess })
            coVerify(exactly = 1) { linkGoogleAccountUseCase("google-token") }
            collectJob.cancel()
        }

        @Test
        fun `fails to link Google account and emits ShowError`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            coEvery {
                linkGoogleAccountUseCase("google-token")
            } returns Result.failure(RuntimeException("Google error"))

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            viewModel.onEvent(AccountStatusUiEvent.LinkGoogle("google-token"))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLinking)
            assertTrue(emittedActions.any { it is AccountStatusUiAction.ShowError })
            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("LinkEmailPassword")
    inner class LinkEmailPassword {

        @Test
        fun `shows and dismisses dialog`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(AccountStatusUiEvent.ShowLinkEmailDialog)
            assertTrue(viewModel.uiState.value.showLinkEmailDialog)

            viewModel.onEvent(AccountStatusUiEvent.DismissLinkEmailDialog)
            assertFalse(viewModel.uiState.value.showLinkEmailDialog)
        }

        @Test
        fun `updates input values`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(AccountStatusUiEvent.LinkPasswordChanged("pass123"))
            viewModel.onEvent(AccountStatusUiEvent.LinkConfirmPasswordChanged("pass456"))

            assertEquals("pass123", viewModel.uiState.value.linkPasswordInput)
            assertEquals("pass456", viewModel.uiState.value.linkConfirmPasswordInput)
        }

        @Test
        fun `submitting with empty fields sets error`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(AccountStatusUiEvent.ShowLinkEmailDialog)
            viewModel.onEvent(AccountStatusUiEvent.SubmitLinkEmailPassword)

            assertNotNull(viewModel.uiState.value.linkPasswordError)
            assertTrue(viewModel.uiState.value.linkPasswordError is UiText.StringResource)
            assertEquals(
                R.string.account_status_link_email_dialog_error_empty,
                (viewModel.uiState.value.linkPasswordError as UiText.StringResource).resId
            )
        }

        @Test
        fun `submitting with mismatched passwords sets error`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(AccountStatusUiEvent.LinkPasswordChanged("password123"))
            viewModel.onEvent(AccountStatusUiEvent.LinkConfirmPasswordChanged("password456"))
            viewModel.onEvent(AccountStatusUiEvent.SubmitLinkEmailPassword)

            assertNotNull(viewModel.uiState.value.linkPasswordError)
            assertTrue(viewModel.uiState.value.linkPasswordError is UiText.StringResource)
            assertEquals(
                R.string.account_status_link_email_dialog_error_mismatch,
                (viewModel.uiState.value.linkPasswordError as UiText.StringResource).resId
            )
        }

        @Test
        fun `submitting successful email password link clears state and reloads`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            coEvery { linkEmailPasswordUseCase("test@example.com", "password123") } returns Result.success(Unit)
            coEvery { getLinkedProvidersUseCase() } returns
                Result.success(listOf(AuthProviderType.GOOGLE, AuthProviderType.EMAIL_PASSWORD))

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            viewModel.onEvent(AccountStatusUiEvent.LinkPasswordChanged("password123"))
            viewModel.onEvent(AccountStatusUiEvent.LinkConfirmPasswordChanged("password123"))
            viewModel.onEvent(AccountStatusUiEvent.SubmitLinkEmailPassword)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLinking)
            assertFalse(viewModel.uiState.value.showLinkEmailDialog)
            assertNull(viewModel.uiState.value.linkPasswordError)
            assertTrue(emittedActions.any { it is AccountStatusUiAction.ShowSuccess })
            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("UnlinkProvider")
    inner class UnlinkProvider {

        @Test
        fun `unlinking last remaining provider returns error`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            viewModel.onEvent(AccountStatusUiEvent.UnlinkProvider(AuthProviderType.GOOGLE))
            advanceUntilIdle()

            assertTrue(emittedActions.any { it is AccountStatusUiAction.ShowError })
            collectJob.cancel()
        }

        @Test
        fun `unlinking provider successfully reloads account status`() = runTest(testDispatcher) {
            // Mock with 2 linked providers to allow unlinking
            coEvery { getLinkedProvidersUseCase() } returnsMany listOf(
                Result.success(listOf(AuthProviderType.GOOGLE, AuthProviderType.EMAIL_PASSWORD)),
                Result.success(listOf(AuthProviderType.EMAIL_PASSWORD))
            )
            createViewModel()
            advanceUntilIdle()

            coEvery { unlinkProviderUseCase(AuthProviderType.GOOGLE) } returns Result.success(Unit)

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            viewModel.onEvent(AccountStatusUiEvent.UnlinkProvider(AuthProviderType.GOOGLE))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLinking)
            assertTrue(emittedActions.any { it is AccountStatusUiAction.ShowSuccess })
            coVerify(exactly = 1) { unlinkProviderUseCase(AuthProviderType.GOOGLE) }
            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("DeleteAccount")
    inner class DeleteAccount {

        @Test
        fun `shows and dismisses delete dialog`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(AccountStatusUiEvent.ShowDeleteAccountDialog)
            assertTrue(viewModel.uiState.value.showDeleteAccountDialog)

            viewModel.onEvent(AccountStatusUiEvent.DismissDeleteAccountDialog)
            assertFalse(viewModel.uiState.value.showDeleteAccountDialog)
        }

        @Test
        fun `confirming delete shows notification that it is out of scope`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(AccountStatusUiEvent.ShowDeleteAccountDialog)
            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            viewModel.onEvent(AccountStatusUiEvent.ConfirmDeleteAccount)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showDeleteAccountDialog)
            assertTrue(emittedActions.any { it is AccountStatusUiAction.ShowSuccess })
            collectJob.cancel()
        }
    }
}
