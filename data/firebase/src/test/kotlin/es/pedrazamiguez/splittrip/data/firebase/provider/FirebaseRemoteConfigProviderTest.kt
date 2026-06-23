package es.pedrazamiguez.splittrip.data.firebase.provider

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FirebaseRemoteConfigProviderTest {

    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var provider: FirebaseRemoteConfigProvider

    @BeforeEach
    fun setUp() {
        firebaseRemoteConfig = mockk(relaxed = true)
        every { firebaseRemoteConfig.setDefaultsAsync(any<Int>()) } returns mockk(relaxed = true)

        provider = FirebaseRemoteConfigProvider(firebaseRemoteConfig)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `init sets default config XML`() {
        verify(exactly = 1) { firebaseRemoteConfig.setDefaultsAsync(any<Int>()) }
    }

    @Test
    fun `getString delegates to FirebaseRemoteConfig`() {
        every { firebaseRemoteConfig.getString("test_key") } returns "test_value"

        val result = provider.getString("test_key")

        assertEquals("test_value", result)
        verify(exactly = 1) { firebaseRemoteConfig.getString("test_key") }
    }

    @Test
    fun `getLong delegates to FirebaseRemoteConfig`() {
        every { firebaseRemoteConfig.getLong("test_key") } returns 42L

        val result = provider.getLong("test_key")

        assertEquals(42L, result)
        verify(exactly = 1) { firebaseRemoteConfig.getLong("test_key") }
    }

    @Test
    fun `getBoolean delegates to FirebaseRemoteConfig`() {
        every { firebaseRemoteConfig.getBoolean("test_key") } returns true

        val result = provider.getBoolean("test_key")

        assertTrue(result)
        verify(exactly = 1) { firebaseRemoteConfig.getBoolean("test_key") }
    }

    @Test
    fun `fetchAndActivate delegates to FirebaseRemoteConfig and triggers callback`() {
        val mockTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.fetchAndActivate() } returns mockTask

        val listenerSlot = slot<OnCompleteListener<Boolean>>()
        every { mockTask.addOnCompleteListener(capture(listenerSlot)) } returns mockTask
        every { mockTask.isSuccessful } returns true

        var callbackCalled = false
        var callbackSuccess = false

        provider.fetchAndActivate { success ->
            callbackCalled = true
            callbackSuccess = success
        }

        listenerSlot.captured.onComplete(mockTask)

        assertTrue(callbackCalled)
        assertTrue(callbackSuccess)
        verify(exactly = 1) { firebaseRemoteConfig.fetchAndActivate() }
    }

    @Test
    fun `init registers config update listener and activates changes on update`() {
        val mockConfigUpdate = mockk<com.google.firebase.remoteconfig.ConfigUpdate>(relaxed = true)
        val updateListenerSlot = slot<com.google.firebase.remoteconfig.ConfigUpdateListener>()

        verify(exactly = 1) { firebaseRemoteConfig.addOnConfigUpdateListener(capture(updateListenerSlot)) }

        val mockActivateTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.activate() } returns mockActivateTask

        val listenerSlot = slot<OnCompleteListener<Boolean>>()
        every { mockActivateTask.addOnCompleteListener(capture(listenerSlot)) } returns mockActivateTask
        every { mockActivateTask.isSuccessful } returns true

        updateListenerSlot.captured.onUpdate(mockConfigUpdate)

        verify(exactly = 1) { firebaseRemoteConfig.activate() }

        listenerSlot.captured.onComplete(mockActivateTask)
    }

    @Test
    fun `config update listener handles error without crashing`() {
        val updateListenerSlot = slot<com.google.firebase.remoteconfig.ConfigUpdateListener>()
        verify(exactly = 1) { firebaseRemoteConfig.addOnConfigUpdateListener(capture(updateListenerSlot)) }

        val error = mockk<com.google.firebase.remoteconfig.FirebaseRemoteConfigException>(relaxed = true)
        updateListenerSlot.captured.onError(error)
    }
}
