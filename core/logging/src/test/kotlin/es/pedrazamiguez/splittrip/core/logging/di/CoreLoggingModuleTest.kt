package es.pedrazamiguez.splittrip.core.logging.di

import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.core.logging.impl.DebugTelemetryTracker
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.koin.dsl.koinApplication

class CoreLoggingModuleTest {

    @Test
    fun `createTelemetryTracker returns standalone DebugTelemetryTracker when isDebug is true`() {
        val firebaseTrackerFactory = mockk<() -> TelemetryTracker>(relaxed = true)

        val result = createTelemetryTracker(
            isDebug = true,
            firebaseTrackerFactory = firebaseTrackerFactory
        )

        assertInstanceOf(DebugTelemetryTracker::class.java, result)
        verify(exactly = 0) { firebaseTrackerFactory.invoke() }

        result.trackScreenView("test_screen", "TestScreen")
        result.trackEvent("test_event", mapOf("key" to "value"))
        result.setUserId("user_123")
        result.setUserProperty("plan", "free")
        result.setCustomKey("env", "debug")
    }

    @Test
    fun `createTelemetryTracker invokes firebaseTrackerFactory and returns its tracker when isDebug is false`() {
        val firebaseTracker = mockk<TelemetryTracker>(relaxed = true)

        val result = createTelemetryTracker(
            isDebug = false,
            firebaseTrackerFactory = { firebaseTracker }
        )

        assertSame(firebaseTracker, result)
    }

    @Test
    fun `coreLoggingModule resolves DebugTelemetryTracker in debug unit test without Android context`() {
        val koinApp = koinApplication {
            modules(coreLoggingModule)
        }

        try {
            val tracker = koinApp.koin.get<TelemetryTracker>()
            assertInstanceOf(DebugTelemetryTracker::class.java, tracker)
        } finally {
            koinApp.close()
        }
    }
}
