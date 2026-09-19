package es.pedrazamiguez.splittrip.data.firebase.auth.service.impl

import android.util.Base64
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import es.pedrazamiguez.splittrip.core.common.extensions.toLocalDateTimeUtc
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.core.performance.PerformanceTraces
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudUserDataSource
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.exception.AdminRestrictedOperationException
import es.pedrazamiguez.splittrip.domain.exception.EmailCollisionException
import es.pedrazamiguez.splittrip.domain.exception.GoogleCollisionWithEmailPasswordException
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions")
class AuthenticationServiceImpl(
    private val firebaseAuth: FirebaseAuth,
    private val cloudUserDataSource: CloudUserDataSource,
    private val performanceMonitor: PerformanceMonitor,
    private val localeProvider: LocaleProvider
) : AuthenticationService {

    companion object {
        private val EMAIL_REGEX = Regex("\"email\"\\s*:\\s*\"([^\"]+)\"")
    }

    override fun currentUserId(): String? = firebaseAuth.currentUser?.uid

    override fun getCurrentUserCreationTimestamp(): Long? =
        firebaseAuth.currentUser?.metadata?.creationTimestamp

    override fun requireUserId(): String = currentUserId() ?: error("User not logged in")

    override val authState: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): Result<String> = runCatching {
        performanceMonitor.traceAsync(PerformanceTraces.AUTH_SIGN_IN_EMAIL) {
            val cleanEmail = email.trim().lowercase()
            try {
                firebaseAuth
                    .signInWithEmailAndPassword(
                        cleanEmail,
                        password
                    )
                    .await().user?.uid ?: ""
            } catch (e: Exception) {
                val canonicalEmail = User.canonicalizeEmail(cleanEmail)
                if (cleanEmail != canonicalEmail) {
                    firebaseAuth
                        .signInWithEmailAndPassword(
                            canonicalEmail,
                            password
                        )
                        .await().user?.uid ?: ""
                } else {
                    throw e
                }
            }
        }
    }

    override suspend fun signUp(email: String, displayName: String, password: String): Result<String> = runCatching {
        performanceMonitor.traceAsync(PerformanceTraces.AUTH_SIGN_UP) {
            try {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user ?: error("Sign-up succeeded but Firebase user is null")

                // Update Firebase Auth profile with display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()

                // Populate domain User object
                val user = User(
                    userId = firebaseUser.uid,
                    email = email.trim().lowercase(),
                    displayName = displayName,
                    profileImagePath = null,
                    createdAt = firebaseUser.metadata?.creationTimestamp?.toLocalDateTimeUtc()
                        ?: LocalDateTime.now(ZoneOffset.UTC)
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
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        firebaseAuth.signOut()
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = runCatching {
        performanceMonitor.traceAsync(PerformanceTraces.AUTH_SIGN_IN_GOOGLE) {
            val googleEmail = extractEmailFromIdToken(idToken)?.trim()?.lowercase()
            val canonicalEmail = googleEmail?.let { User.canonicalizeEmail(it) }

            checkPreSignInGoogleCollision(canonicalEmail, googleEmail, idToken)

            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                    ?: error("Google sign-in succeeded but Firebase user is null")

                val isNewUser = authResult.additionalUserInfo?.isNewUser == true
                checkPostSignInGoogleCollision(canonicalEmail, firebaseUser, isNewUser, idToken)

                val user = User(
                    userId = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName,
                    profileImagePath = firebaseUser.photoUrl?.toString(),
                    createdAt = firebaseUser.metadata?.creationTimestamp?.toLocalDateTimeUtc()
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
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        firebaseAuth.setLanguageCode(localeProvider.getCurrentLocale().language)
        val cleanEmail = email.trim().lowercase()
        try {
            firebaseAuth.sendPasswordResetEmail(cleanEmail).await()
        } catch (e: Exception) {
            val canonicalEmail = User.canonicalizeEmail(cleanEmail)
            if (cleanEmail != canonicalEmail) {
                firebaseAuth.sendPasswordResetEmail(canonicalEmail).await()
            } else {
                throw e
            }
        }
    }

    override suspend fun linkGoogleAccount(idToken: String): Result<Unit> = runCatching {
        val user = requireCurrentUser()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        user.linkWithCredential(credential).await()
    }

    override suspend fun linkEmailPassword(email: String, password: String): Result<Unit> = runCatching {
        val user = requireCurrentUser()
        val credential = EmailAuthProvider.getCredential(email.trim().lowercase(), password)
        user.linkWithCredential(credential).await()
    }

    override suspend fun unlinkProvider(providerType: AuthProviderType): Result<Unit> = runCatching {
        val user = requireCurrentUser()
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

    override suspend fun signInAnonymously(): Result<String> = runCatching {
        try {
            firebaseAuth.signInAnonymously().await().user?.uid ?: ""
        } catch (e: FirebaseAuthException) {
            if (e.errorCode == "ERROR_ADMIN_RESTRICTED_OPERATION") {
                throw AdminRestrictedOperationException(e)
            }
            throw e
        }
    }

    override fun currentUserEmail(): String? = firebaseAuth.currentUser?.email

    override fun currentUserDisplayName(): String? = firebaseAuth.currentUser?.displayName

    override fun currentUserPhotoUrl(): String? = firebaseAuth.currentUser?.photoUrl?.toString()

    override fun isAnonymous(): Boolean = firebaseAuth.currentUser?.isAnonymous == true

    private fun requireCurrentUser(): FirebaseUser =
        firebaseAuth.currentUser ?: error("No active user")

    private fun extractEmailFromIdToken(idToken: String): String? {
        return runCatching {
            val parts = idToken.split(".")
            if (parts.size > 1) {
                val decodedBytes = Base64.decode(
                    parts[1],
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
                )
                val payload = String(decodedBytes, Charsets.UTF_8)
                EMAIL_REGEX.find(payload)?.groupValues?.get(1)
            } else {
                null
            }
        }.getOrNull()
    }

    private suspend fun checkPreSignInGoogleCollision(
        canonicalEmail: String?,
        googleEmail: String?,
        idToken: String
    ) {
        if (canonicalEmail == null || canonicalEmail == googleEmail) return
        val signInMethods = runCatching {
            firebaseAuth.fetchSignInMethodsForEmail(canonicalEmail).await().signInMethods
        }.getOrNull()
        if (signInMethods?.contains(EmailAuthProvider.PROVIDER_ID) == true) {
            throw GoogleCollisionWithEmailPasswordException(canonicalEmail, idToken)
        }
    }

    private suspend fun checkPostSignInGoogleCollision(
        canonicalEmail: String?,
        firebaseUser: FirebaseUser,
        isNewUser: Boolean,
        idToken: String
    ) {
        if (canonicalEmail == null) return
        val existingUsers = cloudUserDataSource.searchUsersByEmail(
            canonicalEmail,
            excludeUserId = firebaseUser.uid
        )
        if (existingUsers.isEmpty()) return

        val collisionUser = existingUsers.first()
        if (!isNewUser) {
            runCatching { cloudUserDataSource.deleteUser(firebaseUser.uid) }
        }
        firebaseUser.delete().await()
        throw GoogleCollisionWithEmailPasswordException(collisionUser.email, idToken)
    }
}
