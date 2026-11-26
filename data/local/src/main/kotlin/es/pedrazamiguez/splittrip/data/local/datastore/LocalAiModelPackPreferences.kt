package es.pedrazamiguez.splittrip.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class InstalledModelPackMetadata(
    val version: String,
    val sizeBytes: Long,
    val sha256Checksum: String,
    val filePath: String
)

class LocalAiModelPackPreferences(private val context: Context) {

    private companion object {
        val KEY_INSTALLED_VERSION = stringPreferencesKey("local_ai_pack_installed_version")
        val KEY_INSTALLED_SIZE_BYTES = longPreferencesKey("local_ai_pack_installed_size_bytes")
        val KEY_INSTALLED_SHA256 = stringPreferencesKey("local_ai_pack_installed_sha256")
        val KEY_INSTALLED_FILE_PATH = stringPreferencesKey("local_ai_pack_installed_file_path")
        val KEY_WIFI_ONLY = booleanPreferencesKey("local_ai_pack_wifi_only")
    }

    val installedPackMetadata: Flow<InstalledModelPackMetadata?> = context.dataStore.data.map { prefs ->
        val version = prefs[KEY_INSTALLED_VERSION]?.takeIf { it.isNotBlank() } ?: return@map null
        val sizeBytes = prefs[KEY_INSTALLED_SIZE_BYTES] ?: return@map null
        val sha256 = prefs[KEY_INSTALLED_SHA256]?.takeIf { it.isNotBlank() } ?: return@map null
        val filePath = prefs[KEY_INSTALLED_FILE_PATH]?.takeIf { it.isNotBlank() } ?: return@map null
        InstalledModelPackMetadata(
            version = version,
            sizeBytes = sizeBytes,
            sha256Checksum = sha256,
            filePath = filePath
        )
    }

    val wifiOnlyDownload: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIFI_ONLY] ?: true
    }

    suspend fun saveInstalledPackMetadata(metadata: InstalledModelPackMetadata) {
        context.dataStore.edit { prefs ->
            prefs[KEY_INSTALLED_VERSION] = metadata.version
            prefs[KEY_INSTALLED_SIZE_BYTES] = metadata.sizeBytes
            prefs[KEY_INSTALLED_SHA256] = metadata.sha256Checksum
            prefs[KEY_INSTALLED_FILE_PATH] = metadata.filePath
        }
    }

    suspend fun clearInstalledPackMetadata() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_INSTALLED_VERSION)
            prefs.remove(KEY_INSTALLED_SIZE_BYTES)
            prefs.remove(KEY_INSTALLED_SHA256)
            prefs.remove(KEY_INSTALLED_FILE_PATH)
        }
    }

    suspend fun setWifiOnlyDownload(wifiOnly: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIFI_ONLY] = wifiOnly
        }
    }
}
