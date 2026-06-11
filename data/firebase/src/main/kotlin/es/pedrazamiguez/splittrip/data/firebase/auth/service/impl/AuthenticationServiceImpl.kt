package es.pedrazamiguez.splittrip.data.firebase.auth.service.impl

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudUserDataSource
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.exception.EmailCollisionException
import es.pedrazamiguez.splittrip.domain.exception.GoogleCollisionWithEmailPasswordException
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthenticationServiceImpl(
    private val firebaseAuth: FirebaseAuth,
    private val cloudUserDataSource: CloudUserDataSource
) : AuthenticationService {

    companion object {
        private val EMAIL_REGEX = Regex("\"email\"\\s*:\\s*\"([^\"]+)\"")
    }

    override fun currentUserId(): String? = firebaseAuth.currentUser?.uid

    override fun requireUserId(): String = currentUserId() ?: error("User not logged in")

    override val authState: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): Result<String> = runCatching {
        firebaseAuth
            .signInWithEmailAndPassword(
                email,
                password
            )
            .await().user?.uid ?: ""
    }

    override suspend fun signUp(email: String, displayName: String, password: String): Result<String> = runCatching {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: error("Sign-up succeeded but Firebase user is null")

            // Update Firebase Auth profile with display name
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Populate domain User object
            val user = User(
                userId = firebaseUser.uid,
                email = email.trim().lowercase(),
                displayName = displayName,
                profileImagePath = null,
                createdAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)
            )

            // Persist user document to Firestore in a NonCancellable block to ensure it completes even if the coroutine is cancelled
            withContext(NonCancellable) {
                cloudUserDataSource.saveUser(user)
            }

            firebaseUser.uid
        } catch (e: FirebaseAuthUserCollisionException) {
            throw EmailCollisionException(email, e)
        }
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        firebaseAuth.signOut()
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = runCatching {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val firebaseUser = firebaseAuth.signInWithCredential(credential).await().user
                ?: error("Google sign-in succeeded but Firebase user is null")
            val user = User(
                userId = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName,
                profileImagePath = firebaseUser.photoUrl?.toString()
            )

            // Persist user document before returning.
            // This MUST happen here (not in the UseCase) because Firebase Auth's
            // AuthStateListener fires immediately after signInWithCredential completes,
            // which triggers navigation away from Login and cancels the ViewModel's
            // coroutine scope — any work after this call in the UseCase may never execute.
            // NonCancellable ensures the write survives scope cancellation.
            withContext(NonCancellable) {
                cloudUserDataSource.saveUser(user)
            }

            user
        } catch (e: FirebaseAuthUserCollisionException) {
            val email = e.email ?: extractEmailFromIdToken(idToken)
            if (email.isNullOrEmpty()) {
                throw e
            }
            throw GoogleCollisionWithEmailPasswordException(email, idToken, e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    override suspend fun linkGoogleAccount(idToken: String): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: error("No active user")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        user.linkWithCredential(credential).await()
    }

    override suspend fun linkEmailPassword(email: String, password: String): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: error("No active user")
        val credential = EmailAuthProvider.getCredential(email, password)
        user.linkWithCredential(credential).await()
    }

    override suspend fun unlinkProvider(providerType: AuthProviderType): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: error("No active user")
        val providerId = when (providerType) {
            AuthProviderType.EMAIL_PASSWORD -> EmailAuthProvider.PROVIDER_ID
            AuthProviderType.GOOGLE -> GoogleAuthProvider.PROVIDER_ID
        }
        val currentProviders = user.providerData.map { it.providerId }
        val signInProviders = currentProviders.filter {
            it == EmailAuthProvider.PROVIDER_ID || it == GoogleAuthProvider.PROVIDER_ID
        }
        if (signInProviders.size <= 1 && signInProviders.contains(providerId)) {
            error("Cannot unlink the last remaining sign-in provider")
        }
        user.unlink(providerId).await()
    }

    override suspend fun getLinkedProviders(): Result<List<AuthProviderType>> = runCatching {
        val user = firebaseAuth.currentUser ?: return@runCatching emptyList()
        user.providerData.mapNotNull { userInfo ->
            when (userInfo.providerId) {
                EmailAuthProvider.PROVIDER_ID -> AuthProviderType.EMAIL_PASSWORD
                GoogleAuthProvider.PROVIDER_ID -> AuthProviderType.GOOGLE
                else -> null
            }
        }.distinct()
    }

    private fun extractEmailFromIdToken(idToken: String): String? {
        return runCatching {
            val parts = idToken.split(".")
            if (parts.size > 1) {
                val decodedBytes = android.util.Base64.decode(
                    parts[1],
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
                )
                val payload = String(decodedBytes, Charsets.UTF_8)
                EMAIL_REGEX.find(payload)?.groupValues?.get(1)
            } else {
                null
            }
        }.getOrNull()
    }
}
