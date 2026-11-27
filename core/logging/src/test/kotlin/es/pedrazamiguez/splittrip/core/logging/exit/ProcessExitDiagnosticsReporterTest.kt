package es.pedrazamiguez.splittrip.core.logging.exit

import android.os.Build
import es.pedrazamiguez.splittrip.core.logging.exception.AbnormalProcessExitException
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProcessExitDiagnosticsReporterTest {

    private lateinit var exitInfoProvider: ProcessExitInfoProvider
    private lateinit var timestampStore: LastReportedExitTimestampStore
    private lateinit var recordedDiagnostics: MutableList<RecordedDiagnostic>
    private lateinit var reporter: ProcessExitDiagnosticsReporter

    private data class RecordedDiagnostic(
        val throwable: Throwable,
        val customKeys: Map<String, String>
    )

    @BeforeEach
    fun setUp() {
        exitInfoProvider = mockk()
        timestampStore = mockk()
        recordedDiagnostics = mutableListOf()
        reporter = ProcessExitDiagnosticsReporter(
            exitInfoProvider = exitInfoProvider,
            timestampStore = timestampStore,
            diagnosticRecorder = { throwable, keys ->
                recordedDiagnostics.add(RecordedDiagnostic(throwable, keys))
            }
        )
    }

    @Test
    fun `reportRecentAbnormalExits reports unseen exits in chronological order and persists latest timestamp`() {
        every { timestampStore.getLastReportedTimestampMillis() } returns 1_000L
        every { timestampStore.setLastReportedTimestampMillis(any()) } just Runs
        every { exitInfoProvider.getRecentAbnormalExits(any()) } returns listOf(
            ProcessExitRecord(
                reason = AbnormalExitReason.LOW_MEMORY,
                timestampMillis = 3_000L,
                pid = 300,
                status = 9,
                importance = 100,
                pssKb = 512_000L,
                rssKb = 768_000L,
                processName = "es.pedrazamiguez.splittrip",
                description = "lmk kill"
            ),
            ProcessExitRecord(
                reason = AbnormalExitReason.CRASH_NATIVE,
                timestampMillis = 2_000L,
                pid = 200,
                status = 11,
                importance = 100,
                pssKb = 400_000L,
                rssKb = 600_000L,
                processName = "es.pedrazamiguez.splittrip",
                description = "SIGSEGV"
            ),
            ProcessExitRecord(
                reason = AbnormalExitReason.SIGNALED,
                timestampMillis = 900L,
                pid = 100,
                status = 15,
                importance = 200,
                pssKb = 256_000L,
                rssKb = 384_000L,
                processName = "es.pedrazamiguez.splittrip",
                description = "already reported"
            )
        )

        val reportedCount = reporter.reportRecentAbnormalExits()

        assertEquals(2, reportedCount)
        assertEquals(2, recordedDiagnostics.size)

        val first = recordedDiagnostics[0]
        assertTrue(first.throwable is AbnormalProcessExitException)
        assertEquals("REASON_CRASH_NATIVE", first.customKeys["last_abnormal_exit_reason"])
        assertEquals("400000", first.customKeys["last_abnormal_exit_pss_kb"])
        assertEquals("2000", first.customKeys["last_abnormal_exit_timestamp_ms"])

        val second = recordedDiagnostics[1]
        assertTrue(second.throwable is AbnormalProcessExitException)
        assertEquals("REASON_LOW_MEMORY", second.customKeys["last_abnormal_exit_reason"])
        assertEquals("512000", second.customKeys["last_abnormal_exit_pss_kb"])
        assertEquals("3000", second.customKeys["last_abnormal_exit_timestamp_ms"])

        verify(exactly = 1) { timestampStore.setLastReportedTimestampMillis(3_000L) }
    }

    @Test
    fun `reportRecentAbnormalExits skips exits already reported on previous launch`() {
        every { timestampStore.getLastReportedTimestampMillis() } returns 5_000L
        every { exitInfoProvider.getRecentAbnormalExits(any()) } returns listOf(
            ProcessExitRecord(
                reason = AbnormalExitReason.LOW_MEMORY,
                timestampMillis = 4_000L,
                pid = 100,
                status = 9,
                importance = 100,
                pssKb = 100_000L,
                rssKb = 200_000L,
                processName = "es.pedrazamiguez.splittrip",
                description = "old"
            ),
            ProcessExitRecord(
                reason = AbnormalExitReason.SIGNALED,
                timestampMillis = 5_000L,
                pid = 101,
                status = 9,
                importance = 100,
                pssKb = 100_000L,
                rssKb = 200_000L,
                processName = "es.pedrazamiguez.splittrip",
                description = "exact match"
            )
        )

        val reportedCount = reporter.reportRecentAbnormalExits()

        assertEquals(0, reportedCount)
        assertTrue(recordedDiagnostics.isEmpty())
        verify(exactly = 0) { timestampStore.setLastReportedTimestampMillis(any()) }
    }

    @Test
    fun `reportRecentAbnormalExits sanitizes PII in exit description and truncates overly long descriptions`() {
        every { timestampStore.getLastReportedTimestampMillis() } returns 0L
        every { timestampStore.setLastReportedTimestampMillis(any()) } just Runs
        every { exitInfoProvider.getRecentAbnormalExits(any()) } returns listOf(
            ProcessExitRecord(
                reason = AbnormalExitReason.CRASH_NATIVE,
                timestampMillis = 2_500L,
                pid = 205,
                status = 6,
                importance = 100,
                pssKb = 300_000L,
                rssKb = 450_000L,
                processName = "es.pedrazamiguez.splittrip",
                description = "crash for alice@example.com " + "x".repeat(300)
            )
        )

        val reportedCount = reporter.reportRecentAbnormalExits()

        assertEquals(1, reportedCount)
        val message = recordedDiagnostics.single().throwable.message.orEmpty()
        assertFalse(message.contains("alice@example.com"))
        assertTrue(message.contains("a***e@e***e.com"))
        val descPart = message.substringAfter("desc=")
        assertTrue(descPart.length <= 200)
    }

    @Test
    fun `AndroidProcessExitInfoProvider returns empty list below API 30`() {
        val provider = AndroidProcessExitInfoProvider(
            context = mockk(relaxed = true),
            sdkInt = Build.VERSION_CODES.Q
        )

        assertEquals(emptyList<ProcessExitRecord>(), provider.getRecentAbnormalExits(10))
    }
}
