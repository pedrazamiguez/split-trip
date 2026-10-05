package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.enums.BiometricCapability
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.usecase.auth.GetLinkedProvidersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.IsUserAnonymousUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SendPasswordResetEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetBiometricCapabilityUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetBiometricLockEnabledUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetBiometricLockEnabledUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.AccountSecurityUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.AccountSecurityUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountSecurityUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AccountSecurityViewModel")
class AccountSecurityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase
    private lateinit var isUserAnonymousUseCase: IsUserAnonymousUseCase
    private lateinit var getLinkedProvidersUseCase: GetLinkedProvidersUseCase
    private lateinit var sendPasswordResetEmailUseCase: SendPasswordResetEmailUseCase
    private lateinit var getBiometricCapabilityUseCase: GetBiometricCapabilityUseCase
    private lateinit var getBiometricLockEnabledUseCase: GetBiometricLockEnabledUseCase
    private lateinit var setBiometricLockEnabledUseCase: SetBiometricLockEnabledUseCase
    private lateinit var accountSecurityUiMapper: AccountSecurityUiMapper

    private val testUser = User(
        userId = "user_123",
        email = "alex@example.com",
        displayName = "Alex"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getCurrentUserProfileUseCase = mockk()
        isUserAnonymousUseCase = mockk()
        getLinkedProvidersUseCase = mockk()
        sendPasswordResetEmailUseCase = mockk()
        getBiometricCapabilityUseCase = mockk()
        getBiometricLockEnabledUseCase = mockk()
        setBiometricLockEnabledUseCase = mockk(relaxed = true)
        accountSecurityUiMapper = mockk()

        coEvery { getCurrentUserProfileUseCase() } returns testUser
        every { isUserAnonymousUseCase() } returns flowOf(false)
        coEvery { getLinkedProvidersUseCase() } returns Result.success(listOf(AuthProviderType.EMAIL_PASSWORD))
        every { getBiometricCapabilityUseCase() } returns BiometricCapability.AVAILABLE
        every { getBiometricLockEnabledUseCase() } returns flowOf(false)
        every { accountSecurityUiMapper.formatPasswordResetSuccessMessage(any()) } returns UiText.StringResource(
            R.string.account_security_password_reset_sent,
            "alex@example.com"
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AccountSecurityViewModel = AccountSecurityViewModel(
        getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
        isUserAnonymousUseCase = isUserAnonymousUseCase,
        getLinkedProvidersUseCase = getLinkedProvidersUseCase,
        sendPasswordResetEmailUseCase = sendPasswordResetEmailUseCase,
        getBiometricCapabilityUseCase = getBiometricCapabilityUseCase,
        getBiometricLockEnabledUseCase = getBiometricLockEnabledUseCase,
        setBiometricLockEnabledUseCase = setBiometricLockEnabledUseCase,
        accountSecurityUiMapper = accountSecurityUiMapper
    )

    @Nested
    @DisplayName("Initial state loading")
    inner class InitialState {

        @Test
        fun `initial state loads user profile, linked providers, and biometric capability`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals("alex@example.com", state.email)
            assertFalse(state.isAnonymous)
            assertEquals(1, state.linkedProviders.size)
            assertEquals(AuthProviderType.EMAIL_PASSWORD, state.linkedProviders.first())
            assertTrue(state.canResetPassword)
            assertFalse(state.biometricLockEnabled)
            assertEquals(BiometricCapability.AVAILABLE, state.biometricCapability)
            assertTrue(state.isBiometricToggleEnabled)
        }

        @Test
        fun `initial state with NO_HARDWARE capability disables toggle`() = runTest(testDispatcher) {
            every { getBiometricCapabilityUseCase() } returns BiometricCapability.NO_HARDWARE

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(BiometricCapability.NO_HARDWARE, state.biometricCapability)
            assertFalse(state.isBiometricToggleEnabled)
        }

        @Test
        fun `guest user state sets isAnonymous to true and disables password reset`() = runTest(testDispatcher) {
            every { isUserAnonymousUseCase() } returns flowOf(true)
            coEvery { getCurrentUserProfileUseCase() } returns null
            coEvery { getLinkedProvidersUseCase() } returns Result.success(emptyList())

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isAnonymous)
            assertEquals("", state.email)
            assertTrue(state.linkedProviders.isEmpty())
            assertFalse(state.canResetPassword)
        }

        @Test
        fun `Google-only user state disables password reset`() = runTest(testDispatcher) {
            coEvery { getLinkedProvidersUseCase() } returns Result.success(listOf(AuthProviderType.GOOGLE))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isGoogleOnly)
            assertFalse(state.canResetPassword)
        }

        @Test
        fun `Email user state enables password reset`() = runTest(testDispatcher) {
            coEvery { getLinkedProvidersUseCase() } returns Result.success(
                listOf(AuthProviderType.GOOGLE, AuthProviderType.EMAIL_PASSWORD)
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isGoogleOnly)
            assertTrue(state.isEmailProviderLinked)
            assertTrue(state.canResetPassword)
        }

        @Test
        fun `LoadAccountSecurity event reloads account security`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(AccountSecurityUiEvent.LoadAccountSecurity)
            advanceUntilIdle()

            coVerify(atLeast = 2) { getCurrentUserProfileUseCase() }
        }

        @Test
        fun `loadAccountSecurity handles exception gracefully`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } throws RuntimeException("DB error")
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
        }

        @Test
        fun `observeBiometricPreference flow updates state with new value`() = runTest(testDispatcher) {
            every { getBiometricLockEnabledUseCase() } returns flowOf(true)
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.biometricLockEnabled)
        }

        @Test
        fun `observeBiometricPreference handles exception gracefully`() = runTest(testDispatcher) {
            every { getBiometricLockEnabledUseCase() } throws RuntimeException("DataStore error")
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.biometricLockEnabled)
        }
    }

    @Nested
    @DisplayName("Password reset confirmation dialog")
    inner class PasswordResetConfirmationDialog {

        @Test
        fun `RequestPasswordResetConfirmation opens confirmation dialog`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(AccountSecurityUiEvent.RequestPasswordResetConfirmation)

            assertTrue(viewModel.uiState.value.showPasswordResetConfirmDialog)
        }

        @Test
        fun `DismissPasswordResetConfirmation closes confirmation dialog`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(AccountSecurityUiEvent.RequestPasswordResetConfirmation)
            assertTrue(viewModel.uiState.value.showPasswordResetConfirmDialog)

            viewModel.onEvent(AccountSecurityUiEvent.DismissPasswordResetConfirmation)
            assertFalse(viewModel.uiState.value.showPasswordResetConfirmDialog)
        }
    }

    @Nested
    @DisplayName("ConfirmSendPasswordReset")
    inner class ConfirmSendPasswordReset {

        @Test
        fun `ConfirmSendPasswordReset does nothing when email is blank`() = runTest(testDispatcher) {
            every { isUserAnonymousUseCase() } returns flowOf(true)
            coEvery { getCurrentUserProfileUseCase() } returns null
            coEvery { getLinkedProvidersUseCase() } returns Result.success(emptyList())

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(AccountSecurityUiEvent.ConfirmSendPasswordReset)
            advanceUntilIdle()

            coVerify(exactly = 0) { sendPasswordResetEmailUseCase(any()) }
        }

        @Test
        fun `ConfirmSendPasswordReset calls SendPasswordResetEmailUseCase and emits ShowTopPill on success`() =
            runTest(testDispatcher) {
                coEvery { sendPasswordResetEmailUseCase("alex@example.com") } returns Result.success(Unit)
                val viewModel = createViewModel()
                advanceUntilIdle()

                val actions = mutableListOf<AccountSecurityUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                viewModel.onEvent(AccountSecurityUiEvent.ConfirmSendPasswordReset)
                advanceUntilIdle()

                coVerify { sendPasswordResetEmailUseCase("alex@example.com") }
                assertFalse(viewModel.uiState.value.isPasswordResetSending)
                assertEquals(1, actions.size)
                val action = assertInstanceOf(AccountSecurityUiAction.ShowTopPill::class.java, actions.first())
                val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
                assertEquals(R.string.account_security_password_reset_sent, message.resId)

                job.cancel()
            }

        @Test
        fun `ConfirmSendPasswordReset emits error ShowTopPill on Result failure`() = runTest(testDispatcher) {
            coEvery { sendPasswordResetEmailUseCase("alex@example.com") } returns Result.failure(
                RuntimeException("Network error")
            )
            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<AccountSecurityUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(AccountSecurityUiEvent.ConfirmSendPasswordReset)
            advanceUntilIdle()

            coVerify { sendPasswordResetEmailUseCase("alex@example.com") }
            assertFalse(viewModel.uiState.value.isPasswordResetSending)
            assertEquals(1, actions.size)
            val action = assertInstanceOf(AccountSecurityUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.account_security_error_prefix, message.resId)

            job.cancel()
        }

        @Test
        fun `ConfirmSendPasswordReset handles thrown exception gracefully`() = runTest(testDispatcher) {
            coEvery { sendPasswordResetEmailUseCase("alex@example.com") } throws RuntimeException("Unexpected error")
            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<AccountSecurityUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(AccountSecurityUiEvent.ConfirmSendPasswordReset)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isPasswordResetSending)
            assertEquals(1, actions.size)
            val action = assertInstanceOf(AccountSecurityUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.account_security_error_prefix, message.resId)

            job.cancel()
        }
    }

    @Nested
    @DisplayName("Biometric lock and navigation")
    inner class BiometricLockAndNavigation {

        @Test
        fun `ToggleBiometricLock to true emits RequestBiometricConfirmation when capability is available`() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                val actions = mutableListOf<AccountSecurityUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                viewModel.onEvent(AccountSecurityUiEvent.ToggleBiometricLock(true))
                advanceUntilIdle()

                assertEquals(1, actions.size)
                assertInstanceOf(AccountSecurityUiAction.RequestBiometricConfirmation::class.java, actions.first())
                coVerify(exactly = 0) { setBiometricLockEnabledUseCase(any()) }
                assertFalse(viewModel.uiState.value.biometricLockEnabled)

                job.cancel()
            }

        @Test
        fun `ToggleBiometricLock to true does not emit RequestBiometricConfirmation when capability is unavailable`() =
            runTest(testDispatcher) {
                every { getBiometricCapabilityUseCase() } returns BiometricCapability.NO_HARDWARE
                val viewModel = createViewModel()
                advanceUntilIdle()

                val actions = mutableListOf<AccountSecurityUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                viewModel.onEvent(AccountSecurityUiEvent.ToggleBiometricLock(true))
                advanceUntilIdle()

                assertTrue(actions.isEmpty())
                coVerify(exactly = 0) { setBiometricLockEnabledUseCase(any()) }

                job.cancel()
            }

        @Test
        fun `BiometricConfirmationSuccess calls SetBiometricLockEnabledUseCase and updates state`() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(AccountSecurityUiEvent.BiometricConfirmationSuccess)
                advanceUntilIdle()

                coVerify { setBiometricLockEnabledUseCase(true) }
                assertTrue(viewModel.uiState.value.biometricLockEnabled)
            }

        @Test
        fun `BiometricConfirmationSuccess handles exception gracefully and emits error ShowTopPill`() =
            runTest(testDispatcher) {
                coEvery { setBiometricLockEnabledUseCase(true) } throws RuntimeException("Set error")
                val viewModel = createViewModel()
                advanceUntilIdle()

                val actions = mutableListOf<AccountSecurityUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                viewModel.onEvent(AccountSecurityUiEvent.BiometricConfirmationSuccess)
                advanceUntilIdle()

                assertFalse(viewModel.uiState.value.biometricLockEnabled)
                assertEquals(1, actions.size)
                val action = assertInstanceOf(AccountSecurityUiAction.ShowTopPill::class.java, actions.first())
                val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
                assertEquals(R.string.account_security_error_prefix, message.resId)

                job.cancel()
            }

        @Test
        fun `ToggleBiometricLock to false calls SetBiometricLockEnabledUseCase directly and updates state`() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(AccountSecurityUiEvent.ToggleBiometricLock(false))
                advanceUntilIdle()

                coVerify { setBiometricLockEnabledUseCase(false) }
                assertFalse(viewModel.uiState.value.biometricLockEnabled)
            }

        @Test
        fun `ToggleBiometricLock to false handles exception gracefully and emits error ShowTopPill`() =
            runTest(testDispatcher) {
                coEvery { setBiometricLockEnabledUseCase(false) } throws RuntimeException("Set error")
                val viewModel = createViewModel()
                advanceUntilIdle()

                val actions = mutableListOf<AccountSecurityUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                viewModel.onEvent(AccountSecurityUiEvent.ToggleBiometricLock(false))
                advanceUntilIdle()

                assertFalse(viewModel.uiState.value.biometricLockEnabled)
                assertEquals(1, actions.size)
                val action = assertInstanceOf(AccountSecurityUiAction.ShowTopPill::class.java, actions.first())
                val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
                assertEquals(R.string.account_security_error_prefix, message.resId)

                job.cancel()
            }

        @Test
        fun `NavigateToAccountStatus emits NavigateToRoute with SETTINGS_ACCOUNT_STATUS`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<AccountSecurityUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(AccountSecurityUiEvent.NavigateToAccountStatus)
            advanceUntilIdle()

            assertEquals(1, actions.size)
            val action = assertInstanceOf(AccountSecurityUiAction.NavigateToRoute::class.java, actions.first())
            assertEquals(Routes.SETTINGS_ACCOUNT_STATUS, action.route)

            job.cancel()
        }
    }
}
