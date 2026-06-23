package es.pedrazamiguez.splittrip.data.firebase.repository

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAppConfigRepositoryTest {

    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var repository: FirebaseAppConfigRepository

    @BeforeEach
    fun setUp() {
        firebaseRemoteConfig = mockk(relaxed = true)
        every { firebaseRemoteConfig.setDefaultsAsync(any<Int>()) } returns mockk(relaxed = true)

        repository = FirebaseAppConfigRepository(firebaseRemoteConfig)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `init sets default config XML`() {
        verify(exactly = 1) { firebaseRemoteConfig.setDefaultsAsync(any<Int>()) }
    }

    @Test
    fun `defaultCurrencyCode, balanceComputationDebounceMs and maxMembersPerGroup expose values from config`() {
        every { firebaseRemoteConfig.getString("default_currency_code") } returns "USD"
        every { firebaseRemoteConfig.getLong("balance_computation_debounce_ms") } returns 500L
        every { firebaseRemoteConfig.getLong("max_members_per_group") } returns 15L

        // Trigger updates
        repository = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertEquals("USD", repository.defaultCurrencyCode.value)
        assertEquals(500L, repository.balanceComputationDebounceMs.value)
        assertEquals(15, repository.maxMembersPerGroup.value)
    }

    @Test
    fun `fetchConfiguration delegates to FirebaseRemoteConfig and updates flows`() = runTest {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        val mockTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.fetchAndActivate() } returns mockTask
        coEvery { mockTask.await() } returns true

        every { firebaseRemoteConfig.getString("default_currency_code") } returns "GBP"
        every { firebaseRemoteConfig.getLong("balance_computation_debounce_ms") } returns 100L
        every { firebaseRemoteConfig.getLong("max_members_per_group") } returns 25L

        val result = repository.fetchConfiguration()

        assertTrue(result)
        assertEquals("GBP", repository.defaultCurrencyCode.value)
        assertEquals(100L, repository.balanceComputationDebounceMs.value)
        assertEquals(25, repository.maxMembersPerGroup.value)
        verify(exactly = 1) { firebaseRemoteConfig.fetchAndActivate() }
    }

    @Test
    fun `fetchConfiguration handles failure safely`() = runTest {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        val mockTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.fetchAndActivate() } returns mockTask
        coEvery { mockTask.await() } throws RuntimeException("Network Error")

        val result = repository.fetchConfiguration()

        assertFalse(result)
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
