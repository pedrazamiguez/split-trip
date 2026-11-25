package es.pedrazamiguez.splittrip.features.balance.presentation.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CreateTripReportDocumentContractTest {

    private val contract = CreateTripReportDocumentContract()
    private val context: Context = mockk(relaxed = true)

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `createIntent configures ACTION_CREATE_DOCUMENT with CATEGORY_OPENABLE, mimeType, and EXTRA_TITLE`() {
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().addCategory(any()) } answers { self as Intent }
        every { anyConstructed<Intent>().setType(any()) } answers { self as Intent }
        every { anyConstructed<Intent>().putExtra(any<String>(), any<String>()) } answers { self as Intent }

        val request = CreateTripReportDocumentRequest(
            fileName = "splittrip-summary-flat-2026-11-20.pdf",
            mimeType = "application/pdf"
        )

        val intent = contract.createIntent(context, request)

        verify(exactly = 1) { intent.addCategory(Intent.CATEGORY_OPENABLE) }
        verify(exactly = 1) { intent.setType("application/pdf") }
        verify(exactly = 1) { intent.putExtra(Intent.EXTRA_TITLE, "splittrip-summary-flat-2026-11-20.pdf") }
    }

    @Test
    fun `parseResult returns Uri when resultCode is RESULT_OK`() {
        val expectedUri = mockk<Uri>()
        val intent = mockk<Intent> {
            every { data } returns expectedUri
        }

        val result = contract.parseResult(Activity.RESULT_OK, intent)

        assertEquals(expectedUri, result)
    }

    @Test
    fun `parseResult returns null when resultCode is RESULT_CANCELED or intent is null`() {
        val intent = mockk<Intent> {
            every { data } returns mockk()
        }

        assertNull(contract.parseResult(Activity.RESULT_CANCELED, intent))
        assertNull(contract.parseResult(Activity.RESULT_OK, null))
    }
}
