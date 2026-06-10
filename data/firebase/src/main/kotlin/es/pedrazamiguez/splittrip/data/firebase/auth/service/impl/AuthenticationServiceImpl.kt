package es.pedrazamiguez.splittrip.data.firebase.auth.service.impl

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudUserDataSource
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
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        firebaseAuth.signOut()
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = runCatching {
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
    }
}
