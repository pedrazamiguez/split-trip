package es.pedrazamiguez.splittrip.features.balance.presentation.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReportShareLauncherTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var parsedUri: Uri
    private lateinit var clipData: ClipData
    private lateinit var chooserIntent: Intent

    @BeforeEach
    fun setUp() {
        mockkStatic(Uri::class, ClipData::class, Intent::class)
        mockkConstructor(Intent::class)

        contentResolver = mockk(relaxed = true)
        context = mockk(relaxed = true) {
            every { contentResolver } returns this@ReportShareLauncherTest.contentResolver
        }
        parsedUri = mockk(relaxed = true)
        clipData = mockk(relaxed = true)
        chooserIntent = mockk(relaxed = true)

        every { Uri.parse(any()) } returns parsedUri
        every { anyConstructed<Intent>().setType(any()) } answers { self as Intent }
        every { anyConstructed<Intent>().putExtra(any<String>(), any<Uri>()) } answers { self as Intent }
        every { anyConstructed<Intent>().setClipData(any()) } just runs
        every { anyConstructed<Intent>().addFlags(any()) } answers { self as Intent }
        every { ClipData.newUri(any(), any(), any()) } returns clipData
        every { Intent.createChooser(any(), any()) } returns chooserIntent
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `launchShareChooser starts chooser activity and returns true`() {
        val launched = ReportShareLauncher.launchShareChooser(
            context = context,
            contentUri = "content://es.pedrazamiguez.splittrip.fileprovider/exports/statement.pdf",
            mimeType = "application/pdf",
            chooserTitle = "Share statement"
        )

        assertTrue(launched)
        verify(exactly = 1) { context.startActivity(chooserIntent) }
    }

    @Test
    fun `launchShareChooser returns false when ActivityNotFoundException is thrown`() {
        every { context.startActivity(chooserIntent) } throws ActivityNotFoundException("No share app")

        val launched = ReportShareLauncher.launchShareChooser(
            context = context,
            contentUri = "content://es.pedrazamiguez.splittrip.fileprovider/exports/statement.pdf",
            mimeType = "application/pdf",
            chooserTitle = "Share statement"
        )

        assertFalse(launched)
    }

    @Test
    fun `launchShareChooser returns false when SecurityException is thrown`() {
        every { context.startActivity(chooserIntent) } throws SecurityException("Denied")

        val launched = ReportShareLauncher.launchShareChooser(
            context = context,
            contentUri = "content://es.pedrazamiguez.splittrip.fileprovider/exports/statement.pdf",
            mimeType = "application/pdf",
            chooserTitle = "Share statement"
        )

        assertFalse(launched)
    }
}
