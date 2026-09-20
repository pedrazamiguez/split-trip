package es.pedrazamiguez.splittrip.data.firebase.auth.service.impl

import android.util.Base64
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AdditionalUserInfo
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.SignInMethodQueryResult
import com.google.firebase.auth.UserInfo
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudUserDataSource
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.exception.AdminRestrictedOperationException
import es.pedrazamiguez.splittrip.domain.exception.GoogleCollisionWithEmailPasswordException
import es.pedrazamiguez.splittrip.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.util.Base64 as JavaBase64
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AuthenticationServiceImplTest {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var cloudUserDataSource: CloudUserDataSource
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var localeProvider: LocaleProvider
    private lateinit var service: AuthenticationServiceImpl

    private val testIdToken = "google-id-token"
    private val testUserId = "firebase-uid-123"
    private val testEmail = "user@example.com"
    private val testDisplayName = "Test User"

    private fun createTestIdToken(email: String): String {
        val payload = "{\"email\":\"$email\"}"
        val encoded = JavaBase64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray(Charsets.UTF_8))
        return "header.$encoded.signature"
    }

    @BeforeEach
    fun setUp() {
        firebaseAuth = mockk(relaxed = true)
        cloudUserDataSource = mockk(relaxed = true)
        performanceMonitor = mockk(relaxed = true) {
            every { trace<Any?>(any(), any()) } answers { secondArg<() -> Any?>().invoke() }
            coEvery { traceAsync<Any?>(any(), any()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
        }
        localeProvider = mockk(relaxed = true) {
            every { getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
        }

        mockkStatic(GoogleAuthProvider::class)
        mockkStatic(EmailAuthProvider::class)
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any<Int>()) } answers {
            val input = firstArg<String>()
            JavaBase64.getUrlDecoder().decode(input)
        }

        val defaultSignInResult = mockk<SignInMethodQueryResult> {
            every { signInMethods } returns null
        }
        every { firebaseAuth.fetchSignInMethodsForEmail(any()) } returns Tasks.forResult(defaultSignInResult)

        service = AuthenticationServiceImpl(
            firebaseAuth = firebaseAuth,
            cloudUserDataSource = cloudUserDataSource,
            performanceMonitor = performanceMonitor,
            localeProvider = localeProvider
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(GoogleAuthProvider::class)
        unmockkStatic(EmailAuthProvider::class)
        unmockkStatic(Base64::class)
    }

    @Nested
    inner class SignInWithGoogle {

        private fun mockSuccessfulGoogleSignIn(): FirebaseUser {
            val credential = mockk<AuthCredential>()
            every { GoogleAuthProvider.getCredential(testIdToken, null) } returns credential

            val firebaseUser = mockk<FirebaseUser>()
            every { firebaseUser.uid } returns testUserId
            every { firebaseUser.email } returns testEmail
            every { firebaseUser.displayName } returns testDisplayName
            // android.net.Uri is a final Android framework class that cannot be mocked
            // in JVM unit tests. Using null to test the nullable photoUrl path.
            every { firebaseUser.photoUrl } returns null
            every { firebaseUser.metadata } returns null

            val authResult = mockk<AuthResult>(relaxed = true)
            every { authResult.user } returns firebaseUser
            every { firebaseAuth.signInWithCredential(credential) } returns Tasks.forResult(authResult)

            return firebaseUser
        }

        @Test
        fun `returns User on successful sign-in`() = runTest {
            // Given
            mockSuccessfulGoogleSignIn()
            coEvery { cloudUserDataSource.saveUser(any()) } returns Unit

            // When
            val result = service.signInWithGoogle(testIdToken)

            // Then
            assertTrue(result.isSuccess)
            val user = result.getOrNull()!!
            assertEquals(testUserId, user.userId)
            assertEquals(testEmail, user.email)
            assertEquals(testDisplayName, user.displayName)
            assertNull(user.profileImagePath)
        }

        @Test
        fun `saves user document before returning`() = runTest {
            // Given
            mockSuccessfulGoogleSignIn()
            coEvery { cloudUserDataSource.saveUser(any()) } returns Unit

            // When
            service.signInWithGoogle(testIdToken)

            // Then
            coVerify(exactly = 1) {
                cloudUserDataSource.saveUser(
                    User(
                        userId = testUserId,
                        email = testEmail,
                        displayName = testDisplayName,
                        profileImagePath = null
                    )
                )
            }
        }

        @Test
        fun `fails when user document save fails`() = runTest {
            // Given
            mockSuccessfulGoogleSignIn()
            coEvery { cloudUserDataSource.saveUser(any()) } throws RuntimeException("Firestore write failed")

            // When
            val result = service.signInWithGoogle(testIdToken)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Firestore write failed", result.exceptionOrNull()?.message)
        }

        @Test
        fun `fails when Firebase user is null`() = runTest {
            // Given
            val credential = mockk<AuthCredential>()
            every { GoogleAuthProvider.getCredential(testIdToken, null) } returns credential

            val authResult = mockk<AuthResult>()
            every { authResult.user } returns null
            every { firebaseAuth.signInWithCredential(credential) } returns Tasks.forResult(authResult)

            // When
            val result = service.signInWithGoogle(testIdToken)

            // Then
            assertTrue(result.isFailure)
            assertEquals(
                "Google sign-in succeeded but Firebase user is null",
                result.exceptionOrNull()?.message
            )
            coVerify(exactly = 0) { cloudUserDataSource.saveUser(any()) }
        }

        @Test
        fun `fails when signInWithCredential fails`() = runTest {
            // Given
            val credential = mockk<AuthCredential>()
            every { GoogleAuthProvider.getCredential(testIdToken, null) } returns credential
            every {
                firebaseAuth.signInWithCredential(credential)
            } returns Tasks.forException(RuntimeException("Auth error"))

            // When
            val result = service.signInWithGoogle(testIdToken)

            // Then
            assertTrue(result.isFailure)
            coVerify(exactly = 0) { cloudUserDataSource.saveUser(any()) }
        }

        @Test
        fun `signInWithGoogle throws collision when canonical email has password provider`() = runTest {
            // Given
            val token = createTestIdToken("pedraza.miguez@gmail.com")
            val canonicalEmail = "pedrazamiguez@gmail.com"
            val queryResult = mockk<SignInMethodQueryResult>()
            every { queryResult.signInMethods } returns listOf(EmailAuthProvider.PROVIDER_ID)
            every { firebaseAuth.fetchSignInMethodsForEmail(canonicalEmail) } returns Tasks.forResult(queryResult)

            // When
            val result = service.signInWithGoogle(token)

            // Then
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is GoogleCollisionWithEmailPasswordException)
            assertEquals(canonicalEmail, (exception as GoogleCollisionWithEmailPasswordException).email)
            assertEquals(token, exception.idToken)
            coVerify(exactly = 0) { firebaseAuth.signInWithCredential(any()) }
        }

        @Test
        fun `signInWithGoogle deletes duplicate user when Firestore has canonical account`() = runTest {
            // Given
            val token = createTestIdToken("pedraza.miguez@gmail.com")
            val canonicalEmail = "pedrazamiguez@gmail.com"

            val credential = mockk<AuthCredential>()
            every { GoogleAuthProvider.getCredential(token, null) } returns credential

            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            every { firebaseUser.uid } returns "new-duplicate-uid"
            every { firebaseUser.delete() } returns Tasks.forResult(null)

            val additionalUserInfo = mockk<AdditionalUserInfo>()
            every { additionalUserInfo.isNewUser } returns true

            val authResult = mockk<AuthResult>()
            every { authResult.user } returns firebaseUser
            every { authResult.additionalUserInfo } returns additionalUserInfo
            every { firebaseAuth.signInWithCredential(credential) } returns Tasks.forResult(authResult)

            val existingUser = User(userId = "original-uid", email = canonicalEmail)
            coEvery {
                cloudUserDataSource.searchUsersByEmail(canonicalEmail, excludeUserId = "new-duplicate-uid")
            } returns listOf(existingUser)

            // When
            val result = service.signInWithGoogle(token)

            // Then
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is GoogleCollisionWithEmailPasswordException)
            assertEquals(canonicalEmail, (exception as GoogleCollisionWithEmailPasswordException).email)
            assertEquals(token, exception.idToken)
            coVerify(exactly = 1) { firebaseUser.delete() }
            coVerify(exactly = 0) { cloudUserDataSource.saveUser(any()) }
        }

        @Test
        fun `signInWithGoogle recovers orphaned duplicate user and routes to collision`() = runTest {
            // Given
            val token = createTestIdToken("pedraza.miguez@gmail.com")
            val canonicalEmail = "pedrazamiguez@gmail.com"

            val credential = mockk<AuthCredential>()
            every { GoogleAuthProvider.getCredential(token, null) } returns credential

            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            every { firebaseUser.uid } returns "orphaned-duplicate-uid"
            every { firebaseUser.delete() } returns Tasks.forResult(null)

            val additionalUserInfo = mockk<AdditionalUserInfo>()
            every { additionalUserInfo.isNewUser } returns false

            val authResult = mockk<AuthResult>()
            every { authResult.user } returns firebaseUser
            every { authResult.additionalUserInfo } returns additionalUserInfo
            every { firebaseAuth.signInWithCredential(credential) } returns Tasks.forResult(authResult)

            val existingUser = User(userId = "original-uid", email = canonicalEmail)
            coEvery {
                cloudUserDataSource.searchUsersByEmail(canonicalEmail, excludeUserId = "orphaned-duplicate-uid")
            } returns listOf(existingUser)
            coEvery { cloudUserDataSource.deleteUser("orphaned-duplicate-uid") } returns Unit

            // When
            val result = service.signInWithGoogle(token)

            // Then
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is GoogleCollisionWithEmailPasswordException)
            assertEquals(canonicalEmail, (exception as GoogleCollisionWithEmailPasswordException).email)
            assertEquals(token, exception.idToken)
            coVerify(exactly = 1) { cloudUserDataSource.deleteUser("orphaned-duplicate-uid") }
            coVerify(exactly = 1) { firebaseUser.delete() }
            coVerify(exactly = 0) { cloudUserDataSource.saveUser(any()) }
        }
    }

    @Nested
    inner class SignIn {

        @Test
        fun `signIn succeeds with original email without fallback`() = runTest {
            val email = "user@example.com"
            val password = "password123"
            val authResult = mockk<AuthResult>()
            val firebaseUser = mockk<FirebaseUser>()
            every { firebaseUser.uid } returns testUserId
            every { authResult.user } returns firebaseUser
            every { firebaseAuth.signInWithEmailAndPassword(email, password) } returns Tasks.forResult(authResult)

            val result = service.signIn(email, password)

            assertTrue(result.isSuccess)
            assertEquals(testUserId, result.getOrNull())
            coVerify(exactly = 1) { firebaseAuth.signInWithEmailAndPassword(email, password) }
        }

        @Test
        fun `signIn falls back to canonical email when dotted email fails`() = runTest {
            val dottedEmail = "pedraza.miguez@gmail.com"
            val canonicalEmail = "pedrazamiguez@gmail.com"
            val password = "password123"
            val authResult = mockk<AuthResult>()
            val firebaseUser = mockk<FirebaseUser>()
            every { firebaseUser.uid } returns testUserId
            every { authResult.user } returns firebaseUser

            every {
                firebaseAuth.signInWithEmailAndPassword(dottedEmail, password)
            } returns Tasks.forException(RuntimeException("User not found"))
            every {
                firebaseAuth.signInWithEmailAndPassword(canonicalEmail, password)
            } returns Tasks.forResult(authResult)

            val result = service.signIn(dottedEmail, password)

            assertTrue(result.isSuccess)
            assertEquals(testUserId, result.getOrNull())
            coVerify(exactly = 1) { firebaseAuth.signInWithEmailAndPassword(dottedEmail, password) }
            coVerify(exactly = 1) { firebaseAuth.signInWithEmailAndPassword(canonicalEmail, password) }
        }
    }

    @Nested
    inner class SignUp {

        @BeforeEach
        fun setUpSignUp() {
            mockkStatic(android.text.TextUtils::class)
            every { android.text.TextUtils.isEmpty(any()) } answers {
                val arg = firstArg<CharSequence?>()
                arg == null || arg.isEmpty()
            }
        }

        @AfterEach
        fun tearDownSignUp() {
            unmockkStatic(android.text.TextUtils::class)
        }

        private fun mockSuccessfulSignUp(
            email: String = "newuser@example.com",
            displayName: String = "New User",
            password: String = "password123",
            userId: String = "firebase-uid-999"
        ): FirebaseUser {
            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            every { firebaseUser.uid } returns userId
            every { firebaseUser.email } returns email
            every { firebaseUser.displayName } returns displayName
            every { firebaseUser.updateProfile(any()) } returns Tasks.forResult(null)

            val authResult = mockk<AuthResult>()
            every { authResult.user } returns firebaseUser
            every { firebaseAuth.createUserWithEmailAndPassword(email, password) } returns Tasks.forResult(authResult)

            return firebaseUser
        }

        @Test
        fun `returns userId on successful sign-up`() = runTest {
            // Given
            mockSuccessfulSignUp()
            coEvery { cloudUserDataSource.saveUser(any()) } returns Unit

            // When
            val result = service.signUp("newuser@example.com", "New User", "password123")

            // Then
            assertTrue(result.isSuccess)
            assertEquals("firebase-uid-999", result.getOrNull())
        }

        @Test
        fun `updates profile display name and saves user document to Firestore`() = runTest {
            // Given
            mockSuccessfulSignUp()
            coEvery { cloudUserDataSource.saveUser(any()) } returns Unit

            // When
            service.signUp("newuser@example.com", "New User", "password123")

            // Then
            coVerify(exactly = 1) {
                cloudUserDataSource.saveUser(
                    match { user ->
                        user.userId == "firebase-uid-999" &&
                            user.email == "newuser@example.com" &&
                            user.displayName == "New User" &&
                            user.profileImagePath == null
                    }
                )
            }
        }

        @Test
        fun `fails when Firebase user is null`() = runTest {
            // Given
            val authResult = mockk<AuthResult>()
            every { authResult.user } returns null
            every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)

            // When
            val result = service.signUp("newuser@example.com", "New User", "password123")

            // Then
            assertTrue(result.isFailure)
            coVerify(exactly = 0) { cloudUserDataSource.saveUser(any()) }
        }

        @Test
        fun `fails when Firestore save fails`() = runTest {
            // Given
            mockSuccessfulSignUp()
            coEvery { cloudUserDataSource.saveUser(any()) } throws RuntimeException("Firestore write failed")

            // When
            val result = service.signUp("newuser@example.com", "New User", "password123")

            // Then
            assertTrue(result.isFailure)
            assertEquals("Firestore write failed", result.exceptionOrNull()?.message)
        }
    }

    @Nested
    inner class SendPasswordResetEmail {

        @Test
        fun `sendPasswordResetEmail sets languageCode on firebaseAuth from localeProvider before sending email`() =
            runTest {
                // Given
                val email = "user@example.com"
                every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
                every { firebaseAuth.sendPasswordResetEmail(email) } returns Tasks.forResult(null)

                // When
                val result = service.sendPasswordResetEmail(email)

                // Then
                assertTrue(result.isSuccess)
                coVerify(exactly = 1) { firebaseAuth.setLanguageCode("es") }
                coVerify(exactly = 1) { firebaseAuth.sendPasswordResetEmail(email) }
            }

        @Test
        fun `sendPasswordResetEmail sets English language code when localeProvider returns English`() =
            runTest {
                // Given
                val email = "user@example.com"
                every { localeProvider.getCurrentLocale() } returns Locale.ENGLISH
                every { firebaseAuth.sendPasswordResetEmail(email) } returns Tasks.forResult(null)

                // When
                val result = service.sendPasswordResetEmail(email)

                // Then
                assertTrue(result.isSuccess)
                coVerify(exactly = 1) { firebaseAuth.setLanguageCode("en") }
                coVerify(exactly = 1) { firebaseAuth.sendPasswordResetEmail(email) }
            }

        @Test
        fun `sendPasswordResetEmail success calls firebaseAuth and returns success`() = runTest {
            // Given
            val email = "user@example.com"
            every { firebaseAuth.sendPasswordResetEmail(email) } returns Tasks.forResult(null)

            // When
            val result = service.sendPasswordResetEmail(email)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { firebaseAuth.setLanguageCode("es") }
            coVerify(exactly = 1) { firebaseAuth.sendPasswordResetEmail(email) }
        }

        @Test
        fun `sendPasswordResetEmail failure propagates exception from firebaseAuth`() = runTest {
            // Given
            val email = "user@example.com"
            val exception = RuntimeException("Firebase error")
            every { firebaseAuth.sendPasswordResetEmail(email) } returns Tasks.forException(exception)

            // When
            val result = service.sendPasswordResetEmail(email)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Firebase error", result.exceptionOrNull()?.message)
            coVerify(exactly = 1) { firebaseAuth.setLanguageCode("es") }
            coVerify(exactly = 1) { firebaseAuth.sendPasswordResetEmail(email) }
        }

        @Test
        fun `sendPasswordResetEmail falls back to canonical email when dotted email fails`() = runTest {
            val dottedEmail = "pedraza.miguez@gmail.com"
            val canonicalEmail = "pedrazamiguez@gmail.com"

            every {
                firebaseAuth.sendPasswordResetEmail(dottedEmail)
            } returns Tasks.forException(RuntimeException("User not found"))
            every {
                firebaseAuth.sendPasswordResetEmail(canonicalEmail)
            } returns Tasks.forResult(null)

            val result = service.sendPasswordResetEmail(dottedEmail)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { firebaseAuth.setLanguageCode("es") }
            coVerify(exactly = 1) { firebaseAuth.sendPasswordResetEmail(dottedEmail) }
            coVerify(exactly = 1) { firebaseAuth.sendPasswordResetEmail(canonicalEmail) }
        }
    }

    @Nested
    inner class LinkGoogleAccount {

        @Test
        fun `linkGoogleAccount calls linkWithCredential on firebaseAuth currentUser`() = runTest {
            // Given
            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.linkWithCredential(any()) } returns Tasks.forResult(mockk())

            // When
            val result = service.linkGoogleAccount("google-token")

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { firebaseUser.linkWithCredential(any()) }
        }
    }

    @Nested
    inner class LinkEmailPassword {

        @Test
        fun `linkEmailPassword calls linkWithCredential on firebaseAuth currentUser`() = runTest {
            // Given
            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            val credential = mockk<AuthCredential>()
            every { EmailAuthProvider.getCredential("email@test.com", "password123") } returns credential
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.linkWithCredential(credential) } returns Tasks.forResult(mockk())

            // When
            val result = service.linkEmailPassword("email@test.com", "password123")

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { firebaseUser.linkWithCredential(credential) }
        }

        @Test
        fun `linkEmailPassword preserves dots in credential`() = runTest {
            val dottedEmail = "pedraza.miguez@gmail.com"
            val password = "password123"
            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            val credential = mockk<AuthCredential>()
            every { EmailAuthProvider.getCredential(dottedEmail, password) } returns credential
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.linkWithCredential(credential) } returns Tasks.forResult(mockk())

            val result = service.linkEmailPassword(dottedEmail, password)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { EmailAuthProvider.getCredential(dottedEmail, password) }
            coVerify(exactly = 1) { firebaseUser.linkWithCredential(credential) }
        }
    }

    @Nested
    inner class UnlinkProvider {

        @Test
        fun `unlinkProvider unlinks when multiple providers exist`() = runTest {
            // Given
            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            val providerInfo1 = mockk<UserInfo>()
            val providerInfo2 = mockk<UserInfo>()
            every { providerInfo1.providerId } returns "password"
            every { providerInfo2.providerId } returns "google.com"

            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.providerData } returns listOf(providerInfo1, providerInfo2)
            every { firebaseUser.unlink("google.com") } returns Tasks.forResult(mockk())

            // When
            val result = service.unlinkProvider(AuthProviderType.GOOGLE)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { firebaseUser.unlink("google.com") }
        }

        @Test
        fun `unlinkProvider fails when it is the last remaining provider`() = runTest {
            // Given
            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            val providerInfo = mockk<UserInfo>()
            every { providerInfo.providerId } returns "password"

            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.providerData } returns listOf(providerInfo)

            // When
            val result = service.unlinkProvider(AuthProviderType.EMAIL_PASSWORD)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Cannot unlink the last remaining sign-in provider", result.exceptionOrNull()?.message)
            coVerify(exactly = 0) { firebaseUser.unlink(any()) }
        }
    }

    @Nested
    inner class GetLinkedProviders {

        @Test
        fun `getLinkedProviders returns mapped AuthProviderType list`() = runTest {
            // Given
            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            val providerInfo1 = mockk<UserInfo>()
            val providerInfo2 = mockk<UserInfo>()
            every { providerInfo1.providerId } returns "password"
            every { providerInfo2.providerId } returns "google.com"

            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.providerData } returns listOf(providerInfo1, providerInfo2)

            // When
            val result = service.getLinkedProviders()

            // Then
            assertTrue(result.isSuccess)
            val providers = result.getOrNull()!!
            assertEquals(2, providers.size)
            assertTrue(providers.contains(AuthProviderType.EMAIL_PASSWORD))
            assertTrue(providers.contains(AuthProviderType.GOOGLE))
        }
    }

    @Nested
    inner class SignInAnonymously {

        @Test
        fun `signInAnonymously returns uid on success`() = runTest {
            // Given
            val firebaseUser = mockk<FirebaseUser>()
            every { firebaseUser.uid } returns testUserId
            val authResult = mockk<AuthResult>()
            every { authResult.user } returns firebaseUser
            every { firebaseAuth.signInAnonymously() } returns Tasks.forResult(authResult)

            // When
            val result = service.signInAnonymously()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(testUserId, result.getOrNull())
        }

        @Test
        fun `signInAnonymously throws AdminRestrictedOperationException on restriction`() =
            runTest {
                // Given
                val firebaseException = mockk<FirebaseAuthException>()
                every { firebaseException.errorCode } returns "ERROR_ADMIN_RESTRICTED_OPERATION"
                every { firebaseAuth.signInAnonymously() } returns Tasks.forException(firebaseException)

                // When
                val result = service.signInAnonymously()

                // Then
                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is AdminRestrictedOperationException)
            }

        @Test
        fun `signInAnonymously propagates other FirebaseAuthException errors`() = runTest {
            // Given
            val firebaseException = mockk<FirebaseAuthException>()
            every { firebaseException.errorCode } returns "ERROR_OTHER"
            every { firebaseAuth.signInAnonymously() } returns Tasks.forException(firebaseException)

            // When
            val result = service.signInAnonymously()

            // Then
            assertTrue(result.isFailure)
            assertEquals(firebaseException, result.exceptionOrNull())
        }
    }
}
