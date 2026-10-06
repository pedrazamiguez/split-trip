package es.pedrazamiguez.splittrip.core.logging.impl

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import es.pedrazamiguez.splittrip.core.logging.sanitizer.hashIdentifier
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FirebaseTelemetryTrackerTest {

    private val context: Context = mockk(relaxed = true)
    private val firebaseAnalytics: FirebaseAnalytics = mockk(relaxed = true)
    private val crashlytics: FirebaseCrashlytics = mockk(relaxed = true)
    private val mockBundle: Bundle = mockk(relaxed = true)
    private lateinit var tracker: FirebaseTelemetryTracker

    @BeforeEach
    fun setUp() {
        clearMocks(context, firebaseAnalytics, crashlytics, mockBundle)
        tracker = FirebaseTelemetryTracker(
            context = context,
            firebaseAnalytics = firebaseAnalytics,
            crashlytics = crashlytics,
            bundleFactory = { mockBundle }
        )
    }

    @Test
    fun `trackScreenView logs SCREEN_VIEW event to FirebaseAnalytics and sets current_screen in Crashlytics`() {
        tracker.trackScreenView("HomeScreen", "HomeActivity")

        verify { firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, any()) }
        verify { crashlytics.setCustomKey("current_screen", "HomeScreen") }
        verify { crashlytics.log("Screen: HomeScreen") }
    }

    @Test
    fun `trackEvent bundles parameters and dispatches to FirebaseAnalytics`() {
        val params = mapOf(
            "currency" to "EUR",
            "has_fee" to true,
            "count" to 5,
            "timestamp" to 123456789L,
            "rate" to 1.25
        )

        tracker.trackEvent("expense_created", params)

        verify { firebaseAnalytics.logEvent("expense_created", any()) }
    }

    @Test
    fun `setUserId hashes userId with SHA-256 and sets in both analytics and crashlytics`() {
        tracker.setUserId("user123")

        val expectedHash = "user123".hashIdentifier()
        verify { firebaseAnalytics.setUserId(expectedHash) }
        verify { crashlytics.setUserId(expectedHash) }
    }

    @Test
    fun `setUserId with null sets null in analytics and anonymous in crashlytics`() {
        tracker.setUserId(null)

        verify { firebaseAnalytics.setUserId(null) }
        verify { crashlytics.setUserId("anonymous") }
    }

    @Test
    fun `setUserProperty sets property in FirebaseAnalytics`() {
        tracker.setUserProperty("theme", "dark")

        verify { firebaseAnalytics.setUserProperty("theme", "dark") }
    }

    @Test
    fun `setCustomKey string sets key in Crashlytics`() {
        tracker.setCustomKey("selected_group_id", "grp-123")

        verify { crashlytics.setCustomKey("selected_group_id", "grp-123") }
    }

    @Test
    fun `setCustomKey boolean sets key in Crashlytics`() {
        tracker.setCustomKey("network_online", true)

        verify { crashlytics.setCustomKey("network_online", true) }
    }
}
