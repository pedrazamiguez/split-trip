package es.pedrazamiguez.splittrip.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

/**
 * Base class providing user-scoped DataStore infrastructure.
 *
 * Shared by [UserPreferences] and [NotificationUserPreferences] so that both
 * access the same underlying DataStore file with consistent user-scoping logic,
 * without duplicating [userScopedFlow], [userKey], or [currentUserPrefix].
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseUserPreferences(
    internal val context: Context,
    protected val authenticationService: AuthenticationService
) {

    protected companion object {
        const val ANONYMOUS_USER = "anonymous"
    }

    // ── Auth-Reactive Flow ───────────────────────────────────────────────

    /**
     * Emits the current userId whenever the auth state changes.
     * Used via [userScopedFlow] so that all user-scoped Flows automatically
     * recompute when a different user signs in (preventing stale cross-user data).
     */
    private val currentUserId: Flow<String?> = authenticationService.authState.map {
        authenticationService.currentUserId()
    }

    /**
     * Creates a user-scoped Flow that restarts whenever the authenticated user changes.
     * Ensures long-lived collectors (e.g., SharedViewModel's `stateIn`) never retain
     * a previous user's values in memory after an auth change.
     */
    internal fun <T> userScopedFlow(block: (userId: String) -> Flow<T>): Flow<T> =
        currentUserId.flatMapLatest { userId ->
            block(userId ?: ANONYMOUS_USER)
        }

    // ── Key Scoping ──────────────────────────────────────────────────────

    /**
     * Builds a user-scoped key name by prefixing the authenticated user's ID.
     * Falls back to [ANONYMOUS_USER] if called before authentication (safety net).
     */
    internal fun userKey(name: String): String {
        val userId = authenticationService.currentUserId() ?: ANONYMOUS_USER
        return "${userId}_$name"
    }

    /**
     * Returns the current user prefix used for scoping keys.
     * Used by [clearAll] to selectively remove keys.
     */
    protected fun currentUserPrefix(): String {
        val userId = authenticationService.currentUserId() ?: ANONYMOUS_USER
        return "${userId}_"
    }

    /**
     * Selectively clears only the current user's scoped preferences.
     * Device-scoped keys (e.g., onboarding) and other users' keys are preserved.
     */
    suspend fun clearAll() {
        val prefix = currentUserPrefix()
        context.dataStore.edit { prefs ->
            val keysToRemove = prefs.asMap().keys.filter { it.name.startsWith(prefix) }
            keysToRemove.forEach { prefs.remove(it) }
        }
    }
}
