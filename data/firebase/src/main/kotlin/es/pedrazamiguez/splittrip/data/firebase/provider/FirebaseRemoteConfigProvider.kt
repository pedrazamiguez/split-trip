package es.pedrazamiguez.splittrip.data.firebase.provider

import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import es.pedrazamiguez.splittrip.core.common.provider.RemoteConfigProvider
import es.pedrazamiguez.splittrip.data.firebase.R
import timber.log.Timber

class FirebaseRemoteConfigProvider(
    private val firebaseRemoteConfig: FirebaseRemoteConfig
) : RemoteConfigProvider {

    init {
        firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        setupRealTimeUpdateListener()
    }

    private fun setupRealTimeUpdateListener() {
        firebaseRemoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Timber.d("Firebase Remote Config update detected: updatedKeys=${configUpdate.updatedKeys}")
                firebaseRemoteConfig.activate().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.d("Firebase Remote Config auto-activated updated keys successfully.")
                    } else {
                        Timber.w("Firebase Remote Config auto-activation of updated keys failed.")
                    }
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Timber.w(error, "Firebase Remote Config real-time update error occurred.")
            }
        })
    }

    override fun getString(key: String): String = firebaseRemoteConfig.getString(key)

    override fun getLong(key: String): Long = firebaseRemoteConfig.getLong(key)

    override fun getBoolean(key: String): Boolean = firebaseRemoteConfig.getBoolean(key)

    override fun fetchAndActivate(onComplete: (Boolean) -> Unit) {
        firebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }
}
