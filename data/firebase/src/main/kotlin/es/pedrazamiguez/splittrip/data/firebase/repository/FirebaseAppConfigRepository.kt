package es.pedrazamiguez.splittrip.data.firebase.repository

import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import es.pedrazamiguez.splittrip.data.firebase.R
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class FirebaseAppConfigRepository(
    private val remoteConfig: FirebaseRemoteConfig
) : AppConfigRepository {

    private val _defaultCurrencyCode = MutableStateFlow(DEFAULT_CURRENCY)
    override val defaultCurrencyCode: StateFlow<String> = _defaultCurrencyCode.asStateFlow()

    private val _balanceComputationDebounceMs = MutableStateFlow(DEFAULT_BALANCE_DEBOUNCE_MS)
    override val balanceComputationDebounceMs: StateFlow<Long> = _balanceComputationDebounceMs.asStateFlow()

    private val _maxMembersPerGroup = MutableStateFlow(DEFAULT_MAX_MEMBERS_PER_GROUP)
    override val maxMembersPerGroup: StateFlow<Int> = _maxMembersPerGroup.asStateFlow()

    init {
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        updateFlowsFromConfig()
        setupRealTimeUpdateListener()
    }

    override suspend fun fetchConfiguration(): Boolean {
        return try {
            val updated = remoteConfig.fetchAndActivate().await()
            if (updated) {
                Timber.d("Firebase Remote Config: Fetch and activate successful.")
                updateFlowsFromConfig()
            }
            updated
        } catch (e: Exception) {
            Timber.e(e, "Firebase Remote Config: Fetch failed.")
            false
        }
    }

    private fun setupRealTimeUpdateListener() {
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Timber.d("Firebase Remote Config real-time update: keys=${configUpdate.updatedKeys}")
                remoteConfig.activate().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        updateFlowsFromConfig()
                    }
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Timber.w(error, "Firebase Remote Config real-time update error.")
            }
        })
    }

    private fun updateFlowsFromConfig() {
        _defaultCurrencyCode.value =
            remoteConfig.getString("default_currency_code").takeIf { it.isNotBlank() } ?: DEFAULT_CURRENCY
        val debounce = remoteConfig.getLong("balance_computation_debounce_ms")
        _balanceComputationDebounceMs.value = if (debounce > 0) debounce else DEFAULT_BALANCE_DEBOUNCE_MS
        val maxMembers = remoteConfig.getLong("max_members_per_group").toInt()
        _maxMembersPerGroup.value = if (maxMembers > 0) maxMembers else DEFAULT_MAX_MEMBERS_PER_GROUP
    }

    companion object {
        private const val DEFAULT_CURRENCY = "EUR"
        private const val DEFAULT_BALANCE_DEBOUNCE_MS = 300L
        private const val DEFAULT_MAX_MEMBERS_PER_GROUP = 20
    }
}
