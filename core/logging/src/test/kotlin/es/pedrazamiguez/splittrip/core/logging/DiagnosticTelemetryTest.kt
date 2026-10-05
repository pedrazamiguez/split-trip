package es.pedrazamiguez.splittrip.core.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DiagnosticTelemetryTest {

    private lateinit var crashlytics: FirebaseCrashlytics

    @BeforeEach
    fun setUp() {
        mockkStatic(FirebaseCrashlytics::class)
        crashlytics = mockk(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns crashlytics
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `recordDiagnosticException sets custom keys and records exception when Crashlytics is initialized`() {
        val throwable = IllegalStateException("Test error")
        val customKeys = mapOf("auth_provider" to "google", "credential_stage" to "test_stage")

        recordDiagnosticException(throwable, customKeys)

        verify {
            crashlytics.setCustomKey("auth_provider", "google")
            crashlytics.setCustomKey("credential_stage", "test_stage")
            crashlytics.recordException(throwable)
        }
    }

    @Test
    fun `recordDiagnosticException handles exception gracefully when Crashlytics throws`() {
        every { FirebaseCrashlytics.getInstance() } throws IllegalStateException("FirebaseApp is not initialized")
        val throwable = IllegalStateException("Test error")

        assertDoesNotThrow {
            recordDiagnosticException(throwable, mapOf("k1" to "v1"))
        }
    }
}
