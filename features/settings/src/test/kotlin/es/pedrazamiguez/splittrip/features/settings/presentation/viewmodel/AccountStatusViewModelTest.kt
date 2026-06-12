package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountStatusUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.handler.AccountStatusEventHandler
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AccountStatusViewModel")
class AccountStatusViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var handler: AccountStatusEventHandler
    private lateinit var viewModel: AccountStatusViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        handler = mockk(relaxed = true) {
            every { bind(any(), any(), any()) } just runs
            every { loadAccountStatus() } just runs
        }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel() {
        viewModel = AccountStatusViewModel(accountStatusEventHandler = handler)
    }

    @Test
    fun `initialization binds handler and loads account status`() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()

        coVerifyOrder {
            handler.bind(any(), any(), any())
            handler.loadAccountStatus()
        }
    }

    @Test
    fun `onEvent LoadAccountStatus delegates to handler`() = runTest(testDispatcher) {
        createViewModel()
        viewModel.onEvent(AccountStatusUiEvent.LoadAccountStatus)
        advanceUntilIdle()

        coVerify { handler.loadAccountStatus() }
    }

    @Test
    fun `onEvent LinkGoogle delegates to handler`() = runTest(testDispatcher) {
        createViewModel()
        viewModel.onEvent(AccountStatusUiEvent.LinkGoogle("token"))
        advanceUntilIdle()

        coVerify { handler.handleLinkGoogle("token") }
    }

    @Test
    fun `onEvent ShowLinkEmailDialog delegates to handler`() = runTest(testDispatcher) {
        createViewModel()
        viewModel.onEvent(AccountStatusUiEvent.ShowLinkEmailDialog)
        advanceUntilIdle()

        coVerify { handler.handleShowLinkEmailDialog() }
    }

    @Test
    fun `onEvent DismissLinkEmailDialog delegates to handler`() = runTest(testDispatcher) {
        createViewModel()
        viewModel.onEvent(AccountStatusUiEvent.DismissLinkEmailDialog)
        advanceUntilIdle()

        coVerify { handler.handleDismissLinkEmailDialog() }
    }

    @Test
    fun `onEvent LinkPasswordChanged delegates to handler`() = runTest(testDispatcher) {
        createViewModel()
        viewModel.onEvent(AccountStatusUiEvent.LinkPasswordChanged("pass"))
        advanceUntilIdle()

        coVerify { handler.handleLinkPasswordChanged("pass") }
    }

    @Test
    fun `onEvent LinkConfirmPasswordChanged delegates to handler`() = runTest(testDispatcher) {
        createViewModel()
        viewModel.onEvent(AccountStatusUiEvent.LinkConfirmPasswordChanged("confirm"))
        advanceUntilIdle()

        coVerify { handler.handleLinkConfirmPasswordChanged("confirm") }
    }

    @Test
    fun `onEvent SubmitLinkEmailPassword delegates to handler`() = runTest(testDispatcher) {
        createViewModel()
        viewModel.onEvent(AccountStatusUiEvent.SubmitLinkEmailPassword)
        advanceUntilIdle()

        coVerify { handler.handleSubmitLinkEmailPassword() }
    }

    @Test
    fun `onEvent UnlinkProvider delegates to handler`() = runTest(testDispatcher) {
        createViewModel()
        viewModel.onEvent(AccountStatusUiEvent.UnlinkProvider(AuthProviderType.GOOGLE))
        advanceUntilIdle()

        coVerify { handler.handleUnlinkProvider(AuthProviderType.GOOGLE) }
    }

    @Test
    fun `onEvent ShowDeleteAccountDialog delegates to handler`() = runTest(testDispatcher) {
        createViewModel()
        viewModel.onEvent(AccountStatusUiEvent.ShowDeleteAccountDialog)
        advanceUntilIdle()

        coVerify { handler.handleShowDeleteAccountDialog() }
    }

    @Test
    fun `onEvent DismissDeleteAccountDialog delegates to handler`() = runTest(testDispatcher) {
        createViewModel()
        viewModel.onEvent(AccountStatusUiEvent.DismissDeleteAccountDialog)
        advanceUntilIdle()

        coVerify { handler.handleDismissDeleteAccountDialog() }
    }

    @Test
    fun `onEvent ConfirmDeleteAccount delegates to handler`() = runTest(testDispatcher) {
        createViewModel()
        viewModel.onEvent(AccountStatusUiEvent.ConfirmDeleteAccount)
        advanceUntilIdle()

        coVerify { handler.handleConfirmDeleteAccount() }
    }
}
