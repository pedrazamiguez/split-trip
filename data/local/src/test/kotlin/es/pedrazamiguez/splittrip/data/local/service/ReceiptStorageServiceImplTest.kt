package es.pedrazamiguez.splittrip.data.local.service

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import es.pedrazamiguez.splittrip.domain.exception.TerminalDownloadException
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkConstructor
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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

        val localFile = File(URI(attachment.localUri))
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
            fail("Expected an exception to be thrown")
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

        val copiedFile = File(URI(attachment.localUri))
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
            val os = arg<OutputStream>(2)
            os.write("Compressed Lossless Webp Bytes".toByteArray())
            true
        }

        // When
        val attachment = localService.copyAndCompress(sourceUri)

        // Then
        assertNotNull(attachment)
        assertEquals("image/webp", attachment.mimeType)
        assertTrue(attachment.localUri.endsWith(".webp"))

        val compressedFile = File(URI(attachment.localUri))
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
            val os = arg<OutputStream>(2)
            os.write("Compressed Legacy Webp Bytes".toByteArray())
            true
        }

        // When
        val attachment = localService.copyAndCompress(sourceUri)

        // Then
        assertNotNull(attachment)
        assertEquals("image/webp", attachment.mimeType)
        assertTrue(attachment.localUri.endsWith(".webp"))

        val compressedFile = File(URI(attachment.localUri))
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

    @Test
    fun deleteLocalFile_existingFile_deletesFile() = runTest {
        // Given
        val receiptsDir = File(context.filesDir, "receipts").also { it.mkdirs() }
        val testFile = File(receiptsDir, "test_receipt.pdf").also { it.writeText("test content") }
        val localUri = testFile.toURI().toString()

        // When
        service.deleteLocalFile(localUri)

        // Then
        assertTrue("File should be deleted", !testFile.exists())
    }

    @Test
    fun deleteLocalFile_nonExistentFile_completesWithoutError() = runTest {
        // Given
        val localUri = "file:///non/existent/file.pdf"

        // When/Then
        // Should not throw any exception
        service.deleteLocalFile(localUri)
    }

    @Test
    fun deleteLocalFile_invalidScheme_refusesToDelete() = runTest {
        // Given
        val receiptsDir = File(context.filesDir, "receipts").also { it.mkdirs() }
        val testFile = File(receiptsDir, "test_receipt.pdf").also { it.writeText("test content") }
        val invalidUri = "http://example.com/receipts/test_receipt.pdf"

        // When
        service.deleteLocalFile(invalidUri)

        // Then
        assertTrue("File should NOT be deleted", testFile.exists())
    }

    @Test
    fun deleteLocalFile_outsideReceiptsDirectory_refusesToDelete() = runTest {
        // Given
        val outsideFile = File(context.filesDir, "outside_file.pdf").also { it.writeText("sensitive content") }
        val localUri = outsideFile.toURI().toString()

        // When
        service.deleteLocalFile(localUri)

        // Then
        assertTrue("File outside receipts directory should NOT be deleted", outsideFile.exists())
        outsideFile.delete() // clean up
    }

    @Test
    fun copyAndCompress_imageWithExifRotation90_rotatesBitmapAndRecyclesBoth() = runTest {
        val setup = setupImageMock("rot90", 1000, 800)
        try {
            mockkConstructor(ExifInterface::class)
            every {
                anyConstructed<ExifInterface>().getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } returns ExifInterface.ORIENTATION_ROTATE_90

            val attachment = setup.localService.copyAndCompress(setup.sourceUri)

            assertNotNull(attachment)
            assertEquals("image/webp", attachment.mimeType)
            assertTrue(attachment.localUri.endsWith(".webp"))
            verify(exactly = 1) { setup.mockOriginalBitmap.recycle() }
            verify(exactly = 1) { setup.mockRotatedBitmap.recycle() }
        } finally {
            setup.cleanUp()
            unmockkConstructor(ExifInterface::class)
        }
    }

    @Test
    fun copyAndCompress_imageWithExifRotation180_rotatesBitmapAndRecyclesBoth() = runTest {
        val setup = setupImageMock("rot180", 1000, 800)
        try {
            mockkConstructor(ExifInterface::class)
            every {
                anyConstructed<ExifInterface>().getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } returns ExifInterface.ORIENTATION_ROTATE_180

            val attachment = setup.localService.copyAndCompress(setup.sourceUri)

            assertNotNull(attachment)
            assertEquals("image/webp", attachment.mimeType)
            assertTrue(attachment.localUri.endsWith(".webp"))
            verify(exactly = 1) { setup.mockOriginalBitmap.recycle() }
            verify(exactly = 1) { setup.mockRotatedBitmap.recycle() }
        } finally {
            setup.cleanUp()
            unmockkConstructor(ExifInterface::class)
        }
    }

    @Test
    fun copyAndCompress_imageWithExifRotation270_rotatesBitmapAndRecyclesBoth() = runTest {
        val setup = setupImageMock("rot270", 800, 1000)
        try {
            mockkConstructor(ExifInterface::class)
            every {
                anyConstructed<ExifInterface>().getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } returns ExifInterface.ORIENTATION_ROTATE_270

            val attachment = setup.localService.copyAndCompress(setup.sourceUri)

            assertNotNull(attachment)
            assertEquals("image/webp", attachment.mimeType)
            assertTrue(attachment.localUri.endsWith(".webp"))
            verify(exactly = 1) { setup.mockOriginalBitmap.recycle() }
            verify(exactly = 1) { setup.mockRotatedBitmap.recycle() }
        } finally {
            setup.cleanUp()
            unmockkConstructor(ExifInterface::class)
        }
    }

    @Test
    fun copyAndCompress_imageWithExifFlipHorizontal_transformsBitmapAndRecyclesBoth() = runTest {
        val setup = setupImageMock("flip_h", 1000, 800)
        try {
            mockkConstructor(ExifInterface::class)
            every {
                anyConstructed<ExifInterface>().getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } returns ExifInterface.ORIENTATION_FLIP_HORIZONTAL

            val attachment = setup.localService.copyAndCompress(setup.sourceUri)

            assertNotNull(attachment)
            assertEquals("image/webp", attachment.mimeType)
            assertTrue(attachment.localUri.endsWith(".webp"))
            verify(exactly = 1) { setup.mockOriginalBitmap.recycle() }
            verify(exactly = 1) { setup.mockRotatedBitmap.recycle() }
        } finally {
            setup.cleanUp()
            unmockkConstructor(ExifInterface::class)
        }
    }

    @Test
    fun copyAndCompress_imageWithExifNormal_doesNotInvokeCreateBitmap() = runTest {
        val setup = setupImageMock("normal", 1000, 800)
        try {
            mockkConstructor(ExifInterface::class)
            every {
                anyConstructed<ExifInterface>().getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } returns ExifInterface.ORIENTATION_NORMAL

            val attachment = setup.localService.copyAndCompress(setup.sourceUri)

            assertNotNull(attachment)
            assertEquals("image/webp", attachment.mimeType)
            assertTrue(attachment.localUri.endsWith(".webp"))
            verify(exactly = 0) {
                Bitmap.createBitmap(setup.mockOriginalBitmap, any(), any(), any(), any(), any<Matrix>(), any())
            }
            verify(exactly = 1) { setup.mockOriginalBitmap.recycle() }
        } finally {
            setup.cleanUp()
            unmockkConstructor(ExifInterface::class)
        }
    }

    @Test
    fun copyAndCompress_whenExifThrowsException_fallsBackGracefullyWithoutCrash() = runTest {
        val setup = setupImageMock("exif_err", 1000, 800)
        try {
            mockkConstructor(ExifInterface::class)
            every {
                anyConstructed<ExifInterface>().getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } throws RuntimeException("corrupt EXIF")

            val attachment = setup.localService.copyAndCompress(setup.sourceUri)

            assertNotNull(attachment)
            assertEquals("image/webp", attachment.mimeType)
            assertTrue(attachment.localUri.endsWith(".webp"))
            verify(exactly = 0) {
                Bitmap.createBitmap(setup.mockOriginalBitmap, any(), any(), any(), any(), any<Matrix>(), any())
            }
            verify(exactly = 1) { setup.mockOriginalBitmap.recycle() }
        } finally {
            setup.cleanUp()
            unmockkConstructor(ExifInterface::class)
        }
    }

    @Test
    fun copyAndCompress_whenCompressThrowsException_recyclesBitmapsInFinally() = runTest {
        val setup = setupImageMock("compress_err", 1000, 800)
        every { setup.mockRotatedBitmap.compress(any(), any(), any()) } throws RuntimeException("Disk write failure")

        try {
            mockkConstructor(ExifInterface::class)
            every {
                anyConstructed<ExifInterface>().getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } returns ExifInterface.ORIENTATION_ROTATE_90

            try {
                setup.localService.copyAndCompress(setup.sourceUri)
                fail("Expected RuntimeException to be thrown")
            } catch (e: RuntimeException) {
                assertEquals("Disk write failure", e.message)
            }

            verify(exactly = 1) { setup.mockOriginalBitmap.recycle() }
            verify(exactly = 1) { setup.mockRotatedBitmap.recycle() }
        } finally {
            setup.cleanUp()
            unmockkConstructor(ExifInterface::class)
        }
    }

    @Test
    fun downloadAndStore_imageWithExifRotation90_rotatesBitmapAndRecyclesBoth() = runTest {
        val remoteUrl = "https://example.com/receipt.jpg"
        val imageData = "Fake Image Bytes".toByteArray()

        every { mockConnection.responseCode } returns HttpURLConnection.HTTP_OK
        every { mockConnection.contentType } returns "image/jpeg"
        every { mockConnection.inputStream } returns ByteArrayInputStream(imageData)

        mockkStatic(BitmapFactory::class)
        val mockOriginalBitmap = mockk<Bitmap>(relaxed = true)
        var originalRecycled = false
        every { mockOriginalBitmap.width } returns 1000
        every { mockOriginalBitmap.height } returns 800
        every { mockOriginalBitmap.recycle() } answers { originalRecycled = true }
        every { mockOriginalBitmap.isRecycled } answers { originalRecycled }

        every { BitmapFactory.decodeFile(any(), any()) } answers {
            val opts = arg<BitmapFactory.Options>(1)
            if (opts.inJustDecodeBounds) {
                opts.outWidth = 1000
                opts.outHeight = 800
                null
            } else {
                mockOriginalBitmap
            }
        }

        mockkStatic(Bitmap::class)
        val mockRotatedBitmap = mockk<Bitmap>(relaxed = true)
        var rotatedRecycled = false
        every { mockRotatedBitmap.width } returns 800
        every { mockRotatedBitmap.height } returns 1000
        every { mockRotatedBitmap.recycle() } answers { rotatedRecycled = true }
        every { mockRotatedBitmap.isRecycled } answers { rotatedRecycled }

        every {
            Bitmap.createBitmap(mockOriginalBitmap, 0, 0, 1000, 800, any(), true)
        } returns mockRotatedBitmap

        every { mockRotatedBitmap.compress(any(), any(), any()) } answers {
            val os = arg<OutputStream>(2)
            os.write("Compressed Rotated Webp Bytes".toByteArray())
            true
        }

        try {
            mockkConstructor(ExifInterface::class)
            every {
                anyConstructed<ExifInterface>().getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } returns ExifInterface.ORIENTATION_ROTATE_90

            val attachment = service.downloadAndStore(remoteUrl)

            assertNotNull(attachment)
            assertEquals("image/webp", attachment.mimeType)
            assertEquals(remoteUrl, attachment.remoteUrl)
            assertTrue(attachment.localUri.endsWith(".webp"))
            verify(exactly = 1) { mockOriginalBitmap.recycle() }
            verify(exactly = 1) { mockRotatedBitmap.recycle() }
        } finally {
            unmockkConstructor(ExifInterface::class)
        }
    }

    private fun createMockBitmaps(width: Int, height: Int): Pair<Bitmap, Bitmap> {
        val mockOriginalBitmap = mockk<Bitmap>(relaxed = true)
        var originalRecycled = false
        every { mockOriginalBitmap.width } returns width
        every { mockOriginalBitmap.height } returns height
        every { mockOriginalBitmap.recycle() } answers { originalRecycled = true }
        every { mockOriginalBitmap.isRecycled } answers { originalRecycled }

        val mockRotatedBitmap = mockk<Bitmap>(relaxed = true)
        var rotatedRecycled = false
        every { mockRotatedBitmap.width } returns height
        every { mockRotatedBitmap.height } returns width
        every { mockRotatedBitmap.recycle() } answers { rotatedRecycled = true }
        every { mockRotatedBitmap.isRecycled } answers { rotatedRecycled }

        return mockOriginalBitmap to mockRotatedBitmap
    }

    private fun setupImageMock(
        tag: String,
        width: Int = 1000,
        height: Int = 800
    ): ImageMockSetup {
        val mockContext = mockk<Context>()
        val mockResolver = mockk<ContentResolver>()
        val tempFilesDir = File(context.filesDir, "filesDir_$tag").also { it.mkdirs() }
        val tempCacheDir = File(context.cacheDir, "cacheDir_$tag").also { it.mkdirs() }
        every { mockContext.filesDir } returns tempFilesDir
        every { mockContext.cacheDir } returns tempCacheDir
        every { mockContext.contentResolver } returns mockResolver
        every { mockContext.packageName } returns "es.pedrazamiguez.splittrip"

        val localService = ReceiptStorageServiceImpl(mockContext, mockConnectionFactory)

        val sourceUri = "content://media/image_$tag"
        val mockUri = mockk<Uri>(relaxed = true)
        mockkStatic(Uri::class)
        every { Uri.parse(sourceUri) } returns mockUri
        every { mockResolver.getType(mockUri) } returns "image/jpeg"
        every { mockResolver.openInputStream(mockUri) } returns ByteArrayInputStream("Fake Image Data".toByteArray())

        val (mockOriginalBitmap, mockRotatedBitmap) = createMockBitmaps(width, height)

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any(), any()) } answers {
            val opts = arg<BitmapFactory.Options>(1)
            if (opts.inJustDecodeBounds) {
                opts.outWidth = width
                opts.outHeight = height
                null
            } else {
                mockOriginalBitmap
            }
        }

        mockkStatic(Bitmap::class)
        every {
            Bitmap.createBitmap(mockOriginalBitmap, 0, 0, width, height, any(), true)
        } returns mockRotatedBitmap

        every { mockRotatedBitmap.compress(any(), any(), any()) } answers {
            val os = arg<OutputStream>(2)
            os.write("Compressed Rotated Webp Bytes".toByteArray())
            true
        }
        every { mockOriginalBitmap.compress(any(), any(), any()) } answers {
            val os = arg<OutputStream>(2)
            os.write("Compressed Original Webp Bytes".toByteArray())
            true
        }

        return ImageMockSetup(
            localService = localService,
            sourceUri = sourceUri,
            mockOriginalBitmap = mockOriginalBitmap,
            mockRotatedBitmap = mockRotatedBitmap,
            tempFilesDir = tempFilesDir,
            tempCacheDir = tempCacheDir
        )
    }

    private data class ImageMockSetup(
        val localService: ReceiptStorageServiceImpl,
        val sourceUri: String,
        val mockOriginalBitmap: Bitmap,
        val mockRotatedBitmap: Bitmap,
        val tempFilesDir: File,
        val tempCacheDir: File
    ) {
        fun cleanUp() {
            tempFilesDir.deleteRecursively()
            tempCacheDir.deleteRecursively()
        }
    }
}
