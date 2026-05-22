package es.pedrazamiguez.splittrip.data.local.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReceiptStorageServiceImplTest {

    private lateinit var context: Context
    private lateinit var service: ReceiptStorageServiceImpl
    private lateinit var mockConnection: HttpURLConnection

    companion object {
        private var factoryRegistered = false

        @Volatile
        private var mockConnectionInstance: HttpURLConnection? = null

        private fun registerFactoryIfNeeded() {
            if (!factoryRegistered) {
                try {
                    URL.setURLStreamHandlerFactory(object : URLStreamHandlerFactory {
                        override fun createURLStreamHandler(protocol: String): URLStreamHandler? {
                            if (protocol == "http" || protocol == "https") {
                                return object : URLStreamHandler() {
                                    override fun openConnection(u: URL?): URLConnection {
                                        return mockConnectionInstance
                                            ?: error("Mock Connection not set")
                                    }
                                }
                            }
                            return null
                        }
                    })
                    factoryRegistered = true
                } catch (ignored: Error) {
                    // Already set, ignore
                }
            }
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        service = ReceiptStorageServiceImpl(context)
        mockConnection = mockk(relaxed = true)
        registerFactoryIfNeeded()
        mockConnectionInstance = mockConnection
    }

    @After
    fun tearDown() {
        mockConnectionInstance = null
        val receiptsDir = File(context.filesDir, "receipts")
        if (receiptsDir.exists()) {
            receiptsDir.deleteRecursively()
        }
    }

    @Test
    fun downloadAndStore_success_downloadsAndSavesPdf() = runTest {
        // Given
        val remoteUrl = "https://example.com/receipt.pdf"
        val pdfContent = "Dummy PDF Content".toByteArray()

        every { mockConnection.responseCode } returns HttpURLConnection.HTTP_OK
        every { mockConnection.contentType } returns "application/pdf"
        every { mockConnection.inputStream } returns ByteArrayInputStream(pdfContent)

        // When
        val attachment = service.downloadAndStore(remoteUrl)

        // Then
        assertNotNull(attachment)
        assertEquals("application/pdf", attachment.mimeType)
        assertEquals(remoteUrl, attachment.remoteUrl)
        assertTrue(attachment.localUri.startsWith("file:/"))
        assertTrue(attachment.localUri.endsWith(".pdf"))

        val localFile = File(java.net.URI(attachment.localUri))
        assertTrue(localFile.exists())
        assertEquals("Dummy PDF Content", localFile.readText())
    }

    @Test
    fun downloadAndStore_httpError_throwsException() = runTest {
        // Given
        val remoteUrl = "https://example.com/receipt.pdf"
        every { mockConnection.responseCode } returns HttpURLConnection.HTTP_NOT_FOUND

        // When/Then
        try {
            service.downloadAndStore(remoteUrl)
            org.junit.Assert.fail("Expected an exception to be thrown")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Failed to download file: HTTP 404") == true)
        }
    }
}
