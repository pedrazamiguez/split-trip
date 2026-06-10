package es.pedrazamiguez.splittrip.domain.service

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
}
