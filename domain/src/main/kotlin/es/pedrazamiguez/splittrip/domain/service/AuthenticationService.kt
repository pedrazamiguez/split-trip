package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthenticationService {

    fun currentUserId(): String?

    fun requireUserId(): String

    val authState: Flow<Boolean>

    suspend fun signIn(email: String, password: String): Result<String>

    suspend fun signUp(email: String, displayName: String, password: String): Result<String>

    suspend fun signOut(): Result<Unit>

    suspend fun signInWithGoogle(idToken: String): Result<User>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun linkGoogleAccount(idToken: String): Result<Unit>

    suspend fun linkEmailPassword(email: String, password: String): Result<Unit>

    suspend fun unlinkProvider(providerType: AuthProviderType): Result<Unit>

    suspend fun getLinkedProviders(): Result<List<AuthProviderType>>
}
