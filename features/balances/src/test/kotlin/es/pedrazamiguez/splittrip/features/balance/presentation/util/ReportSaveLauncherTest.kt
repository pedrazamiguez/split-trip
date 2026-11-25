package es.pedrazamiguez.splittrip.features.balance.presentation.util

import android.content.ActivityNotFoundException
import androidx.activity.result.ActivityResultLauncher
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReportSaveLauncherTest {

    private val request = CreateTripReportDocumentRequest(
        fileName = "splittrip-summary-flat-2026-11-20.pdf",
        mimeType = "application/pdf"
    )

    @Test
    fun `launchCreateDocument launches request and returns true`() {
        val launcher = mockk<ActivityResultLauncher<CreateTripReportDocumentRequest>> {
            every { launch(request) } just runs
        }

        val launched = ReportSaveLauncher.launchCreateDocument(launcher, request)

        assertTrue(launched)
        verify(exactly = 1) { launcher.launch(request) }
    }

    @Test
    fun `launchCreateDocument returns false when ActivityNotFoundException is thrown`() {
        val launcher = mockk<ActivityResultLauncher<CreateTripReportDocumentRequest>> {
            every { launch(request) } throws ActivityNotFoundException("No picker")
        }

        val launched = ReportSaveLauncher.launchCreateDocument(launcher, request)

        assertFalse(launched)
    }

    @Test
    fun `launchCreateDocument returns false when SecurityException is thrown`() {
        val launcher = mockk<ActivityResultLauncher<CreateTripReportDocumentRequest>> {
            every { launch(request) } throws SecurityException("Denied")
        }

        val launched = ReportSaveLauncher.launchCreateDocument(launcher, request)

        assertFalse(launched)
    }
}
