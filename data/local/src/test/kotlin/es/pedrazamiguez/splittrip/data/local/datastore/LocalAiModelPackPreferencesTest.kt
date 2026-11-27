package es.pedrazamiguez.splittrip.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LocalAiModelPackPreferencesTest {

    private lateinit var context: Context
    private lateinit var preferences: LocalAiModelPackPreferences

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        context.dataStore.edit { it.clear() }
        preferences = LocalAiModelPackPreferences(context)
    }

    @After
    fun tearDown() = runTest {
        context.dataStore.edit { it.clear() }
    }

    @Test
    fun `saveInstalledPackMetadata persists and reads packId alongside version, sizeBytes, sha256, and filePath`() =
        runTest {
            val expected = InstalledModelPackMetadata(
                packId = "qwen3-0.6b-litert-v1",
                version = "1.0.0",
                sizeBytes = 343_490_560L,
                sha256Checksum = "bf1aba9ce8d963124407353c6aa4cbf72eba78db21ae133de983eac672ce83e0",
                filePath = "/data/user/0/app/files/ai_models/qwen3-0.6b-litert-v1/1.0.0/model.litertlm"
            )

            preferences.saveInstalledPackMetadata(expected)

            assertEquals(expected, preferences.installedPackMetadata.first())
        }

    @Test
    fun `clearInstalledPackMetadata removes packId and all installed metadata keys`() = runTest {
        preferences.saveInstalledPackMetadata(
            InstalledModelPackMetadata(
                packId = "qwen2.5-1.5b-litert-v1",
                version = "1.0.0",
                sizeBytes = 1_597_922_856L,
                sha256Checksum = "c4a9b7289365ab80fe599b844b77ad253eb3c53fb0e5d963d699146a6f01255e",
                filePath = "/data/user/0/app/files/ai_models/qwen2.5-1.5b-litert-v1/1.0.0/model.litertlm"
            )
        )

        preferences.clearInstalledPackMetadata()

        assertNull(preferences.installedPackMetadata.first())
    }

    @Test
    fun `installedPackMetadata emits null when packId key is missing or blank`() = runTest {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("local_ai_pack_installed_version")] = "1.0.0"
            prefs[longPreferencesKey("local_ai_pack_installed_size_bytes")] = 500L
            prefs[stringPreferencesKey("local_ai_pack_installed_sha256")] = "abc"
            prefs[stringPreferencesKey("local_ai_pack_installed_file_path")] = "/tmp/model.litertlm"
        }
        assertNull(preferences.installedPackMetadata.first())

        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("local_ai_pack_installed_pack_id")] = "   "
        }
        assertNull(preferences.installedPackMetadata.first())
    }

    @Test
    fun `wifiOnlyDownload defaults to true and updates on setWifiOnlyDownload`() = runTest {
        assertTrue(preferences.wifiOnlyDownload.first())

        preferences.setWifiOnlyDownload(false)

        assertFalse(preferences.wifiOnlyDownload.first())
    }
}
