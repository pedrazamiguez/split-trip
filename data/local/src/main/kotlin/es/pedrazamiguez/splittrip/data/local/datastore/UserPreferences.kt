package es.pedrazamiguez.splittrip.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val MAX_RECENT_ITEMS = 3

@Suppress("TooManyFunctions")
class UserPreferences(
    context: Context,
    authenticationService: AuthenticationService
) : BaseUserPreferences(context, authenticationService) {

    private companion object {

        // Device-scoped keys (NOT user-scoped) — these are device concerns
        private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
        private val PENDING_FCM_TOKEN_KEY = stringPreferencesKey("pending_fcm_token")
        private val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")
        private val SHOULD_SHOW_LANGUAGE_PILL_KEY = booleanPreferencesKey("should_show_language_pill")

        // User-scoped key name constants (prefixed at access time via userKey())
        private const val SELECTED_GROUP_ID = "selected_group_id"
        private const val SELECTED_GROUP_NAME = "selected_group_name"
        private const val SELECTED_GROUP_CURRENCY = "selected_group_currency"
        private const val DEFAULT_CURRENCY = "default_currency"
        private const val ACTIVE_AI_ENGINE = "active_ai_engine"
    }

    // ── App Language (Device-scoped) ─────────────────────────────────────

    val appLanguage: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[APP_LANGUAGE_KEY]
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_LANGUAGE_KEY] = languageCode
        }
    }

    // ── Language Changed Pill (Device-scoped) ────────────────────────────

    val shouldShowLanguagePill: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOULD_SHOW_LANGUAGE_PILL_KEY] ?: false
    }

    suspend fun setShouldShowLanguagePill(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SHOULD_SHOW_LANGUAGE_PILL_KEY] = show
        }
    }

    // ── Onboarding (Device-scoped) ───────────────────────────────────────

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETE_KEY] ?: false
    }

    suspend fun setOnboardingComplete() {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE_KEY] = true
        }
    }

    // ── Pending FCM Token (Device-scoped) ────────────────────────────────

    val pendingFcmToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PENDING_FCM_TOKEN_KEY]
    }

    suspend fun setPendingFcmToken(token: String?) {
        context.dataStore.edit { prefs ->
            if (token != null) {
                prefs[PENDING_FCM_TOKEN_KEY] = token
            } else {
                prefs.remove(PENDING_FCM_TOKEN_KEY)
            }
        }
    }

    // ── Selected Group (User-scoped, auth-reactive) ──────────────────────

    val selectedGroupId: Flow<String?> = userScopedFlow { userId ->
        context.dataStore.data.map { prefs ->
            prefs[stringPreferencesKey("${userId}_$SELECTED_GROUP_ID")]
        }
    }

    val selectedGroupName: Flow<String?> = userScopedFlow { userId ->
        context.dataStore.data.map { prefs ->
            prefs[stringPreferencesKey("${userId}_$SELECTED_GROUP_NAME")]
        }
    }

    val selectedGroupCurrency: Flow<String?> = userScopedFlow { userId ->
        context.dataStore.data.map { prefs ->
            prefs[stringPreferencesKey("${userId}_$SELECTED_GROUP_CURRENCY")]
        }
    }

    suspend fun setSelectedGroup(groupId: String?, groupName: String?, currency: String?) {
        context.dataStore.edit { prefs ->
            val idKey = stringPreferencesKey(userKey(SELECTED_GROUP_ID))
            val nameKey = stringPreferencesKey(userKey(SELECTED_GROUP_NAME))
            val currencyKey = stringPreferencesKey(userKey(SELECTED_GROUP_CURRENCY))
            if (groupId != null && groupName != null) {
                prefs[idKey] = groupId
                prefs[nameKey] = groupName
                if (currency != null) {
                    prefs[currencyKey] = currency
                } else {
                    prefs.remove(currencyKey)
                }
            } else {
                prefs.remove(idKey)
                prefs.remove(nameKey)
                prefs.remove(currencyKey)
            }
        }
    }

    // ── Default Currency (User-scoped, auth-reactive) ────────────────────

    val defaultCurrency: Flow<String> = userScopedFlow { userId ->
        context.dataStore.data.map { prefs ->
            prefs[stringPreferencesKey("${userId}_$DEFAULT_CURRENCY")]
                ?: AppConstants.DEFAULT_CURRENCY_CODE
        }
    }

    suspend fun setDefaultCurrency(currencyCode: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(userKey(DEFAULT_CURRENCY))] = currencyCode
        }
    }

    // ── Active AI Engine (User-scoped, auth-reactive) ─────────────────────

    val activeAiEngine: Flow<es.pedrazamiguez.splittrip.domain.enums.AiEngineType> = userScopedFlow { userId ->
        context.dataStore.data.map { prefs ->
            val name = prefs[stringPreferencesKey("${userId}_$ACTIVE_AI_ENGINE")]
            if (name != null) {
                try {
                    es.pedrazamiguez.splittrip.domain.enums.AiEngineType.valueOf(name)
                } catch (_: Exception) {
                    es.pedrazamiguez.splittrip.domain.enums.AiEngineType.AI_CORE_GEMMA_4
                }
            } else {
                es.pedrazamiguez.splittrip.domain.enums.AiEngineType.AI_CORE_GEMMA_4
            }
        }
    }

    suspend fun setActiveAiEngine(engineType: es.pedrazamiguez.splittrip.domain.enums.AiEngineType) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(userKey(ACTIVE_AI_ENGINE))] = engineType.name
        }
    }

    // ── Per-Group Last-Used Currency (User + Group scoped) ───────────────

    fun getGroupLastUsedCurrency(groupId: String): Flow<String?> = userScopedFlow { userId ->
        val key = stringPreferencesKey("${userId}_last_used_currency_$groupId")
        context.dataStore.data.map { prefs -> prefs[key] }
    }

    suspend fun setGroupLastUsedCurrency(groupId: String, currencyCode: String) {
        val key = stringPreferencesKey(userKey("last_used_currency_$groupId"))
        context.dataStore.edit { prefs ->
            prefs[key] = currencyCode
        }
    }

    fun getGroupLastUsedPaymentMethod(groupId: String): Flow<List<String>> =
        getRecentIds("last_used_payment_method", groupId)

    suspend fun setGroupLastUsedPaymentMethod(groupId: String, paymentMethodId: String) {
        addRecentId("last_used_payment_method", groupId, paymentMethodId)
    }

    fun getGroupLastUsedCategory(groupId: String): Flow<List<String>> = getRecentIds("last_used_category", groupId)

    suspend fun setGroupLastUsedCategory(groupId: String, categoryId: String) {
        addRecentId("last_used_category", groupId, categoryId)
    }

    // ── Last Seen Balance (User + Group scoped) ──────────────────────────

    fun getLastSeenBalance(groupId: String): Flow<String?> = userScopedFlow { userId ->
        val key = stringPreferencesKey("${userId}_last_seen_balance_$groupId")
        context.dataStore.data.map { prefs -> prefs[key] }
    }

    suspend fun setLastSeenBalance(groupId: String, formattedBalance: String) {
        val key = stringPreferencesKey(userKey("last_seen_balance_$groupId"))
        context.dataStore.edit { prefs ->
            prefs[key] = formattedBalance
        }
    }

    // ── Active AI Model (User-scoped, auth-reactive) ─────────────────────

    val activeAiModel: Flow<String?> = userScopedFlow { userId ->
        context.dataStore.data.map { prefs ->
            prefs[stringPreferencesKey("${userId}_$ACTIVE_AI_ENGINE")]
        }
    }

    suspend fun setActiveAiModel(model: String?) {
        context.dataStore.edit { prefs ->
            val key = stringPreferencesKey(userKey(ACTIVE_AI_ENGINE))
            if (model != null) {
                prefs[key] = model
            } else {
                prefs.remove(key)
            }
        }
    }
}

// ── MRU List Helpers (User + Group scoped) ───────────────────────────

private fun UserPreferences.getRecentIds(
    keyPrefix: String,
    groupId: String
): Flow<List<String>> = userScopedFlow { userId ->
    val key = stringPreferencesKey("${userId}_${keyPrefix}_$groupId")
    context.dataStore.data.map { prefs ->
        prefs[key]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }
}

private suspend fun UserPreferences.addRecentId(
    keyPrefix: String,
    groupId: String,
    id: String
) {
    val key = stringPreferencesKey(userKey("${keyPrefix}_$groupId"))
    context.dataStore.edit { prefs ->
        val current = prefs[key]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val updated = (listOf(id) + current.filter { it != id })
            .take(MAX_RECENT_ITEMS)
        prefs[key] = updated.joinToString(",")
    }
}
