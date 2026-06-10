package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.usecase.auth.SendPasswordResetEmailUseCase
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.ForgotPasswordUiAction
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.ForgotPasswordUiEvent
import io.mockk.coEvery
import io.mockk.mockk
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("ForgotPasswordViewModel")
class ForgotPasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var sendPasswordResetEmailUseCase: SendPasswordResetEmailUseCase
    private lateinit var viewModel: ForgotPasswordViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sendPasswordResetEmailUseCase = mockk()
        viewModel = ForgotPasswordViewModel(sendPasswordResetEmailUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `EmailChanged updates email in state`() = runTest(testDispatcher) {
        viewModel.onEvent(ForgotPasswordUiEvent.EmailChanged("test@example.com"))

        assertEquals("test@example.com", viewModel.uiState.value.email)
        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.generalError)
    }

    @Test
    fun `Submit with empty email sets emailError`() = runTest(testDispatcher) {
        viewModel.onEvent(ForgotPasswordUiEvent.EmailChanged("   "))
        viewModel.onEvent(ForgotPasswordUiEvent.Submit)

        val error = viewModel.uiState.value.emailError
        assertNotNull(error)
        assertTrue(error is UiText.StringResource)
        assertEquals(R.string.register_error_invalid_email, (error as UiText.StringResource).resId)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `Submit with valid email triggers UseCase and sets isSuccess`() = runTest(testDispatcher) {
        coEvery { sendPasswordResetEmailUseCase("user@example.com") } returns Result.success(Unit)

        viewModel.onEvent(ForgotPasswordUiEvent.EmailChanged("user@example.com"))
        viewModel.onEvent(ForgotPasswordUiEvent.Submit)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isSuccess)
        assertNull(viewModel.uiState.value.generalError)
    }

    @Test
    fun `Submit with UseCase failure sets generalError`() = runTest(testDispatcher) {
        val errorMsg = "Reset failed"
        coEvery { sendPasswordResetEmailUseCase("user@example.com") } returns Result.failure(RuntimeException(errorMsg))

        viewModel.onEvent(ForgotPasswordUiEvent.EmailChanged("user@example.com"))
        viewModel.onEvent(ForgotPasswordUiEvent.Submit)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isSuccess)
        val error = viewModel.uiState.value.generalError
        assertNotNull(error)
        assertTrue(error is UiText.DynamicString)
        assertEquals(errorMsg, (error as UiText.DynamicString).value)
    }

    @Test
    fun `BackClicked emits NavigateBack action`() = runTest(testDispatcher) {
        val actions = mutableListOf<ForgotPasswordUiAction>()
        val job = launch {
            viewModel.uiAction.collect { actions.add(it) }
        }

        viewModel.onEvent(ForgotPasswordUiEvent.BackClicked)
        advanceUntilIdle()

        assertEquals(1, actions.size)
        assertEquals(ForgotPasswordUiAction.NavigateBack, actions[0])
        job.cancel()
    }
}
