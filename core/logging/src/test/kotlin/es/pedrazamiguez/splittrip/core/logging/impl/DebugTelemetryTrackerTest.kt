package es.pedrazamiguez.splittrip.core.logging.impl

import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DebugTelemetryTrackerTest {

    private val delegate: TelemetryTracker = mockk(relaxed = true)
    private lateinit var tracker: DebugTelemetryTracker

    @BeforeEach
    fun setUp() {
        clearMocks(delegate)
        tracker = DebugTelemetryTracker(firebaseTracker = delegate)
    }

    @Test
    fun `trackScreenView delegates to firebaseTracker`() {
        tracker.trackScreenView("HomeScreen", "HomeActivity")

        verify { delegate.trackScreenView("HomeScreen", "HomeActivity") }
    }

    @Test
    fun `trackEvent delegates to firebaseTracker`() {
        val params = mapOf("key" to "value")
        tracker.trackEvent("test_event", params)

        verify { delegate.trackEvent("test_event", params) }
    }

    @Test
    fun `setUserId delegates to firebaseTracker`() {
        tracker.setUserId("user-123")

        verify { delegate.setUserId("user-123") }
    }

    @Test
    fun `setUserProperty delegates to firebaseTracker`() {
        tracker.setUserProperty("prop", "val")

        verify { delegate.setUserProperty("prop", "val") }
    }

    @Test
    fun `setCustomKey string delegates to firebaseTracker`() {
        tracker.setCustomKey("screen", "home")

        verify { delegate.setCustomKey("screen", "home") }
    }

    @Test
    fun `setCustomKey boolean delegates to firebaseTracker`() {
        tracker.setCustomKey("online", true)

        verify { delegate.setCustomKey("online", true) }
    }

    @Test
    fun `works gracefully when firebaseTracker is null`() {
        val standalone = DebugTelemetryTracker(firebaseTracker = null)

        standalone.trackScreenView("Screen", null)
        standalone.trackEvent("Event", emptyMap())
        standalone.setUserId("User")
        standalone.setUserProperty("Key", "Value")
        standalone.setCustomKey("k", "v")
        standalone.setCustomKey("k", false)
        // Passes without exception
    }
}
