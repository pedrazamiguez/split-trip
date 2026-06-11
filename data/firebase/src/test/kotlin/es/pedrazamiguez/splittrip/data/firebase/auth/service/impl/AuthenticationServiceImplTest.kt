package es.pedrazamiguez.splittrip.data.firebase.auth.service.impl

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudUserDataSource
import es.pedrazamiguez.splittrip.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
    private lateinit var service: AuthenticationServiceImpl

    private val testIdToken = "google-id-token"
    private val testUserId = "firebase-uid-123"
    private val testEmail = "user@example.com"
    private val testDisplayName = "Test User"

    @BeforeEach
    fun setUp() {
        firebaseAuth = mockk(relaxed = true)
        cloudUserDataSource = mockk(relaxed = true)

        mockkStatic(GoogleAuthProvider::class)
        mockkStatic(EmailAuthProvider::class)

        service = AuthenticationServiceImpl(
            firebaseAuth = firebaseAuth,
            cloudUserDataSource = cloudUserDataSource
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(GoogleAuthProvider::class)
        unmockkStatic(EmailAuthProvider::class)
    }

    @Nested
    inner class SignInWithGoogle {

        private fun mockSuccessfulGoogleSignIn(): FirebaseUser {
            val credential = mockk<com.google.firebase.auth.AuthCredential>()
            every { GoogleAuthProvider.getCredential(testIdToken, null) } returns credential

            val firebaseUser = mockk<FirebaseUser>()
            every { firebaseUser.uid } returns testUserId
            every { firebaseUser.email } returns testEmail
            every { firebaseUser.displayName } returns testDisplayName
            // android.net.Uri is a final Android framework class that cannot be mocked
            // in JVM unit tests. Using null to test the nullable photoUrl path.
            every { firebaseUser.photoUrl } returns null
            every { firebaseUser.metadata } returns null

            val authResult = mockk<AuthResult>()
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
            val credential = mockk<com.google.firebase.auth.AuthCredential>()
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
            val credential = mockk<com.google.firebase.auth.AuthCredential>()
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
        fun `sendPasswordResetEmail success calls firebaseAuth and returns success`() = runTest {
            // Given
            val email = "user@example.com"
            every { firebaseAuth.sendPasswordResetEmail(email) } returns Tasks.forResult(null)

            // When
            val result = service.sendPasswordResetEmail(email)

            // Then
            assertTrue(result.isSuccess)
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
            coVerify(exactly = 1) { firebaseAuth.sendPasswordResetEmail(email) }
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
            val credential = mockk<com.google.firebase.auth.AuthCredential>()
            every { EmailAuthProvider.getCredential("email@test.com", "password123") } returns credential
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.linkWithCredential(credential) } returns Tasks.forResult(mockk())

            // When
            val result = service.linkEmailPassword("email@test.com", "password123")

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { firebaseUser.linkWithCredential(credential) }
        }
    }

    @Nested
    inner class UnlinkProvider {

        @Test
        fun `unlinkProvider unlinks when multiple providers exist`() = runTest {
            // Given
            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            val providerInfo1 = mockk<com.google.firebase.auth.UserInfo>()
            val providerInfo2 = mockk<com.google.firebase.auth.UserInfo>()
            every { providerInfo1.providerId } returns "password"
            every { providerInfo2.providerId } returns "google.com"

            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.providerData } returns listOf(providerInfo1, providerInfo2)
            every { firebaseUser.unlink("google.com") } returns Tasks.forResult(mockk())

            // When
            val result = service.unlinkProvider(es.pedrazamiguez.splittrip.domain.enums.AuthProviderType.GOOGLE)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { firebaseUser.unlink("google.com") }
        }

        @Test
        fun `unlinkProvider fails when it is the last remaining provider`() = runTest {
            // Given
            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            val providerInfo = mockk<com.google.firebase.auth.UserInfo>()
            every { providerInfo.providerId } returns "password"

            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.providerData } returns listOf(providerInfo)

            // When
            val result = service.unlinkProvider(es.pedrazamiguez.splittrip.domain.enums.AuthProviderType.EMAIL_PASSWORD)

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
            val providerInfo1 = mockk<com.google.firebase.auth.UserInfo>()
            val providerInfo2 = mockk<com.google.firebase.auth.UserInfo>()
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
            assertTrue(providers.contains(es.pedrazamiguez.splittrip.domain.enums.AuthProviderType.EMAIL_PASSWORD))
            assertTrue(providers.contains(es.pedrazamiguez.splittrip.domain.enums.AuthProviderType.GOOGLE))
        }
    }
}
