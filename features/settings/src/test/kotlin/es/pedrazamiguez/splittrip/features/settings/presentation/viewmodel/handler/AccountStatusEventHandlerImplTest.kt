package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.handler

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
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.time.LocalDateTime
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
@DisplayName("AccountStatusEventHandlerImpl")
class AccountStatusEventHandlerImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase
    private lateinit var getLinkedProvidersUseCase: GetLinkedProvidersUseCase
    private lateinit var linkGoogleAccountUseCase: LinkGoogleAccountUseCase
    private lateinit var linkEmailPasswordUseCase: LinkEmailPasswordUseCase
    private lateinit var unlinkProviderUseCase: UnlinkProviderUseCase
    private lateinit var accountStatusUiMapper: AccountStatusUiMapper

    private lateinit var handler: AccountStatusEventHandlerImpl
    private lateinit var stateFlow: MutableStateFlow<AccountStatusUiState>
    private lateinit var actionsFlow: MutableSharedFlow<AccountStatusUiAction>

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

        stateFlow = MutableStateFlow(AccountStatusUiState())
        actionsFlow = MutableSharedFlow()

        handler = AccountStatusEventHandlerImpl(
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
            getLinkedProvidersUseCase = getLinkedProvidersUseCase,
            linkGoogleAccountUseCase = linkGoogleAccountUseCase,
            linkEmailPasswordUseCase = linkEmailPasswordUseCase,
            unlinkProviderUseCase = unlinkProviderUseCase,
            accountStatusUiMapper = accountStatusUiMapper
        )

        handler.bind(stateFlow, actionsFlow, CoroutineScope(testDispatcher))

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

    @Nested
    @DisplayName("InitialLoad")
    inner class InitialLoad {

        @Test
        fun `loads account status successfully`() = runTest(testDispatcher) {
            handler.loadAccountStatus()
            advanceUntilIdle()

            val state = stateFlow.value
            assertFalse(state.isLoading)
            assertEquals("test@example.com", state.email)
            assertEquals("June 11, 2026", state.joinDateText)
            assertEquals(1, state.linkedProviders.size)
            assertTrue(state.linkedProviders.contains(AuthProviderType.GOOGLE))
        }

        @Test
        fun `emits ShowError action when user profile is null`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } returns null

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { emittedActions.add(it) }
            }

            handler.loadAccountStatus()
            advanceUntilIdle()

            assertTrue(emittedActions.isNotEmpty())
            val firstAction = emittedActions.first()
            assertTrue(firstAction is AccountStatusUiAction.ShowError)
            assertEquals(
                R.string.account_status_error_prefix,
                (firstAction as AccountStatusUiAction.ShowError).message.let {
                    (it as UiText.StringResource).resId
                }
            )
            collectJob.cancel()
        }

        @Test
        fun `emits ShowError action when usecase throws exception`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } throws RuntimeException("Network error")

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { emittedActions.add(it) }
            }

            handler.loadAccountStatus()
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
            coEvery { linkGoogleAccountUseCase("google-token") } returns Result.success(Unit)
            coEvery { getLinkedProvidersUseCase() } returns
                Result.success(listOf(AuthProviderType.GOOGLE, AuthProviderType.EMAIL_PASSWORD))

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { emittedActions.add(it) }
            }

            handler.handleLinkGoogle("google-token")
            advanceUntilIdle()

            assertFalse(stateFlow.value.isLinking)
            assertTrue(emittedActions.any { it is AccountStatusUiAction.ShowSuccess })
            coVerify(exactly = 1) { linkGoogleAccountUseCase("google-token") }
            collectJob.cancel()
        }

        @Test
        fun `fails to link Google account and emits ShowError`() = runTest(testDispatcher) {
            coEvery {
                linkGoogleAccountUseCase("google-token")
            } returns Result.failure(RuntimeException("Google error"))

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { emittedActions.add(it) }
            }

            handler.handleLinkGoogle("google-token")
            advanceUntilIdle()

            assertFalse(stateFlow.value.isLinking)
            assertTrue(emittedActions.any { it is AccountStatusUiAction.ShowError })
            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("LinkEmailPassword")
    inner class LinkEmailPassword {

        @Test
        fun `shows and dismisses dialog`() = runTest(testDispatcher) {
            handler.handleShowLinkEmailDialog()
            assertTrue(stateFlow.value.showLinkEmailDialog)

            handler.handleDismissLinkEmailDialog()
            assertFalse(stateFlow.value.showLinkEmailDialog)
        }

        @Test
        fun `updates input values`() = runTest(testDispatcher) {
            handler.handleLinkPasswordChanged("pass123")
            handler.handleLinkConfirmPasswordChanged("pass456")

            assertEquals("pass123", stateFlow.value.linkPasswordInput)
            assertEquals("pass456", stateFlow.value.linkConfirmPasswordInput)
        }

        @Test
        fun `submitting with empty fields sets error`() = runTest(testDispatcher) {
            handler.handleShowLinkEmailDialog()
            handler.handleSubmitLinkEmailPassword()

            assertNotNull(stateFlow.value.linkPasswordError)
            assertTrue(stateFlow.value.linkPasswordError is UiText.StringResource)
            assertEquals(
                R.string.account_status_link_email_dialog_error_empty,
                (stateFlow.value.linkPasswordError as UiText.StringResource).resId
            )
        }

        @Test
        fun `submitting with mismatched passwords sets error`() = runTest(testDispatcher) {
            // Setup email state
            stateFlow.value = stateFlow.value.copy(email = "test@example.com")
            handler.handleLinkPasswordChanged("password123")
            handler.handleLinkConfirmPasswordChanged("password456")
            handler.handleSubmitLinkEmailPassword()

            assertNotNull(stateFlow.value.linkPasswordError)
            assertTrue(stateFlow.value.linkPasswordError is UiText.StringResource)
            assertEquals(
                R.string.account_status_link_email_dialog_error_mismatch,
                (stateFlow.value.linkPasswordError as UiText.StringResource).resId
            )
        }

        @Test
        fun `submitting successful email password link clears state and reloads`() = runTest(testDispatcher) {
            // Setup email state
            stateFlow.value = stateFlow.value.copy(email = "test@example.com")
            coEvery { linkEmailPasswordUseCase("test@example.com", "password123") } returns Result.success(Unit)
            coEvery { getLinkedProvidersUseCase() } returns
                Result.success(listOf(AuthProviderType.GOOGLE, AuthProviderType.EMAIL_PASSWORD))

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { emittedActions.add(it) }
            }

            handler.handleLinkPasswordChanged("password123")
            handler.handleLinkConfirmPasswordChanged("password123")
            handler.handleSubmitLinkEmailPassword()
            advanceUntilIdle()

            assertFalse(stateFlow.value.isLinking)
            assertFalse(stateFlow.value.showLinkEmailDialog)
            assertNull(stateFlow.value.linkPasswordError)
            assertTrue(emittedActions.any { it is AccountStatusUiAction.ShowSuccess })
            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("UnlinkProvider")
    inner class UnlinkProvider {

        @Test
        fun `unlinking last remaining provider returns error`() = runTest(testDispatcher) {
            // Setup only one linked provider
            stateFlow.value = stateFlow.value.copy(linkedProviders = listOf(AuthProviderType.GOOGLE).toImmutableList())

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { emittedActions.add(it) }
            }

            handler.handleUnlinkProvider(AuthProviderType.GOOGLE)
            advanceUntilIdle()

            assertTrue(emittedActions.any { it is AccountStatusUiAction.ShowError })
            collectJob.cancel()
        }

        @Test
        fun `unlinking provider successfully reloads account status`() = runTest(testDispatcher) {
            // Setup two linked providers
            stateFlow.value = stateFlow.value.copy(
                linkedProviders = listOf(AuthProviderType.GOOGLE, AuthProviderType.EMAIL_PASSWORD).toImmutableList()
            )
            coEvery { getLinkedProvidersUseCase() } returnsMany listOf(
                Result.success(listOf(AuthProviderType.GOOGLE, AuthProviderType.EMAIL_PASSWORD)),
                Result.success(listOf(AuthProviderType.EMAIL_PASSWORD))
            )
            coEvery { unlinkProviderUseCase(AuthProviderType.GOOGLE) } returns Result.success(Unit)

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { emittedActions.add(it) }
            }

            handler.handleUnlinkProvider(AuthProviderType.GOOGLE)
            advanceUntilIdle()

            assertFalse(stateFlow.value.isLinking)
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
            handler.handleShowDeleteAccountDialog()
            assertTrue(stateFlow.value.showDeleteAccountDialog)

            handler.handleDismissDeleteAccountDialog()
            assertFalse(stateFlow.value.showDeleteAccountDialog)
        }

        @Test
        fun `confirming delete shows notification that it is out of scope`() = runTest(testDispatcher) {
            handler.handleShowDeleteAccountDialog()

            val emittedActions = mutableListOf<AccountStatusUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { emittedActions.add(it) }
            }

            handler.handleConfirmDeleteAccount()
            advanceUntilIdle()

            assertFalse(stateFlow.value.showDeleteAccountDialog)
            assertTrue(emittedActions.any { it is AccountStatusUiAction.ShowSuccess })
            val action = emittedActions.first {
                it is AccountStatusUiAction.ShowSuccess
            } as AccountStatusUiAction.ShowSuccess
            assertEquals(
                R.string.account_status_delete_request_submitted,
                (action.message as UiText.StringResource).resId
            )
            collectJob.cancel()
        }
    }
}
