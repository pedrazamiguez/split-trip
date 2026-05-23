package es.pedrazamiguez.splittrip.data.local.service

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import es.pedrazamiguez.splittrip.domain.exception.TerminalDownloadException
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class ReceiptStorageServiceImplTest {

    private lateinit var context: Context
    private lateinit var service: ReceiptStorageServiceImpl
    private lateinit var mockConnection: HttpURLConnection
    private lateinit var mockConnectionFactory: HttpConnectionFactory
    private val originalSdkInt = Build.VERSION.SDK_INT

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockConnection = mockk(relaxed = true)
        mockConnectionFactory = HttpConnectionFactory { mockConnection }
        service = ReceiptStorageServiceImpl(context, mockConnectionFactory)
    }

    @After
    fun tearDown() {
        val receiptsDir = File(context.filesDir, "receipts")
        if (receiptsDir.exists()) {
            receiptsDir.deleteRecursively()
        }
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", originalSdkInt)
        clearAllMocks()
        unmockkAll()
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
        } catch (e: TerminalDownloadException) {
            assertEquals(404, e.responseCode)
            assertTrue(e.message?.contains("Failed to download file: HTTP 404") == true)
        }
    }

    @Test
    fun copyAndCompress_pdf_copiesVerbatim() = runTest {
        val mockContext = mockk<Context>()
        val mockResolver = mockk<ContentResolver>()
        val tempFilesDir = File(context.filesDir, "filesDir_temp").also { it.mkdirs() }
        val tempCacheDir = File(context.cacheDir, "cacheDir_temp").also { it.mkdirs() }
        every { mockContext.filesDir } returns tempFilesDir
        every { mockContext.cacheDir } returns tempCacheDir
        every { mockContext.contentResolver } returns mockResolver
        every { mockContext.packageName } returns "es.pedrazamiguez.splittrip"

        val localService = ReceiptStorageServiceImpl(mockContext, mockConnectionFactory)

        val sourceUri = "content://com.android.providers.media.documents/document/document%3A1000000034"
        val mockUri = mockk<Uri>(relaxed = true)
        mockkStatic(Uri::class)
        every { Uri.parse(sourceUri) } returns mockUri
        every { mockResolver.getType(mockUri) } returns "application/pdf"

        val pdfData = "Mock PDF Data".toByteArray()
        every { mockResolver.openInputStream(mockUri) } returns ByteArrayInputStream(pdfData)

        // When
        val attachment = localService.copyAndCompress(sourceUri)

        // Then
        assertNotNull(attachment)
        assertEquals("application/pdf", attachment.mimeType)
        assertTrue(attachment.localUri.endsWith(".pdf"))

        val copiedFile = File(java.net.URI(attachment.localUri))
        assertTrue(copiedFile.exists())
        assertEquals("Mock PDF Data", copiedFile.readText())

        // Clean up
        tempFilesDir.deleteRecursively()
        tempCacheDir.deleteRecursively()
    }

    @Test
    fun copyAndCompress_imageJpeg_compressesToWebp_AndroidR() = runTest {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.R)

        val mockContext = mockk<Context>()
        val mockResolver = mockk<ContentResolver>()
        val tempFilesDir = File(context.filesDir, "filesDir_image_R").also { it.mkdirs() }
        val tempCacheDir = File(context.cacheDir, "cacheDir_image_R").also { it.mkdirs() }
        every { mockContext.filesDir } returns tempFilesDir
        every { mockContext.cacheDir } returns tempCacheDir
        every { mockContext.contentResolver } returns mockResolver
        every { mockContext.packageName } returns "es.pedrazamiguez.splittrip"

        val localService = ReceiptStorageServiceImpl(mockContext, mockConnectionFactory)

        val sourceUri = "content://com.android.providers.media.documents/document/image%3A1"
        val mockUri = mockk<Uri>(relaxed = true)
        mockkStatic(Uri::class)
        every { Uri.parse(sourceUri) } returns mockUri
        every { mockResolver.getType(mockUri) } returns "image/jpeg"

        val imageData = "Fake Image Data".toByteArray()
        every { mockResolver.openInputStream(mockUri) } returns ByteArrayInputStream(imageData)

        mockkStatic(BitmapFactory::class)
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { BitmapFactory.decodeFile(any(), any()) } returns mockBitmap
        every { mockBitmap.compress(any(), any(), any()) } answers {
            val os = arg<java.io.OutputStream>(2)
            os.write("Compressed Lossless Webp Bytes".toByteArray())
            true
        }

        // When
        val attachment = localService.copyAndCompress(sourceUri)

        // Then
        assertNotNull(attachment)
        assertEquals("image/webp", attachment.mimeType)
        assertTrue(attachment.localUri.endsWith(".webp"))

        val compressedFile = File(java.net.URI(attachment.localUri))
        assertTrue(compressedFile.exists())
        assertEquals("Compressed Lossless Webp Bytes", compressedFile.readText())

        // Clean up
        tempFilesDir.deleteRecursively()
        tempCacheDir.deleteRecursively()
    }

    @Test
    fun copyAndCompress_imageJpeg_compressesToWebp_AndroidM() = runTest {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.M)

        val mockContext = mockk<Context>()
        val mockResolver = mockk<ContentResolver>()
        val tempFilesDir = File(context.filesDir, "filesDir_image_M").also { it.mkdirs() }
        val tempCacheDir = File(context.cacheDir, "cacheDir_image_M").also { it.mkdirs() }
        every { mockContext.filesDir } returns tempFilesDir
        every { mockContext.cacheDir } returns tempCacheDir
        every { mockContext.contentResolver } returns mockResolver
        every { mockContext.packageName } returns "es.pedrazamiguez.splittrip"

        val localService = ReceiptStorageServiceImpl(mockContext, mockConnectionFactory)

        val sourceUri = "content://com.android.providers.media.documents/document/image%3A1"
        val mockUri = mockk<Uri>(relaxed = true)
        mockkStatic(Uri::class)
        every { Uri.parse(sourceUri) } returns mockUri
        every { mockResolver.getType(mockUri) } returns "image/jpeg"

        val imageData = "Fake Image Data".toByteArray()
        every { mockResolver.openInputStream(mockUri) } returns ByteArrayInputStream(imageData)

        mockkStatic(BitmapFactory::class)
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { BitmapFactory.decodeFile(any(), any()) } returns mockBitmap
        every { mockBitmap.compress(any(), any(), any()) } answers {
            val os = arg<java.io.OutputStream>(2)
            os.write("Compressed Legacy Webp Bytes".toByteArray())
            true
        }

        // When
        val attachment = localService.copyAndCompress(sourceUri)

        // Then
        assertNotNull(attachment)
        assertEquals("image/webp", attachment.mimeType)
        assertTrue(attachment.localUri.endsWith(".webp"))

        val compressedFile = File(java.net.URI(attachment.localUri))
        assertTrue(compressedFile.exists())
        assertEquals("Compressed Legacy Webp Bytes", compressedFile.readText())

        // Clean up
        tempFilesDir.deleteRecursively()
        tempCacheDir.deleteRecursively()
    }

    @Test
    fun copyAndCompress_cameraTempFile_deletesSourceCameraFile() = runTest {
        val mockContext = mockk<Context>()
        val mockResolver = mockk<ContentResolver>()
        val tempFilesDir = File(context.filesDir, "filesDir_camera").also { it.mkdirs() }
        val tempCacheDir = File(context.cacheDir, "cacheDir_camera").also { it.mkdirs() }
        every { mockContext.filesDir } returns tempFilesDir
        every { mockContext.cacheDir } returns tempCacheDir
        every { mockContext.contentResolver } returns mockResolver
        every { mockContext.packageName } returns "es.pedrazamiguez.splittrip"

        val localService = ReceiptStorageServiceImpl(mockContext, mockConnectionFactory)

        val receiptsDir = File(tempFilesDir, "receipts").also { it.mkdirs() }
        val cameraFile = File(receiptsDir, "camera_123.jpg").also { it.writeText("Camera Photo Bytes") }

        val sourceUri = "content://es.pedrazamiguez.splittrip.fileprovider/camera_123.jpg"
        val mockUri = mockk<Uri>(relaxed = true)
        mockkStatic(Uri::class)
        every { Uri.parse(sourceUri) } returns mockUri
        every { mockUri.authority } returns "es.pedrazamiguez.splittrip.fileprovider"
        every { mockUri.lastPathSegment } returns "camera_123.jpg"

        every { mockResolver.getType(mockUri) } returns "image/jpeg"
        every { mockResolver.openInputStream(mockUri) } returns ByteArrayInputStream("Camera Photo Bytes".toByteArray())

        mockkStatic(BitmapFactory::class)
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { BitmapFactory.decodeFile(any(), any()) } returns mockBitmap
        every { mockBitmap.compress(any(), any(), any()) } returns true

        // When
        val attachment = localService.copyAndCompress(sourceUri)

        // Then
        assertNotNull(attachment)
        assertTrue("Camera temp file should be deleted", !cameraFile.exists())

        // Clean up
        tempFilesDir.deleteRecursively()
        tempCacheDir.deleteRecursively()
    }
}
