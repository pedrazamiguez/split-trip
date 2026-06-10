package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignUpWithEmailUseCase
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiEvent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("RegisterViewModel")
class RegisterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var signUpWithEmailUseCase: SignUpWithEmailUseCase
    private lateinit var emailValidationService: EmailValidationService
    private lateinit var viewModel: RegisterViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        signUpWithEmailUseCase = mockk()
        emailValidationService = mockk()

        viewModel = RegisterViewModel(
            signUpWithEmailUseCase = signUpWithEmailUseCase,
            emailValidationService = emailValidationService
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Field Updates")
    inner class FieldUpdates {

        @Test
        fun `EmailChanged updates email in state`() = runTest(testDispatcher) {
            viewModel.onEvent(RegisterUiEvent.EmailChanged("explorer@test.com")) {}
            assertEquals("explorer@test.com", viewModel.uiState.value.email)
        }

        @Test
        fun `DisplayNameChanged updates displayName in state`() = runTest(testDispatcher) {
            viewModel.onEvent(RegisterUiEvent.DisplayNameChanged("Explorer")) {}
            assertEquals("Explorer", viewModel.uiState.value.displayName)
        }

        @Test
        fun `PasswordChanged updates password in state`() = runTest(testDispatcher) {
            viewModel.onEvent(RegisterUiEvent.PasswordChanged("secure123")) {}
            assertEquals("secure123", viewModel.uiState.value.password)
        }

        @Test
        fun `ConfirmPasswordChanged updates confirmPassword in state`() = runTest(testDispatcher) {
            viewModel.onEvent(RegisterUiEvent.ConfirmPasswordChanged("secure123")) {}
            assertEquals("secure123", viewModel.uiState.value.confirmPassword)
        }
    }

    @Nested
    @DisplayName("Form Validation")
    inner class FormValidation {

        @Test
        fun `fails when display name is empty`() = runTest(testDispatcher) {
            viewModel.onEvent(RegisterUiEvent.DisplayNameChanged("")) {}
            viewModel.onEvent(RegisterUiEvent.EmailChanged("a@b.com")) {}
            viewModel.onEvent(RegisterUiEvent.PasswordChanged("secret")) {}
            viewModel.onEvent(RegisterUiEvent.ConfirmPasswordChanged("secret")) {}

            viewModel.onEvent(RegisterUiEvent.SubmitSignUp) {}

            val error = viewModel.uiState.value.error
            assertNotNull(error)
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.register_error_empty_display_name, (error as UiText.StringResource).resId)
        }

        @Test
        fun `fails when email is invalid`() = runTest(testDispatcher) {
            every { emailValidationService.isValidEmail("invalid-email") } returns false

            viewModel.onEvent(RegisterUiEvent.DisplayNameChanged("Explorer")) {}
            viewModel.onEvent(RegisterUiEvent.EmailChanged("invalid-email")) {}
            viewModel.onEvent(RegisterUiEvent.PasswordChanged("secret")) {}
            viewModel.onEvent(RegisterUiEvent.ConfirmPasswordChanged("secret")) {}

            viewModel.onEvent(RegisterUiEvent.SubmitSignUp) {}

            val error = viewModel.uiState.value.error
            assertNotNull(error)
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.register_error_invalid_email, (error as UiText.StringResource).resId)
        }

        @Test
        fun `fails when password is too short`() = runTest(testDispatcher) {
            every { emailValidationService.isValidEmail("a@b.com") } returns true

            viewModel.onEvent(RegisterUiEvent.DisplayNameChanged("Explorer")) {}
            viewModel.onEvent(RegisterUiEvent.EmailChanged("a@b.com")) {}
            viewModel.onEvent(RegisterUiEvent.PasswordChanged("12345")) {}
            viewModel.onEvent(RegisterUiEvent.ConfirmPasswordChanged("12345")) {}

            viewModel.onEvent(RegisterUiEvent.SubmitSignUp) {}

            val error = viewModel.uiState.value.error
            assertNotNull(error)
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.register_error_password_too_short, (error as UiText.StringResource).resId)
        }

        @Test
        fun `fails when passwords do not match`() = runTest(testDispatcher) {
            every { emailValidationService.isValidEmail("a@b.com") } returns true

            viewModel.onEvent(RegisterUiEvent.DisplayNameChanged("Explorer")) {}
            viewModel.onEvent(RegisterUiEvent.EmailChanged("a@b.com")) {}
            viewModel.onEvent(RegisterUiEvent.PasswordChanged("secret")) {}
            viewModel.onEvent(RegisterUiEvent.ConfirmPasswordChanged("mismatch")) {}

            viewModel.onEvent(RegisterUiEvent.SubmitSignUp) {}

            val error = viewModel.uiState.value.error
            assertNotNull(error)
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.register_error_passwords_do_not_match, (error as UiText.StringResource).resId)
        }
    }

    @Nested
    @DisplayName("Submit Sign Up")
    inner class SubmitSignUp {

        @Test
        fun `success calls onRegisterSuccess and clears loading`() = runTest(testDispatcher) {
            every { emailValidationService.isValidEmail("a@b.com") } returns true
            coEvery { signUpWithEmailUseCase(any(), any(), any()) } returns Result.success("new-user-id")

            viewModel.onEvent(RegisterUiEvent.DisplayNameChanged("Explorer")) {}
            viewModel.onEvent(RegisterUiEvent.EmailChanged("a@b.com")) {}
            viewModel.onEvent(RegisterUiEvent.PasswordChanged("secret")) {}
            viewModel.onEvent(RegisterUiEvent.ConfirmPasswordChanged("secret")) {}

            var successCalled = false
            viewModel.onEvent(RegisterUiEvent.SubmitSignUp) { successCalled = true }
            advanceUntilIdle()

            assertTrue(successCalled)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

        @Test
        fun `failure sets error in state`() = runTest(testDispatcher) {
            every { emailValidationService.isValidEmail("a@b.com") } returns true
            coEvery { signUpWithEmailUseCase(any(), any(), any()) } returns
                Result.failure(RuntimeException("Registration failed"))

            viewModel.onEvent(RegisterUiEvent.DisplayNameChanged("Explorer")) {}
            viewModel.onEvent(RegisterUiEvent.EmailChanged("a@b.com")) {}
            viewModel.onEvent(RegisterUiEvent.PasswordChanged("secret")) {}
            viewModel.onEvent(RegisterUiEvent.ConfirmPasswordChanged("secret")) {}

            viewModel.onEvent(RegisterUiEvent.SubmitSignUp) {}
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNotNull(viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.error is UiText.DynamicString)
        }
    }
}
