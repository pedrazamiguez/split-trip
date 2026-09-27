package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.exception.EmailCollisionException
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.service.PasswordValidationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignUpWithEmailUseCase
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiAction
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
@DisplayName("RegisterSubmitEventHandlerImpl")
class RegisterSubmitEventHandlerImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var signUpWithEmailUseCase: SignUpWithEmailUseCase
    private lateinit var emailValidationService: EmailValidationService
    private lateinit var passwordValidationService: PasswordValidationService
    private lateinit var handler: RegisterSubmitEventHandlerImpl

    private lateinit var uiState: MutableStateFlow<RegisterUiState>
    private lateinit var actionsChannel: Channel<RegisterUiAction>

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        signUpWithEmailUseCase = mockk()
        emailValidationService = mockk()
        passwordValidationService = mockk()

        handler = RegisterSubmitEventHandlerImpl(
            signUpWithEmailUseCase = signUpWithEmailUseCase,
            emailValidationService = emailValidationService,
            passwordValidationService = passwordValidationService
        )

        uiState = MutableStateFlow(RegisterUiState())
        actionsChannel = Channel(Channel.BUFFERED)
        handler.bind(uiState, actionsChannel, testScope)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Validation Rules")
    inner class ValidationRules {

        @Test
        fun `fails when display name is empty`() = runTest(testDispatcher) {
            uiState.value = RegisterUiState(
                displayName = "   ",
                email = "user@test.com",
                password = "P@ssword1",
                confirmPassword = "P@ssword1"
            )

            handler.handleSubmitSignUp()

            val error = uiState.value.error
            assertNotNull(error)
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.register_error_empty_display_name, (error as UiText.StringResource).resId)
        }

        @Test
        fun `fails when email is invalid`() = runTest(testDispatcher) {
            every { emailValidationService.isValidEmail("invalid-email") } returns false

            uiState.value = RegisterUiState(
                displayName = "Explorer",
                email = "invalid-email",
                password = "P@ssword1",
                confirmPassword = "P@ssword1"
            )

            handler.handleSubmitSignUp()

            val error = uiState.value.error
            assertNotNull(error)
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.register_error_invalid_email, (error as UiText.StringResource).resId)
        }

        @Test
        fun `fails when password complexity is not satisfied`() = runTest(testDispatcher) {
            every { emailValidationService.isValidEmail("user@test.com") } returns true
            every { passwordValidationService.isValidPassword("weak") } returns false

            uiState.value = RegisterUiState(
                displayName = "Explorer",
                email = "user@test.com",
                password = "weak",
                confirmPassword = "weak"
            )

            handler.handleSubmitSignUp()

            val error = uiState.value.error
            assertNotNull(error)
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.register_error_password_complexity, (error as UiText.StringResource).resId)
        }

        @Test
        fun `fails when passwords do not match`() = runTest(testDispatcher) {
            every { emailValidationService.isValidEmail("user@test.com") } returns true
            every { passwordValidationService.isValidPassword("P@ssword1") } returns true

            uiState.value = RegisterUiState(
                displayName = "Explorer",
                email = "user@test.com",
                password = "P@ssword1",
                confirmPassword = "P@ssword2"
            )

            handler.handleSubmitSignUp()

            val error = uiState.value.error
            assertNotNull(error)
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.register_error_passwords_do_not_match, (error as UiText.StringResource).resId)
        }
    }

    @Nested
    @DisplayName("Submission and Exceptions")
    inner class SubmissionAndExceptions {

        @Test
        fun `submits registration successfully when all fields and complexity pass`() = runTest(testDispatcher) {
            every { emailValidationService.isValidEmail("user@test.com") } returns true
            every { passwordValidationService.isValidPassword("P@ssword1") } returns true
            coEvery {
                signUpWithEmailUseCase(
                    email = "user@test.com",
                    displayName = "Explorer",
                    password = "P@ssword1"
                )
            } returns Result.success("user-id-123")

            uiState.value = RegisterUiState(
                displayName = "Explorer",
                email = "user@test.com",
                password = "P@ssword1",
                confirmPassword = "P@ssword1"
            )

            val actions = mutableListOf<RegisterUiAction>()
            val job = launch {
                actionsChannel.receiveAsFlow().collect { actions.add(it) }
            }

            handler.handleSubmitSignUp()
            advanceUntilIdle()

            assertTrue(actions.contains(RegisterUiAction.RegisterSuccess))
            assertFalse(uiState.value.isLoading)
            assertNull(uiState.value.error)
            job.cancel()
        }

        @Test
        fun `handles EmailCollisionException properly`() = runTest(testDispatcher) {
            every { emailValidationService.isValidEmail("user@test.com") } returns true
            every { passwordValidationService.isValidPassword("P@ssword1") } returns true
            coEvery {
                signUpWithEmailUseCase(
                    email = "user@test.com",
                    displayName = "Explorer",
                    password = "P@ssword1"
                )
            } returns Result.failure(EmailCollisionException("Account collision"))

            uiState.value = RegisterUiState(
                displayName = "Explorer",
                email = "user@test.com",
                password = "P@ssword1",
                confirmPassword = "P@ssword1"
            )

            handler.handleSubmitSignUp()
            advanceUntilIdle()

            assertFalse(uiState.value.isLoading)
            assertTrue(uiState.value.showCollisionDialog)
            assertEquals("user@test.com", uiState.value.collisionEmail)
            assertNotNull(uiState.value.error)
            assertTrue(uiState.value.error is UiText.StringResource)
            assertEquals(R.string.register_error_collision, (uiState.value.error as UiText.StringResource).resId)
        }

        @Test
        fun `handles generic exception properly`() = runTest(testDispatcher) {
            every { emailValidationService.isValidEmail("user@test.com") } returns true
            every { passwordValidationService.isValidPassword("P@ssword1") } returns true
            coEvery {
                signUpWithEmailUseCase(
                    email = "user@test.com",
                    displayName = "Explorer",
                    password = "P@ssword1"
                )
            } returns Result.failure(RuntimeException("Network error"))

            uiState.value = RegisterUiState(
                displayName = "Explorer",
                email = "user@test.com",
                password = "P@ssword1",
                confirmPassword = "P@ssword1"
            )

            handler.handleSubmitSignUp()
            advanceUntilIdle()

            assertFalse(uiState.value.isLoading)
            assertFalse(uiState.value.showCollisionDialog)
            assertNotNull(uiState.value.error)
            assertTrue(uiState.value.error is UiText.StringResource)
            assertEquals(R.string.register_error_failed, (uiState.value.error as UiText.StringResource).resId)
        }

        @Test
        fun `handleDismissCollisionDialog dismisses dialog in state`() = runTest(testDispatcher) {
            uiState.value = RegisterUiState(
                showCollisionDialog = true,
                collisionEmail = "user@test.com"
            )

            handler.handleDismissCollisionDialog()

            assertFalse(uiState.value.showCollisionDialog)
        }
    }
}
