package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.exception.GoogleCollisionWithEmailPasswordException
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithGoogleUseCase
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiEvent
import io.mockk.coEvery
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
@DisplayName("AuthenticationViewModel")
class AuthenticationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var signInWithEmailUseCase: SignInWithEmailUseCase
    private lateinit var signInWithGoogleUseCase: SignInWithGoogleUseCase
    private lateinit var linkGoogleAccountUseCase: LinkGoogleAccountUseCase
    private lateinit var viewModel: AuthenticationViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        signInWithEmailUseCase = mockk()
        signInWithGoogleUseCase = mockk()
        linkGoogleAccountUseCase = mockk()

        viewModel = AuthenticationViewModel(
            signInWithEmailUseCase = signInWithEmailUseCase,
            signInWithGoogleUseCase = signInWithGoogleUseCase,
            linkGoogleAccountUseCase = linkGoogleAccountUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── EmailChanged / PasswordChanged ──────────────────────────────────────

    @Nested
    @DisplayName("Field Updates")
    inner class FieldUpdates {

        @Test
        fun `EmailChanged updates email in state`() = runTest(testDispatcher) {
            viewModel.onEvent(AuthenticationUiEvent.EmailChanged("user@test.com")) {}

            assertEquals("user@test.com", viewModel.uiState.value.email)
        }

        @Test
        fun `PasswordChanged updates password in state`() = runTest(testDispatcher) {
            viewModel.onEvent(AuthenticationUiEvent.PasswordChanged("secret123")) {}

            assertEquals("secret123", viewModel.uiState.value.password)
        }
    }

    // ── SubmitLogin ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("SubmitLogin")
    inner class SubmitLogin {

        @Test
        fun `success calls onLoginSuccess and clears loading`() =
            runTest(testDispatcher) {
                coEvery {
                    signInWithEmailUseCase(any(), any())
                } returns Result.success("user-id-1")

                viewModel.onEvent(AuthenticationUiEvent.EmailChanged("a@b.com")) {}
                viewModel.onEvent(AuthenticationUiEvent.PasswordChanged("pass")) {}

                var successCalled = false
                viewModel.onEvent(AuthenticationUiEvent.SubmitLogin) { successCalled = true }
                advanceUntilIdle()

                assertTrue(successCalled)
                assertFalse(viewModel.uiState.value.isLoading)
                assertNull(viewModel.uiState.value.error)
            }

        @Test
        fun `failure sets error in state`() = runTest(testDispatcher) {
            coEvery {
                signInWithEmailUseCase(any(), any())
            } returns Result.failure(RuntimeException("Invalid credentials"))

            viewModel.onEvent(AuthenticationUiEvent.EmailChanged("a@b.com")) {}
            viewModel.onEvent(AuthenticationUiEvent.PasswordChanged("wrong")) {}

            viewModel.onEvent(AuthenticationUiEvent.SubmitLogin) {}
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNotNull(viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.error is UiText.DynamicString)
        }
    }

    // ── GoogleSignInResult ──────────────────────────────────────────────────

    @Nested
    @DisplayName("GoogleSignInResult")
    inner class GoogleSignInResult {

        @Test
        fun `success calls onLoginSuccess and clears loading`() =
            runTest(testDispatcher) {
                coEvery {
                    signInWithGoogleUseCase(any())
                } returns Result.success("google-user-id")

                var successCalled = false
                viewModel.onEvent(
                    AuthenticationUiEvent.GoogleSignInResult("id-token-123")
                ) { successCalled = true }
                advanceUntilIdle()

                assertTrue(successCalled)
                assertFalse(viewModel.uiState.value.isGoogleLoading)
                assertNull(viewModel.uiState.value.error)
            }

        @Test
        fun `failure sets error in state`() = runTest(testDispatcher) {
            coEvery {
                signInWithGoogleUseCase(any())
            } returns Result.failure(RuntimeException("Token expired"))

            viewModel.onEvent(
                AuthenticationUiEvent.GoogleSignInResult("bad-token")
            ) {}
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isGoogleLoading)
            assertNotNull(viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.error is UiText.DynamicString)
        }
    }

    // ── GoogleSignInFailed ──────────────────────────────────────────────────

    @Nested
    @DisplayName("GoogleSignInFailed")
    inner class GoogleSignInFailed {

        @Test
        fun `sets error as StringResource`() = runTest(testDispatcher) {
            viewModel.onEvent(AuthenticationUiEvent.GoogleSignInFailed) {}

            val error = viewModel.uiState.value.error
            assertNotNull(error)
            assertTrue(error is UiText.StringResource)
            assertEquals(
                R.string.login_google_error,
                (error as UiText.StringResource).resId
            )
            assertFalse(viewModel.uiState.value.isGoogleLoading)
        }
    }

    // ── Collision and Merge Flow ──────────────────────────────────────────────

    @Nested
    @DisplayName("Collision and Merge Flow")
    inner class CollisionAndMergeFlow {

        @Test
        fun `GoogleSignInResult failure with GoogleCollisionWithEmailPasswordException shows collision dialog`() =
            runTest(testDispatcher) {
                coEvery {
                    signInWithGoogleUseCase(any())
                } returns Result.failure(
                    GoogleCollisionWithEmailPasswordException(
                        email = "user@test.com",
                        idToken = "google-id-token"
                    )
                )

                viewModel.onEvent(AuthenticationUiEvent.GoogleSignInResult("google-id-token")) {}
                advanceUntilIdle()

                assertTrue(viewModel.uiState.value.showCollisionDialog)
                assertEquals("user@test.com", viewModel.uiState.value.collisionEmail)
                assertEquals("google-id-token", viewModel.uiState.value.pendingGoogleIdToken)
                assertFalse(viewModel.uiState.value.isGoogleLoading)
                assertEquals("", viewModel.uiState.value.collisionPassword)
                assertNull(viewModel.uiState.value.mergeError)
            }

        @Test
        fun `SubmitCollisionMerge success links Google account and calls success callback`() =
            runTest(testDispatcher) {
                coEvery {
                    signInWithGoogleUseCase(any())
                } returns Result.failure(
                    GoogleCollisionWithEmailPasswordException(
                        email = "user@test.com",
                        idToken = "google-id-token"
                    )
                )
                coEvery {
                    signInWithEmailUseCase("user@test.com", "password123")
                } returns Result.success("user-id-123")
                coEvery {
                    linkGoogleAccountUseCase("google-id-token")
                } returns Result.success(Unit)

                viewModel.onEvent(AuthenticationUiEvent.GoogleSignInResult("google-id-token")) {}
                advanceUntilIdle()

                viewModel.onEvent(AuthenticationUiEvent.CollisionPasswordChanged("password123")) {}
                var successCalled = false
                viewModel.onEvent(AuthenticationUiEvent.SubmitCollisionMerge) {
                    successCalled = true
                }
                advanceUntilIdle()

                assertTrue(successCalled)
                assertFalse(viewModel.uiState.value.isMerging)
                assertFalse(viewModel.uiState.value.showCollisionDialog)
                assertNull(viewModel.uiState.value.pendingGoogleIdToken)
            }

        @Test
        fun `SubmitCollisionMerge empty password sets error`() = runTest(testDispatcher) {
            coEvery {
                signInWithGoogleUseCase(any())
            } returns Result.failure(
                GoogleCollisionWithEmailPasswordException(
                    email = "user@test.com",
                    idToken = "google-id-token"
                )
            )

            viewModel.onEvent(AuthenticationUiEvent.GoogleSignInResult("google-id-token")) {}
            advanceUntilIdle()

            viewModel.onEvent(AuthenticationUiEvent.CollisionPasswordChanged("")) {}
            viewModel.onEvent(AuthenticationUiEvent.SubmitCollisionMerge) {}
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.mergeError)
            assertTrue(viewModel.uiState.value.mergeError is UiText.StringResource)
            assertEquals(
                R.string.login_error_empty_password,
                (viewModel.uiState.value.mergeError as UiText.StringResource).resId
            )
        }

        @Test
        fun `SubmitCollisionMerge incorrect password sets error`() =
            runTest(testDispatcher) {
                coEvery {
                    signInWithGoogleUseCase(any())
                } returns Result.failure(
                    GoogleCollisionWithEmailPasswordException(
                        email = "user@test.com",
                        idToken = "google-id-token"
                    )
                )
                coEvery {
                    signInWithEmailUseCase("user@test.com", "wrong-password")
                } returns Result.failure(RuntimeException("Auth failed"))

                viewModel.onEvent(AuthenticationUiEvent.GoogleSignInResult("google-id-token")) {}
                advanceUntilIdle()

                viewModel.onEvent(AuthenticationUiEvent.CollisionPasswordChanged("wrong-password")) {}
                viewModel.onEvent(AuthenticationUiEvent.SubmitCollisionMerge) {}
                advanceUntilIdle()

                assertFalse(viewModel.uiState.value.isMerging)
                assertNotNull(viewModel.uiState.value.mergeError)
                assertEquals(
                    R.string.login_error_invalid_credentials,
                    (viewModel.uiState.value.mergeError as UiText.StringResource).resId
                )
            }

        @Test
        fun `DismissCollisionDialog clears pending tokens and hides dialog`() = runTest(testDispatcher) {
            coEvery {
                signInWithGoogleUseCase(any())
            } returns Result.failure(
                GoogleCollisionWithEmailPasswordException(
                    email = "user@test.com",
                    idToken = "google-id-token"
                )
            )

            viewModel.onEvent(AuthenticationUiEvent.GoogleSignInResult("google-id-token")) {}
            advanceUntilIdle()

            viewModel.onEvent(AuthenticationUiEvent.DismissCollisionDialog) {}
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showCollisionDialog)
            assertNull(viewModel.uiState.value.pendingGoogleIdToken)
        }
    }
}
