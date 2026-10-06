package es.pedrazamiguez.splittrip.core.logging.tree

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import es.pedrazamiguez.splittrip.core.logging.LogContext
import es.pedrazamiguez.splittrip.core.logging.exception.SyntheticDiagnosticException
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProductionCrashlyticsTreeTest {

    private val logContext: LogContext = mockk(relaxed = true)
    private val crashlytics: FirebaseCrashlytics = mockk(relaxed = true)
    private lateinit var tree: ProductionCrashlyticsTree

    @BeforeEach
    fun setUp() {
        clearMocks(logContext, crashlytics)
        every { logContext.userId } returns "hashed-user-123"
        every { logContext.deviceId } returns "device-123"
        every { logContext.sessionId } returns "session-123"
        every { logContext.appVersion } returns "1.0.0"

        tree = ProductionCrashlyticsTree(
            logContext = logContext,
            crashlytics = crashlytics
        )
    }

    @Test
    fun `log with INFO priority logs breadcrumb to crashlytics without dropping`() {
        tree.log(Log.INFO, "TestTag", "Test info message", null)

        verify { crashlytics.log("I/TestTag: Test info message") }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `log with VERBOSE or DEBUG priority is ignored`() {
        tree.log(Log.VERBOSE, "TestTag", "Verbose message", null)
        tree.log(Log.DEBUG, "TestTag", "Debug message", null)

        verify(exactly = 0) { crashlytics.log(any()) }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `log with ERROR priority and Throwable records exception to crashlytics`() {
        val throwable = RuntimeException("Crash!")

        tree.log(Log.ERROR, "ErrorTag", "Error occurred", throwable)

        verify { crashlytics.log("E/ErrorTag: Error occurred") }
        verify { crashlytics.recordException(throwable) }
    }

    @Test
    fun `log with ERROR priority and null Throwable records SyntheticDiagnosticException`() {
        val exceptionSlot = slot<Throwable>()
        every { crashlytics.recordException(capture(exceptionSlot)) } returns Unit

        tree.log(Log.ERROR, "ErrorTag", "Something went wrong", null)

        verify { crashlytics.log("E/ErrorTag: Something went wrong") }
        assertTrue(exceptionSlot.isCaptured)
        val captured = exceptionSlot.captured
        assertTrue(captured is SyntheticDiagnosticException)
        assertEquals("[ErrorTag] Something went wrong", captured.message)
    }

    @Test
    fun `log with WARN priority and null Throwable only logs breadcrumb and does not record exception`() {
        tree.log(Log.WARN, "WarnTag", "Warning message", null)

        verify { crashlytics.log("W/WarnTag: Warning message") }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `log sanitizes PII emails in message before logging breadcrumb and synthetic exception`() {
        val exceptionSlot = slot<Throwable>()
        every { crashlytics.recordException(capture(exceptionSlot)) } returns Unit

        tree.log(Log.ERROR, "AuthTag", "Failed login for user john.doe@example.com", null)

        verify { crashlytics.log("E/AuthTag: Failed login for user j***e@e***e.com") }
        assertTrue(exceptionSlot.isCaptured)
        assertEquals("[AuthTag] Failed login for user j***e@e***e.com", exceptionSlot.captured.message)
    }

    @Test
    fun `log updates crashlytics userId on every invocation from logContext`() {
        tree.log(Log.INFO, "Tag", "Message", null)

        verify { crashlytics.setUserId("hashed-user-123") }
    }
}
